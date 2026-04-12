package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.AvailabilityConflictDTO;
import com.domination.booking.dto.AvailabilityResponse;
import com.domination.booking.dto.CreateReservationLineRequest;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ProviderReservationMetricsDto;
import com.domination.booking.dto.ProviderReservationTimeMode;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.exception.ConflictException;
import com.domination.booking.exception.InsufficientStockException;
import com.domination.booking.exception.NotFoundException;
import com.domination.booking.mapper.ReservationMapper;
import com.domination.booking.model.HoldInventoryRequest;
import com.domination.booking.model.HoldInventoryResponse;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.model.ReleaseInventoryRequest;
import com.domination.booking.repository.ReservationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final CatalogClient catalogClient;
    private final ReservationDtoEnricher reservationDtoEnricher;

    @Transactional(readOnly = true)
    public List<ReservationDTO> getMyReservations(String customerId) {
        log.debug("Obteniendo reservas del cliente: {}", customerId);

        List<ReservationDTO> list = reservationRepository.findByCustomerIdOrderByStartAtDescIdDesc(customerId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        reservationDtoEnricher.enrichMissingCatalogFields(list);
        return list;
    }

    @Transactional(readOnly = true)
    public Page<ReservationDTO> searchProviderReservations(
            Long providerId,
            Long branchId,
            ReservationStatus status,
            ProviderReservationTimeMode timeMode,
            LocalDateTime windowFrom,
            LocalDateTime windowTo,
            Pageable pageable,
            String authorizationHeader) {
        log.debug(
                "Listado paginado provider {} branchId={} status={} time={} window=[{},{}] page={}",
                providerId, branchId, status, timeMode, windowFrom, windowTo, pageable.getPageNumber());

        LocalDateTime now = LocalDateTime.now();
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("providerId"), providerId));
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branchId"), branchId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (timeMode == ProviderReservationTimeMode.UPCOMING) {
                predicates.add(cb.notEqual(root.get("status"), ReservationStatus.CANCELLED));
                predicates.add(cb.greaterThan(root.get("startAt"), now));
            } else if (timeMode == ProviderReservationTimeMode.IN_PROGRESS) {
                predicates.add(cb.notEqual(root.get("status"), ReservationStatus.CANCELLED));
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), now));
                predicates.add(cb.greaterThan(root.get("endAt"), now));
            } else if (timeMode == ProviderReservationTimeMode.COMPLETED) {
                predicates.add(cb.notEqual(root.get("status"), ReservationStatus.CANCELLED));
                predicates.add(cb.lessThanOrEqualTo(root.get("endAt"), now));
            }
            if (windowFrom != null && windowTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), windowTo));
                predicates.add(cb.greaterThanOrEqualTo(root.get("endAt"), windowFrom));
            } else if (windowFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endAt"), windowFrom));
            } else if (windowTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), windowTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Reservation> entityPage = reservationRepository.findAll(spec, pageable);
        List<ReservationDTO> dtos = entityPage.getContent().stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
        reservationDtoEnricher.enrichMissingCatalogFields(dtos);
        reservationDtoEnricher.enrichCustomerUsernamesForProvider(dtos, authorizationHeader);
        return new PageImpl<>(dtos, pageable, entityPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProviderReservationMetricsDto getProviderReservationMetrics(Long providerId) {
        return reservationRepository.fetchProviderReservationMetrics(providerId, LocalDateTime.now());
    }

    @Transactional
    public ReservationDTO createReservation(CreateReservationRequest request, String customerId) {
        log.info("Creando reserva para cliente {} en sucursal {}", customerId, request.getBranchId());

        // Validar rango de fechas
        if (!request.getStartAt().isBefore(request.getEndAt())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        // Obtener providerId del branch
        var branchDetail = catalogClient.getBranchDetail(request.getBranchId());
        Long providerId = branchDetail.getProviderId();
        
        log.debug("Branch {} pertenece al provider {}", request.getBranchId(), providerId);

        // Crear la reserva
        Reservation reservation = Reservation.builder()
                .customerId(customerId)
                .branchId(request.getBranchId())
                .branchName(branchDetail.getName())
                .providerId(providerId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(ReservationStatus.CONFIRMED)
                .lines(new ArrayList<>())
                .build();

        List<String> createdHoldIds = new ArrayList<>();

        // Procesar cada línea de reserva
        try {
            for (CreateReservationLineRequest lineReq : request.getLines()) {
                processReservationLine(reservation, lineReq, customerId, request.getBranchId(), createdHoldIds);
            }
        } catch (RuntimeException ex) {
            releaseCreatedHoldsBestEffort(createdHoldIds);
            throw ex;
        }

        // Guardar la reserva
        Reservation saved = reservationRepository.save(reservation);
        log.info("Reserva creada con id: {}", saved.getId());

        ReservationDTO dto = reservationMapper.toDTO(saved);
        reservationDtoEnricher.enrichMissingCatalogFields(List.of(dto));
        return dto;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(CreateReservationRequest request, String customerId) {
        log.info("Chequeando disponibilidad para cliente {} en sucursal {}", customerId, request.getBranchId());

        List<AvailabilityConflictDTO> conflicts = new ArrayList<>();

        // Validar rango sin lanzar excepción
        if (!request.getStartAt().isBefore(request.getEndAt())) {
            conflicts.add(AvailabilityConflictDTO.builder()
                    .itemId(null)
                    .reason("INVALID_RANGE")
                    .detail("La fecha de inicio debe ser anterior a la fecha de fin")
                    .requestedQty(null)
                    .availableQty(null)
                    .reservedQty(null)
                    .build());

            log.warn("Rango inválido en checkAvailability para cliente {}: startAt={}, endAt={}",
                    customerId, request.getStartAt(), request.getEndAt());

            return AvailabilityResponse.builder()
                    .available(false)
                    .conflicts(conflicts)
                    .build();
        }

        for (CreateReservationLineRequest lineReq : request.getLines()) {
            Long itemId = lineReq.getItemId();
            Integer requestedQty = lineReq.getQuantity();
            log.debug("Pre-check línea: itemId={}, quantity={}", itemId, requestedQty);

            ItemDetailResponse itemDetail = catalogClient.getItemDetail(itemId);

            if (!Boolean.TRUE.equals(itemDetail.getActive())) {
                conflicts.add(AvailabilityConflictDTO.builder()
                        .itemId(itemId)
                        .reason("ITEM_INACTIVE")
                        .detail("El item " + itemId + " no está activo")
                        .requestedQty(requestedQty)
                        .availableQty(null)
                        .reservedQty(null)
                        .build());
                continue;
            }

            if ("TIME_EXCLUSIVE".equals(itemDetail.getRentalMode())) {
                List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                        itemId, request.getStartAt(), request.getEndAt(), ReservationStatus.CANCELLED
                );

                if (!overlapping.isEmpty()) {
                    conflicts.add(AvailabilityConflictDTO.builder()
                            .itemId(itemId)
                            .reason("OVERLAP")
                            .detail(String.format("El item %d ya está reservado en el horario solicitado", itemId))
                            .requestedQty(requestedQty)
                            .availableQty(null)
                            .reservedQty(null)
                            .build());
                }
                continue;
            }

            if ("TIME_QUANTITY".equals(itemDetail.getRentalMode())) {
                Integer totalStock = itemDetail.getQuantityTotal();
                Integer reservedQty = reservationRepository.sumReservedQuantity(
                        itemId, request.getStartAt(), request.getEndAt(), ReservationStatus.CANCELLED
                );
                if (reservedQty == null) {
                    reservedQty = 0;
                }

                if (totalStock == null || requestedQty == null || (reservedQty + requestedQty > totalStock)) {
                    conflicts.add(AvailabilityConflictDTO.builder()
                            .itemId(itemId)
                            .reason("INSUFFICIENT_STOCK")
                            .detail(String.format("Stock insuficiente para item %d en el rango solicitado", itemId))
                            .requestedQty(requestedQty)
                            .availableQty(totalStock)
                            .reservedQty(reservedQty)
                            .build());
                }
            }
        }

        boolean available = conflicts.isEmpty();
        log.info("Resultado checkAvailability cliente {}: available={}, conflicts={}",
                customerId, available, conflicts.size());

        return AvailabilityResponse.builder()
                .available(available)
                .conflicts(conflicts)
                .build();
    }

    private void processReservationLine(Reservation reservation,
                                        CreateReservationLineRequest lineReq,
                                        String customerId,
                                        Long branchId,
                                        List<String> createdHoldIds) {
        Long itemId = lineReq.getItemId();
        Integer requestedQty = lineReq.getQuantity();

        log.debug("Procesando línea: itemId={}, quantity={}", itemId, requestedQty);

        // Obtener detalles del item desde catalog-service
        ItemDetailResponse itemDetail = catalogClient.getItemDetail(itemId);

        if (!itemDetail.getActive()) {
            throw new IllegalArgumentException("El item " + itemId + " no está activo");
        }

        // Validar según el modo de alquiler
        if ("TIME_EXCLUSIVE".equals(itemDetail.getRentalMode())) {
            validateTimeExclusive(itemId, reservation.getStartAt(), reservation.getEndAt());
        } else if ("TIME_QUANTITY".equals(itemDetail.getRentalMode())) {
            validateTimeQuantity(itemId, requestedQty, itemDetail.getQuantityTotal(), 
                               reservation.getStartAt(), reservation.getEndAt());
        }

        // Calcular precio
        var totalPrice = itemDetail.getBasePrice().multiply(java.math.BigDecimal.valueOf(requestedQty));

        // Crear línea de reserva
        ReservationLine line = ReservationLine.builder()
                .reservation(reservation)
                .itemId(itemId)
                .itemName(itemDetail.getName())
                .quantity(requestedQty)
                .price(totalPrice)
                .build();

        if ("TIME_QUANTITY".equals(itemDetail.getRentalMode())) {
            String reference = "cust-" + customerId + "-branch-" + branchId;
            HoldInventoryRequest holdRequest = HoldInventoryRequest.builder()
                    .itemId(itemId)
                    .quantity(requestedQty)
                    .startAt(reservation.getStartAt())
                    .endAt(reservation.getEndAt())
                    .reference(reference)
                    .build();

            HoldInventoryResponse holdResponse = catalogClient.holdInventory(holdRequest);
            if (holdResponse != null && holdResponse.getHoldId() != null) {
                line.setHoldId(holdResponse.getHoldId());
                createdHoldIds.add(holdResponse.getHoldId());
            }
        }

        reservation.getLines().add(line);

        log.debug("Línea procesada: itemId={}, qty={}, price={}", itemId, requestedQty, totalPrice);
    }

    /**
     * Valida que no haya solapamiento para items TIME_EXCLUSIVE
     */
    private void validateTimeExclusive(Long itemId, java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        log.debug("Validando TIME_EXCLUSIVE para itemId={}", itemId);

        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                itemId, startAt, endAt, ReservationStatus.CANCELLED
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException(
                    String.format("El item %d ya está reservado en el horario solicitado", itemId)
            );
        }

        log.debug("Validación TIME_EXCLUSIVE OK para itemId={}", itemId);
    }

    /**
     * Valida que haya stock suficiente para items TIME_QUANTITY
     */
    private void validateTimeQuantity(Long itemId, Integer requestedQty, Integer totalStock,
                                     java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        log.debug("Validando TIME_QUANTITY para itemId={}, requested={}, total={}", 
                 itemId, requestedQty, totalStock);

        Integer alreadyReserved = reservationRepository.sumReservedQuantity(
                itemId, startAt, endAt, ReservationStatus.CANCELLED
        );

        Integer available = totalStock - alreadyReserved;

        log.debug("Stock disponible: {} (total: {}, reservado: {})", available, totalStock, alreadyReserved);

        if (available < requestedQty) {
            throw new InsufficientStockException(
                    String.format("Stock insuficiente para item %d. Disponible: %d, solicitado: %d",
                            itemId, available, requestedQty)
            );
        }

        log.debug("Validación TIME_QUANTITY OK para itemId={}", itemId);
    }

    /**
        *   Mét0do cancel(...) Objetivo: transacción que busca, valida ownership, aplica regla “ya empezó”, setea status CANCELLED y guarda.
        *   Buscar la reserva por id + customerId (ideal para no filtrar existencia).
        *   Si ya está CANCELLED ⇒ return tal cual.
        *   Si no, validar si se puede cancelar (si aplica).
        *   Set status y persistir.
    */
    @Transactional
    public ReservationDTO cancelReservation(Long reservationId, String customerId){
        Reservation reservation = reservationRepository
                .findByIdAndCustomerId(reservationId,customerId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            ReservationDTO cancelledDto = reservationMapper.toDTO(reservation);
            reservationDtoEnricher.enrichMissingCatalogFields(List.of(cancelledDto));
            return cancelledDto;
        }
        if (!reservation.getStartAt().isAfter(java.time.LocalDateTime.now())){
            throw new ConflictException("No se puede cancelar una reserva que ya empezó");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        // Liberación best-effort de holds de líneas TIME_QUANTITY
        saved.getLines().stream()
                .map(ReservationLine::getHoldId)
                .filter(holdId -> holdId != null && !holdId.isBlank())
                .forEach(this::releaseHoldBestEffort);

        ReservationDTO dto = reservationMapper.toDTO(saved);
        reservationDtoEnricher.enrichMissingCatalogFields(List.of(dto));
        return dto;
    }

    private void releaseCreatedHoldsBestEffort(List<String> holdIds) {
        for (String holdId : holdIds) {
            releaseHoldBestEffort(holdId);
        }
    }

    private void releaseHoldBestEffort(String holdId) {
        try {
            catalogClient.releaseInventory(ReleaseInventoryRequest.builder()
                    .holdId(holdId)
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo liberar holdId={} (best-effort)", holdId, e);
        }
    }

}



package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.AvailabilityConflictDTO;
import com.domination.booking.dto.AvailabilityResponse;
import com.domination.booking.dto.CreateReservationLineRequest;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.exception.ConflictException;
import com.domination.booking.exception.InsufficientStockException;
import com.domination.booking.exception.NotFoundException;
import com.domination.booking.mapper.ReservationMapper;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<ReservationDTO> getMyReservations(String customerId) {
        log.debug("Obteniendo reservas del cliente: {}", customerId);
        
        return reservationRepository.findByCustomerId(customerId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> getProviderReservations(Long providerId) {
        log.debug("Obteniendo reservas del provider: {}", providerId);
        
        return reservationRepository.findByProviderId(providerId).stream()
                .map(reservationMapper::toDTO)
                .collect(Collectors.toList());
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
                .providerId(providerId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .status(ReservationStatus.PENDING)
                .lines(new ArrayList<>())
                .build();

        // Procesar cada línea de reserva
        for (CreateReservationLineRequest lineReq : request.getLines()) {
            processReservationLine(reservation, lineReq);
        }

        // Guardar la reserva
        Reservation saved = reservationRepository.save(reservation);
        log.info("Reserva creada con id: {}", saved.getId());

        return reservationMapper.toDTO(saved);
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

    private void processReservationLine(Reservation reservation, CreateReservationLineRequest lineReq) {
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
                .quantity(requestedQty)
                .price(totalPrice)
                .build();

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
        if (reservation.getStatus() == ReservationStatus.CANCELLED){
            return reservationMapper.toDTO(reservation);
        }
        if (!reservation.getStartAt().isAfter(java.time.LocalDateTime.now())){
            throw new ConflictException("No se puede cancelar una reserva que ya empezó");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toDTO(saved);
        // TODO Sprint stock real: liberar inventario/hold en catalog-service
    }

}



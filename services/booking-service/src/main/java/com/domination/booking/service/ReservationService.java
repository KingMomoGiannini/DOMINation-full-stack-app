package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.CreateReservationLineRequest;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.exception.ConflictException;
import com.domination.booking.exception.InsufficientStockException;
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

    @Transactional
    public ReservationDTO createReservation(CreateReservationRequest request, String customerId) {
        log.info("Creando reserva para cliente {} en sucursal {}", customerId, request.getBranchId());

        // Validar rango de fechas
        if (!request.getStartAt().isBefore(request.getEndAt())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        // Crear la reserva
        Reservation reservation = Reservation.builder()
                .customerId(customerId)
                .branchId(request.getBranchId())
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
}



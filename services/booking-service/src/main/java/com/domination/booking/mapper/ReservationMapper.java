package com.domination.booking.mapper;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import com.domination.booking.service.ReservationLifecycleResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final ReservationLifecycleResolver reservationLifecycleResolver;

    public ReservationDTO toDTO(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();

        return ReservationDTO.builder()
                .id(reservation.getId())
                .customerId(reservation.getCustomerId())
                .branchName(reservation.getBranchName())
                .branchId(reservation.getBranchId())
                .providerId(reservation.getProviderId())
                .startAt(reservation.getStartAt())
                .endAt(reservation.getEndAt())
                .status(reservation.getStatus())
                .operationalStatus(reservationLifecycleResolver.resolveOperationalStatus(reservation, now))
                .cancellable(reservationLifecycleResolver.isCancellable(reservation, now))
                .cancellationBlockReason(reservationLifecycleResolver.resolveCancellationBlockReason(reservation, now))
                .createdAt(reservation.getCreatedAt())
                .lines(reservation.getLines().stream()
                        .map(this::lineToDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    public ReservationLineDTO lineToDTO(ReservationLine line) {
        if (line == null) {
            return null;
        }

        return ReservationLineDTO.builder()
                .id(line.getId())
                .itemId(line.getItemId())
                .itemName(line.getItemName())
                .quantity(line.getQuantity())
                .price(line.getPrice())
                .build();
    }
}



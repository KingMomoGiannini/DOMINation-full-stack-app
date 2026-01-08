package com.domination.booking.mapper;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationLine;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ReservationMapper {

    public ReservationDTO toDTO(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        return ReservationDTO.builder()
                .id(reservation.getId())
                .customerId(reservation.getCustomerId())
                .branchId(reservation.getBranchId())
                .startAt(reservation.getStartAt())
                .endAt(reservation.getEndAt())
                .status(reservation.getStatus())
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
                .quantity(line.getQuantity())
                .price(line.getPrice())
                .build();
    }
}



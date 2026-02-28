package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.AvailabilityResponse;
import com.domination.booking.dto.CreateReservationLineRequest;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.mapper.ReservationMapper;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceCheckAvailabilityTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private CatalogClient catalogClient;

    @InjectMocks private ReservationService reservationService;

    @Test
    void checkAvailability_returnsInvalidRangeConflict_whenStartIsNotBeforeEnd() {
        LocalDateTime now = LocalDateTime.now();
        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(now.plusDays(1))
                .endAt(now.plusDays(1)) // inválido
                .lines(List.of(CreateReservationLineRequest.builder().itemId(1L).quantity(1).build()))
                .build();

        AvailabilityResponse response = reservationService.checkAvailability(request, "cust-1");

        assertFalse(response.isAvailable());
        assertNotNull(response.getConflicts());
        assertEquals(1, response.getConflicts().size());
        assertEquals("INVALID_RANGE", response.getConflicts().get(0).getReason());

        verifyNoInteractions(catalogClient);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void checkAvailability_returnsOverlapConflict_whenTimeExclusiveHasOverlappingReservations() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(CreateReservationLineRequest.builder().itemId(1L).quantity(1).build()))
                .build();

        ItemDetailResponse itemDetail = ItemDetailResponse.builder()
                .id(1L)
                .active(true)
                .rentalMode("TIME_EXCLUSIVE")
                .build();
        when(catalogClient.getItemDetail(1L)).thenReturn(itemDetail);
        when(reservationRepository.findOverlappingReservations(
                eq(1L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(List.of(mock(Reservation.class)));

        AvailabilityResponse response = reservationService.checkAvailability(request, "cust-1");

        assertFalse(response.isAvailable());
        assertEquals(1, response.getConflicts().size());
        assertEquals("OVERLAP", response.getConflicts().get(0).getReason());
        assertEquals(1L, response.getConflicts().get(0).getItemId());

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void checkAvailability_returnsInsufficientStockConflict_whenTimeQuantityHasNotEnoughStock() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(CreateReservationLineRequest.builder().itemId(2L).quantity(5).build()))
                .build();

        ItemDetailResponse itemDetail = ItemDetailResponse.builder()
                .id(2L)
                .active(true)
                .rentalMode("TIME_QUANTITY")
                .quantityTotal(10)
                .build();

        when(catalogClient.getItemDetail(2L)).thenReturn(itemDetail);
        when(reservationRepository.sumReservedQuantity(
                eq(2L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(8);

        AvailabilityResponse response = reservationService.checkAvailability(request, "cust-1");

        assertFalse(response.isAvailable());
        assertEquals(1, response.getConflicts().size());
        assertEquals("INSUFFICIENT_STOCK", response.getConflicts().get(0).getReason());
        assertEquals(5, response.getConflicts().get(0).getRequestedQty());
        assertEquals(8, response.getConflicts().get(0).getReservedQty());
        assertEquals(10, response.getConflicts().get(0).getAvailableQty());

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void checkAvailability_returnsAvailableTrue_whenNoConflicts() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(
                        CreateReservationLineRequest.builder().itemId(1L).quantity(1).build(),
                        CreateReservationLineRequest.builder().itemId(2L).quantity(2).build()
                ))
                .build();

        ItemDetailResponse exclusive = ItemDetailResponse.builder()
                .id(1L)
                .active(true)
                .rentalMode("TIME_EXCLUSIVE")
                .build();
        when(catalogClient.getItemDetail(1L)).thenReturn(exclusive);
        when(reservationRepository.findOverlappingReservations(
                eq(1L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(List.of());

        ItemDetailResponse quantity = ItemDetailResponse.builder()
                .id(2L)
                .active(true)
                .rentalMode("TIME_QUANTITY")
                .quantityTotal(10)
                .build();
        when(catalogClient.getItemDetail(2L)).thenReturn(quantity);
        when(reservationRepository.sumReservedQuantity(
                eq(2L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(3);

        AvailabilityResponse response = reservationService.checkAvailability(request, "cust-1");

        assertTrue(response.isAvailable());
        assertNotNull(response.getConflicts());
        assertTrue(response.getConflicts().isEmpty());

        verify(reservationRepository, never()).save(any());
    }
}

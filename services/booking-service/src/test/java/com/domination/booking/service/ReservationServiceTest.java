package com.domination.booking.service;

import com.domination.booking.domain.Reservation;
import com.domination.booking.domain.ReservationStatus;
import com.domination.booking.dto.CreateReservationLineRequest;
import com.domination.booking.dto.CreateReservationRequest;
import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.exception.ConflictException;
import com.domination.booking.exception.InsufficientStockException;
import com.domination.booking.mapper.ReservationMapper;
import com.domination.booking.model.BranchResponse;
import com.domination.booking.model.ItemDetailResponse;
import com.domination.booking.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static  org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationMapper reservationMapper;
    @Mock private CatalogClient catalogClient;

    @InjectMocks private ReservationService reservationService;

    @Test
    void createReservation_throwsIllegalArgumentException_whenStartIsNotBeforeEnd() {
        LocalDateTime now = LocalDateTime.now();

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(now.plusDays(1))
                .endAt(now.plusDays(1)) // igual -> inválido
                .lines(List.of(CreateReservationLineRequest.builder().itemId(1L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request, "cust-1"));

        // No debería llamar a catalog ni guardar nada
        verifyNoInteractions(catalogClient);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_throwsIllegalArgumentException_whenItemIsInactive() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(mockLine(99L, 2)))
                .build();

        // branch -> providerId

        var branchDetail = mock(BranchResponse.class);
        when(branchDetail.getProviderId()).thenReturn(777L);
        when(catalogClient.getBranchDetail(10L)).thenReturn(branchDetail);

        // item inactivo
        ItemDetailResponse itemDetail = mock(ItemDetailResponse.class);
        when(itemDetail.getActive()).thenReturn(false);
        when(catalogClient.getItemDetail(99L)).thenReturn(itemDetail);

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.createReservation(request, "cust-1"));

        verify(reservationRepository, never()).save(any());
        verify(reservationMapper, never()).toDTO(any());
    }

    @Test
    void createReservation_throwsConflictException_whenTimeExclusiveOverlaps() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(mockLine(1L, 1)))
                .build();

        var branchDetail = mock(BranchResponse.class);
        when(branchDetail.getProviderId()).thenReturn(777L);
        when(catalogClient.getBranchDetail(10L)).thenReturn(branchDetail);

        ItemDetailResponse itemDetail = mock(ItemDetailResponse.class);
        when(itemDetail.getActive()).thenReturn(true);
        when(itemDetail.getRentalMode()).thenReturn("TIME_EXCLUSIVE");
        when(catalogClient.getItemDetail(1L)).thenReturn(itemDetail);

        when(reservationRepository.findOverlappingReservations(
                eq(1L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(List.of(mock(Reservation.class))); // hay solapamiento

        // Act + Assert
        assertThrows(ConflictException.class,
                () -> reservationService.createReservation(request, "cust-1"));

        verify(reservationRepository, never()).save(any());
        verify(reservationMapper, never()).toDTO(any());
    }

    @Test
    void createReservation_throwsInsufficientStockException_whenTimeQuantityNotEnoughStock() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(mockLine(2L, 5))) // pide 5
                .build();

        var branchDetail = mock(BranchResponse.class);
        when(branchDetail.getProviderId()).thenReturn(777L);
        when(catalogClient.getBranchDetail(10L)).thenReturn(branchDetail);

        ItemDetailResponse itemDetail = mock(ItemDetailResponse.class);
        when(itemDetail.getActive()).thenReturn(true);
        when(itemDetail.getRentalMode()).thenReturn("TIME_QUANTITY");
        when(itemDetail.getQuantityTotal()).thenReturn(10); // total 10
        when(catalogClient.getItemDetail(2L)).thenReturn(itemDetail);

        when(reservationRepository.sumReservedQuantity(
                eq(2L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(8); // ya reservados 8 -> quedan 2

        // Act + Assert
        assertThrows(InsufficientStockException.class,
                () -> reservationService.createReservation(request, "cust-1"));

        verify(reservationRepository, never()).save(any());
        verify(reservationMapper, never()).toDTO(any());
    }

    @Test
    void createReservation_savesReservation_andMapsToDto_whenValid() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .branchId(10L)
                .startAt(start)
                .endAt(end)
                .lines(List.of(mockLine(5L, 3)))
                .build();

        var branchDetail = mock(BranchResponse.class);
        when(branchDetail.getProviderId()).thenReturn(777L);
        when(catalogClient.getBranchDetail(10L)).thenReturn(branchDetail);

        ItemDetailResponse itemDetail = mock(ItemDetailResponse.class);
        when(itemDetail.getActive()).thenReturn(true);
        when(itemDetail.getRentalMode()).thenReturn("TIME_EXCLUSIVE");
        when(itemDetail.getBasePrice()).thenReturn(new BigDecimal("100.00"));
        when(catalogClient.getItemDetail(5L)).thenReturn(itemDetail);

        when(reservationRepository.findOverlappingReservations(
                eq(5L), eq(start), eq(end), eq(ReservationStatus.CANCELLED)
        )).thenReturn(List.of()); // sin solapamiento

        // capturar qué se guarda
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0)); // devuelve lo mismo

        ReservationDTO dto = mock(ReservationDTO.class);
        when(reservationMapper.toDTO(any(Reservation.class))).thenReturn(dto);

        // Act
        ReservationDTO result = reservationService.createReservation(request, "cust-1");

        // Assert
        assertSame(dto, result);

        Reservation saved = captor.getValue();
        assertEquals("cust-1", saved.getCustomerId());
        assertEquals(10L, saved.getBranchId());
        assertEquals(777L, saved.getProviderId());
        assertEquals(start, saved.getStartAt());
        assertEquals(end, saved.getEndAt());
        assertEquals(ReservationStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getLines());
        assertEquals(1, saved.getLines().size());

        var line = saved.getLines().get(0);
        assertEquals(5L, line.getItemId());
        assertEquals(3, line.getQuantity());
        assertEquals(new BigDecimal("300.00"), line.getPrice()); // 100 * 3

        verify(reservationRepository).save(any(Reservation.class));
        verify(reservationMapper).toDTO(any(Reservation.class));
    }

    // ---------- helpers ----------
    private CreateReservationLineRequest mockLine(Long itemId, Integer qty) {
        CreateReservationLineRequest line = mock(CreateReservationLineRequest.class);
        when(line.getItemId()).thenReturn(itemId);
        when(line.getQuantity()).thenReturn(qty);
        return line;
    }
}

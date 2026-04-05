package com.domination.booking.service;

import com.domination.booking.dto.ReservationDTO;
import com.domination.booking.dto.ReservationLineDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationDtoEnricherTest {

    @Mock
    private CatalogClient catalogClient;
    @Mock
    private AuthClient authClient;

    @InjectMocks
    private ReservationDtoEnricher enricher;

    @Test
    void enrichMissingCatalogFields_fillsBranchAndItem_fromCatalogOncePerId() {
        ReservationLineDTO line1 = ReservationLineDTO.builder().id(1L).itemId(10L).quantity(1).itemName(null).price(null).build();
        ReservationLineDTO line2 = ReservationLineDTO.builder().id(2L).itemId(10L).quantity(2).itemName("").price(null).build();
        ReservationDTO a = ReservationDTO.builder()
                .id(1L).customerId("1").branchId(5L).branchName(null)
                .lines(List.of(line1))
                .build();
        ReservationDTO b = ReservationDTO.builder()
                .id(2L).customerId("2").branchId(5L).branchName("  ")
                .lines(List.of(line2))
                .build();

        when(catalogClient.fetchBranchNameForEnrichment(5L)).thenReturn(Optional.of("Centro"));
        when(catalogClient.fetchItemNameForEnrichment(10L)).thenReturn(Optional.of("Cancha 1"));

        enricher.enrichMissingCatalogFields(List.of(a, b));

        assertEquals("Centro", a.getBranchName());
        assertEquals("Centro", b.getBranchName());
        assertEquals("Cancha 1", line1.getItemName());
        assertEquals("Cancha 1", line2.getItemName());

        verify(catalogClient, times(1)).fetchBranchNameForEnrichment(5L);
        verify(catalogClient, times(1)).fetchItemNameForEnrichment(10L);
    }

    @Test
    void enrichMissingCatalogFields_leavesNull_whenCatalogReturnsEmpty() {
        ReservationDTO dto = ReservationDTO.builder()
                .id(1L).customerId("9").branchId(99L).branchName(null)
                .lines(List.of(ReservationLineDTO.builder().id(1L).itemId(1L).quantity(1).itemName(null).price(null).build()))
                .build();

        when(catalogClient.fetchBranchNameForEnrichment(99L)).thenReturn(Optional.empty());
        when(catalogClient.fetchItemNameForEnrichment(1L)).thenReturn(Optional.empty());

        enricher.enrichMissingCatalogFields(List.of(dto));

        assertNull(dto.getBranchName());
        assertNull(dto.getLines().get(0).getItemName());
    }

    @Test
    void enrichCustomerUsernamesForProvider_skips_whenNoAuthHeader() {
        ReservationDTO dto = ReservationDTO.builder()
                .id(1L).customerId("42").branchId(1L).build();

        enricher.enrichCustomerUsernamesForProvider(List.of(dto), null);

        verifyNoInteractions(authClient);
        assertNull(dto.getCustomerUsername());
    }

    @Test
    void enrichCustomerUsernamesForProvider_fillsUsername_andCachesByUserId() {
        ReservationDTO a = ReservationDTO.builder().id(1L).customerId("7").branchId(1L).build();
        ReservationDTO b = ReservationDTO.builder().id(2L).customerId("7").branchId(2L).build();

        when(authClient.getUsernameForProvider(7L, "Bearer x")).thenReturn(Optional.of("bob"));

        enricher.enrichCustomerUsernamesForProvider(List.of(a, b), "Bearer x");

        assertEquals("bob", a.getCustomerUsername());
        assertEquals("bob", b.getCustomerUsername());
        verify(authClient, times(1)).getUsernameForProvider(7L, "Bearer x");
    }
}

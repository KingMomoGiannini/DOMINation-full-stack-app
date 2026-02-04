package com.domination.booking.service;

import com.domination.booking.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static  org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private ReservationService reservationService;


    @Test
    void sanitiy_service_test(){
        assertNotNull(reservationService);
    }
}

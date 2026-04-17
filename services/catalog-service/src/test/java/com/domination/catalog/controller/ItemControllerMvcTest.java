package com.domination.catalog.controller;

import com.domination.catalog.config.SecurityConfig;
import com.domination.catalog.domain.ItemType;
import com.domination.catalog.domain.RentalMode;
import com.domination.catalog.dto.ItemDTO;
import com.domination.catalog.exception.GlobalExceptionHandler;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.service.ItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ItemController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ItemControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getItems_isPublicAndSupportsFilters() throws Exception {
        when(itemService.getItems(2L, ItemType.ROOM)).thenReturn(List.of(
                ItemDTO.builder()
                        .id(7L)
                        .branchId(2L)
                        .name("Sala B")
                        .type(ItemType.ROOM)
                        .rentalMode(RentalMode.TIME_EXCLUSIVE)
                        .basePrice(new BigDecimal("15000"))
                        .active(true)
                        .quantityTotal(0)
                        .build()
        ));

        mockMvc.perform(get("/api/catalog/items")
                        .param("branchId", "2")
                        .param("type", "ROOM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].type").value("ROOM"));
    }

    @Test
    void getItemById_returnsNotFoundWhenMissing() throws Exception {
        when(itemService.getItemById(88L))
                .thenThrow(new ResourceNotFoundException("Item no encontrado con id: 88"));

        mockMvc.perform(get("/api/catalog/items/88"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}

package com.domination.catalog.controller;

import com.domination.catalog.config.SecurityConfig;
import com.domination.catalog.dto.BranchDTO;
import com.domination.catalog.exception.GlobalExceptionHandler;
import com.domination.catalog.exception.ResourceNotFoundException;
import com.domination.catalog.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BranchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class BranchControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllBranches_isPublicAndReturnsData() throws Exception {
        when(branchService.getAllActiveBranches()).thenReturn(List.of(
                BranchDTO.builder()
                        .id(1L)
                        .name("Centro")
                        .address("Av. Siempre Viva 123")
                        .active(true)
                        .providerId(44L)
                        .build()
        ));

        mockMvc.perform(get("/api/catalog/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Centro"));
    }

    @Test
    void getBranchById_returnsNotFoundWhenMissing() throws Exception {
        when(branchService.getBranchById(99L))
                .thenThrow(new ResourceNotFoundException("Sucursal no encontrada con id: 99"));

        mockMvc.perform(get("/api/catalog/branches/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }
}

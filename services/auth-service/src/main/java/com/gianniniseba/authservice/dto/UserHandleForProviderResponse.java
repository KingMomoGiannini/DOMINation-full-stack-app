package com.gianniniseba.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos mínimos del cliente para contexto operativo del prestador (sin email ni roles).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHandleForProviderResponse {
    private Long userId;
    private String username;
}

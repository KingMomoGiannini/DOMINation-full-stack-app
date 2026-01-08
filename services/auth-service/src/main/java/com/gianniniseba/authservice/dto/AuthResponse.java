package com.gianniniseba.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String message;
    private String token;
}

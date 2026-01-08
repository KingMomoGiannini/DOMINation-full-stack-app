package com.domination.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    
    @NotBlank(message = "La dirección es obligatoria")
    private String address;
}



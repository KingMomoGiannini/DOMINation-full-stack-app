package com.domination.catalog.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDTO {
    private Long id;
    private String name;
    private String address;
    private Boolean active;
    private Long providerId;
}


package com.gianniniseba.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "provider_requests",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "status"})
    },
    indexes = {
        @Index(name = "idx_provider_requests_created_at_id", columnList = "created_at,id"),
        @Index(name = "idx_provider_requests_user_id_created_at_id", columnList = "user_id,created_at,id"),
        @Index(name = "idx_provider_requests_status_created_at_id", columnList = "status,created_at,id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }
}


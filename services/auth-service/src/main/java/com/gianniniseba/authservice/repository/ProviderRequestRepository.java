package com.gianniniseba.authservice.repository;

import com.gianniniseba.authservice.entity.ProviderRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRequestRepository extends JpaRepository<ProviderRequest, Long> {
    Optional<ProviderRequest> findByUserIdAndStatus(Long userId, ProviderRequest.RequestStatus status);
    Optional<ProviderRequest> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
    boolean existsByUserIdAndStatus(Long userId, ProviderRequest.RequestStatus status);
    List<ProviderRequest> findByStatus(ProviderRequest.RequestStatus status);
}


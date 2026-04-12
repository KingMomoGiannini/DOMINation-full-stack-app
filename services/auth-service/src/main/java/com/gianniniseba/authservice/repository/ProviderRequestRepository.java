package com.gianniniseba.authservice.repository;

import com.gianniniseba.authservice.entity.ProviderRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRequestRepository extends JpaRepository<ProviderRequest, Long>, JpaSpecificationExecutor<ProviderRequest> {
    Optional<ProviderRequest> findByUserIdAndStatus(Long userId, ProviderRequest.RequestStatus status);
    Optional<ProviderRequest> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    boolean existsByUserIdAndStatus(Long userId, ProviderRequest.RequestStatus status);

    @Query("""
            select new com.gianniniseba.authservice.dto.ProviderRequestSummaryDto(
                count(pr),
                coalesce(sum(case when pr.status = com.gianniniseba.authservice.entity.ProviderRequest.RequestStatus.PENDING then 1 else 0 end), 0),
                coalesce(sum(case when pr.status = com.gianniniseba.authservice.entity.ProviderRequest.RequestStatus.APPROVED then 1 else 0 end), 0),
                coalesce(sum(case when pr.status = com.gianniniseba.authservice.entity.ProviderRequest.RequestStatus.REJECTED then 1 else 0 end), 0)
            )
            from ProviderRequest pr
            """)
    com.gianniniseba.authservice.dto.ProviderRequestSummaryDto fetchAdminSummary();
}


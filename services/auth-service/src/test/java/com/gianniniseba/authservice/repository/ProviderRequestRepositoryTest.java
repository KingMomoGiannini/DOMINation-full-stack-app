package com.gianniniseba.authservice.repository;

import com.gianniniseba.authservice.dto.ProviderRequestSummaryDto;
import com.gianniniseba.authservice.entity.ProviderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class ProviderRequestRepositoryTest {

    @Autowired
    private ProviderRequestRepository providerRequestRepository;

    @Autowired
    private DataSource dataSource;

    @Test
    void fetchAdminSummary_aggregatesAllStatusesInSingleQuery() {
        providerRequestRepository.save(request(10L, ProviderRequest.RequestStatus.PENDING, LocalDateTime.now().minusDays(3)));
        providerRequestRepository.save(request(11L, ProviderRequest.RequestStatus.PENDING, LocalDateTime.now().minusDays(2)));
        providerRequestRepository.save(request(12L, ProviderRequest.RequestStatus.APPROVED, LocalDateTime.now().minusDays(1)));
        providerRequestRepository.save(request(13L, ProviderRequest.RequestStatus.REJECTED, LocalDateTime.now()));

        ProviderRequestSummaryDto summary = providerRequestRepository.fetchAdminSummary();

        assertEquals(4, summary.total());
        assertEquals(2, summary.pending());
        assertEquals(1, summary.approved());
        assertEquals(1, summary.rejected());
    }

    @Test
    void findFirstByUserIdOrderByCreatedAtDescIdDesc_returnsLatestStableRow() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(1);
        ProviderRequest first = providerRequestRepository.save(request(42L, ProviderRequest.RequestStatus.PENDING, createdAt));
        ProviderRequest second = providerRequestRepository.save(request(42L, ProviderRequest.RequestStatus.APPROVED, createdAt));

        ProviderRequest latest = providerRequestRepository
                .findFirstByUserIdOrderByCreatedAtDescIdDesc(42L)
                .orElseThrow();

        assertEquals(second.getId(), latest.getId());
        assertTrue(second.getId() > first.getId());
    }

    @Test
    void providerRequestTable_exposesExplicitPerformanceIndexes() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Set<String> indexNames = new HashSet<>();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "PROVIDER_REQUESTS", false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        indexNames.add(indexName.toUpperCase(Locale.ROOT));
                    }
                }
            }

            assertTrue(indexNames.contains("IDX_PROVIDER_REQUESTS_CREATED_AT_ID"));
            assertTrue(indexNames.contains("IDX_PROVIDER_REQUESTS_USER_ID_CREATED_AT_ID"));
            assertTrue(indexNames.contains("IDX_PROVIDER_REQUESTS_STATUS_CREATED_AT_ID"));
        }
    }

    private static ProviderRequest request(
            Long userId,
            ProviderRequest.RequestStatus status,
            LocalDateTime createdAt) {
        return ProviderRequest.builder()
                .userId(userId)
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}

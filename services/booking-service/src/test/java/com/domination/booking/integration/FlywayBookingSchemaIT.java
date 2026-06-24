package com.domination.booking.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que el historial Flyway del booking-service crea el esquema esperado en PostgreSQL real.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayBookingSchemaIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("booking_flyway_it")
            .withUsername("test")
            .withPassword("test");

    @Test
    void flywayMigrations_createReservationTables_andSnapshotColumns() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            DatabaseMetaData md = c.getMetaData();
            assertTableExists(md, "reservations");
            assertTableExists(md, "reservation_lines");
            assertTableExists(md, "reservation_audit_events");
            assertColumnExists(md, "reservations", "branch_name");
            assertColumnExists(md, "reservation_lines", "item_name");
            assertColumnExists(md, "reservation_audit_events", "event_type");
            assertColumnExists(md, "reservation_audit_events", "actor_user_id");
            assertIndexExists(md, "reservations", "idx_reservations_customer_start_id");
            assertIndexExists(md, "reservations", "idx_reservations_provider_start_id");
            assertIndexExists(md, "reservations", "idx_reservations_provider_branch_start_id");
            assertIndexExists(md, "reservation_audit_events", "idx_reservation_audit_events_reservation_id");
            assertIndexExists(md, "reservation_audit_events", "idx_reservation_audit_events_reservation_created_at");
        }
    }

    private static void assertTableExists(DatabaseMetaData md, String table) throws SQLException {
        try (ResultSet rs = md.getTables(null, "public", table, new String[]{"TABLE"})) {
            assertTrue(rs.next(), "tabla faltante: " + table);
        }
    }

    private static void assertColumnExists(DatabaseMetaData md, String table, String column) throws SQLException {
        try (ResultSet rs = md.getColumns(null, "public", table, column)) {
            assertTrue(rs.next(), "columna faltante: " + table + "." + column);
        }
    }

    private static void assertIndexExists(DatabaseMetaData md, String table, String indexName) throws SQLException {
        Set<String> indexNames = new HashSet<>();
        try (ResultSet rs = md.getIndexInfo(null, "public", table, false, false)) {
            while (rs.next()) {
                String current = rs.getString("INDEX_NAME");
                if (current != null) {
                    indexNames.add(current.toLowerCase(Locale.ROOT));
                }
            }
        }
        assertTrue(indexNames.contains(indexName.toLowerCase(Locale.ROOT)), "indice faltante: " + indexName);
    }
}

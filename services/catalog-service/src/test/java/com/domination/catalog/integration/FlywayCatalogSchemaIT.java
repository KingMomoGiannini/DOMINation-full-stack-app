package com.domination.catalog.integration;

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
 * Verifica que las migraciones Flyway de catalog-service creen el esquema esperado en PostgreSQL real.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayCatalogSchemaIT {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_flyway_it")
            .withUsername("test")
            .withPassword("test");

    @Test
    void flywayMigrations_createCatalogTablesColumnsAndIndexes() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            DatabaseMetaData metadata = connection.getMetaData();

            assertTableExists(metadata, "branches");
            assertTableExists(metadata, "rentable_items");
            assertTableExists(metadata, "inventory");
            assertTableExists(metadata, "inventory_holds");

            assertColumnExists(metadata, "branches", "provider_id");
            assertColumnExists(metadata, "rentable_items", "rental_mode");
            assertColumnExists(metadata, "inventory", "quantity_total");
            assertColumnExists(metadata, "inventory_holds", "created_at");

            assertIndexExists(metadata, "inventory", "uk_inventory_branch_item");
            assertIndexExists(metadata, "inventory_holds", "idx_inventory_holds_item_status_range");
            assertIndexExists(metadata, "rentable_items", "idx_rentable_items_branch_type_active");
        }
    }

    private static void assertTableExists(DatabaseMetaData metadata, String table) throws SQLException {
        try (ResultSet resultSet = metadata.getTables(null, "public", table, new String[]{"TABLE"})) {
            assertTrue(resultSet.next(), "tabla faltante: " + table);
        }
    }

    private static void assertColumnExists(DatabaseMetaData metadata, String table, String column) throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(null, "public", table, column)) {
            assertTrue(resultSet.next(), "columna faltante: " + table + "." + column);
        }
    }

    private static void assertIndexExists(DatabaseMetaData metadata, String table, String indexName) throws SQLException {
        Set<String> indexNames = new HashSet<>();
        try (ResultSet resultSet = metadata.getIndexInfo(null, "public", table, false, false)) {
            while (resultSet.next()) {
                String current = resultSet.getString("INDEX_NAME");
                if (current != null) {
                    indexNames.add(current.toLowerCase(Locale.ROOT));
                }
            }
        }
        assertTrue(indexNames.contains(indexName.toLowerCase(Locale.ROOT)), "indice faltante: " + indexName);
    }
}

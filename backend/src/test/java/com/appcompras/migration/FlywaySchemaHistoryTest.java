package com.appcompras.migration;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywaySchemaHistoryTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("appcompras_migration_test")
			.withUsername("appcompras_user")
			.withPassword("appcompras_pass");

	@DynamicPropertySource
	static void dataSourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
	}

	@Autowired
	private EntityManager entityManager;

	@Test
	void flywaySchemaHistoryHasExpectedMigrationCount() {
		Long count = (Long) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM flyway_schema_history"
		).getSingleResult();

		assertEquals(7L, count, "Expected exactly 7 applied migrations (V1 through V7)");
	}

	@Test
	void allMigrationsAreSuccessful() {
		Long successCount = (Long) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = true"
		).getSingleResult();

		Long totalCount = (Long) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM flyway_schema_history"
		).getSingleResult();

		assertEquals(totalCount, successCount, "All migrations should be successful");
	}

	@Test
	void migrationsAppliedInOrder() {
		var results = entityManager.createNativeQuery(
				"SELECT version FROM flyway_schema_history ORDER BY version ASC",
				String.class
		).getResultList();

		assertEquals(7, results.size(), "Expected 7 migrations");
		for (int i = 0; i < results.size(); i++) {
			String version = (String) results.get(i);
			assertEquals(String.valueOf(i + 1), version, "Migration V" + (i + 1) + " should be at position " + (i + 1));
		}
	}
}

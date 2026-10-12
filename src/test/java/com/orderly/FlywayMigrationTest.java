package com.orderly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class FlywayMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationCreatesTable() {

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name = 'flyway_test'
                """,
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }

    @Test
    void flywayMigrationHistoryIsRecorded() {

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1'
                  AND success = true
                """,
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }
}
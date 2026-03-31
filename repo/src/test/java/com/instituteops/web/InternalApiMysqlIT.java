package com.instituteops.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mysql-it")
class InternalApiMysqlIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
            "UPDATE sync_config SET enabled = ?, lan_only = ? WHERE sync_name = ?",
            false,
            false,
            "LAN_OPTIONAL_SYNC"
        );
    }

    @Test
    void internalPing_usesRealMysqlBackedCredentialsAndPolicy() {
        jdbcTemplate.update(
            "UPDATE sync_config SET enabled = ?, lan_only = ? WHERE sync_name = ?",
            true,
            false,
            "LAN_OPTIONAL_SYNC"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "local-sync-client");
        headers.add("X-API-SECRET", "internal-secret");

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/internal/ping",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("pong");
    }

    @Test
    void internalPing_rejectsInvalidCredentialsAgainstRealMysqlData() {
        jdbcTemplate.update(
            "UPDATE sync_config SET enabled = ?, lan_only = ? WHERE sync_name = ?",
            true,
            false,
            "LAN_OPTIONAL_SYNC"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-KEY", "local-sync-client");
        headers.add("X-API-SECRET", "wrong-secret");

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/internal/ping",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}

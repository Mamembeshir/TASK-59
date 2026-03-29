package com.instituteops.security.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "internal_api_clients")
public class InternalApiClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_key", nullable = false, unique = true, length = 128)
    private String clientKey;

    @Column(name = "client_secret_hash", nullable = false, length = 255)
    private String clientSecretHash;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Long getId() {
        return id;
    }

    public String getClientKey() {
        return clientKey;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}

package com.instituteops.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class ProductionSecurityValidator {

    private static final String DEV_DEFAULT_AES_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final Set<String> DEV_PROFILES = Set.of("docker", "test", "mysql-it", "dev", "default");

    private final Environment environment;
    private final String aesKeyBase64;

    public ProductionSecurityValidator(
        Environment environment,
        @Value("${app.encryption.aes-key-base64:}") String aesKeyBase64
    ) {
        this.environment = environment;
        this.aesKeyBase64 = aesKeyBase64;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityDefaults() {
        if (isDevProfile()) {
            return;
        }
        if (DEV_DEFAULT_AES_KEY.equals(aesKeyBase64)) {
            throw new IllegalStateException(
                "Default development AES encryption key detected in non-dev profile. "
                    + "Set APP_ENCRYPTION_AES_KEY_BASE64 to a securely generated key for production deployment."
            );
        }
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
            .anyMatch(DEV_PROFILES::contains)
            || environment.getActiveProfiles().length == 0;
    }
}

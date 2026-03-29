package com.instituteops.shared.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class EncryptionConverterInitializer {

    private final AesGcmStringEncryptor encryptor;

    public EncryptionConverterInitializer(AesGcmStringEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @PostConstruct
    void initialize() {
        AesStringAttributeConverter.setEncryptor(encryptor);
    }
}

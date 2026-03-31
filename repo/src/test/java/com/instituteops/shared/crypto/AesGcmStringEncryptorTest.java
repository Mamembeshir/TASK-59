package com.instituteops.shared.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmStringEncryptorTest {

    @Test
    void encryptAndDecrypt_roundTripWithAes256Key() {
        AesGcmStringEncryptor encryptor = new AesGcmStringEncryptor(propertiesForBytes(32));

        byte[] encrypted = encryptor.encrypt("sensitive-value");

        assertThat(encrypted).isNotNull();
        assertThat(encryptor.decrypt(encrypted)).isEqualTo("sensitive-value");
    }

    @Test
    void constructor_rejectsAes128AndAes192Keys() {
        assertThatThrownBy(() -> new AesGcmStringEncryptor(propertiesForBytes(16)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("exactly 32 bytes");

        assertThatThrownBy(() -> new AesGcmStringEncryptor(propertiesForBytes(24)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("exactly 32 bytes");
    }

    private static EncryptionProperties propertiesForBytes(int byteLength) {
        byte[] keyBytes = new byte[byteLength];
        for (int i = 0; i < keyBytes.length; i++) {
            keyBytes[i] = (byte) ('A' + (i % 26));
        }
        EncryptionProperties properties = new EncryptionProperties();
        properties.setAesKeyBase64(Base64.getEncoder().encodeToString(keyBytes));
        return properties;
    }
}

package com.instituteops.shared.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AesGcmStringEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AES_128_KEY_BYTES = 16;
    private static final int AES_192_KEY_BYTES = 24;
    private static final int AES_256_KEY_BYTES = 32;

    private static final Logger log = LoggerFactory.getLogger(AesGcmStringEncryptor.class);

    private final byte[] key;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmStringEncryptor(EncryptionProperties encryptionProperties) {
        if (encryptionProperties.getAesKeyBase64() == null || encryptionProperties.getAesKeyBase64().isBlank()) {
            throw new IllegalStateException("Missing app.encryption.aes-key-base64 configuration");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptionProperties.getAesKeyBase64().trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.encryption.aes-key-base64 must be valid Base64", ex);
        }
        if (decoded.length != AES_128_KEY_BYTES && decoded.length != AES_192_KEY_BYTES && decoded.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException("app.encryption.aes-key-base64 must decode to 16, 24, or 32 bytes");
        }
        log.info("Initialized AES-GCM encryptor with {}-bit key", decoded.length * 8);
        this.key = decoded;
    }

    public byte[] encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return payload;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt value", ex);
        }
    }

    public String decrypt(byte[] encryptedPayload) {
        if (encryptedPayload == null) {
            return null;
        }
        if (encryptedPayload.length <= IV_LENGTH_BYTES) {
            throw new IllegalStateException("Encrypted payload is invalid");
        }
        try {
            byte[] iv = Arrays.copyOfRange(encryptedPayload, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(encryptedPayload, IV_LENGTH_BYTES, encryptedPayload.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to decrypt value", ex);
        }
    }
}

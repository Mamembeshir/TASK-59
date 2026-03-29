package com.instituteops.shared.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    private String aesKeyBase64;

    public String getAesKeyBase64() {
        return aesKeyBase64;
    }

    public void setAesKeyBase64(String aesKeyBase64) {
        this.aesKeyBase64 = aesKeyBase64;
    }
}

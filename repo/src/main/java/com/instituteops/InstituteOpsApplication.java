package com.instituteops;

import com.instituteops.shared.crypto.EncryptionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(EncryptionProperties.class)
public class InstituteOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstituteOpsApplication.class, args);
    }
}

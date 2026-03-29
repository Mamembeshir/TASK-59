package com.instituteops.shared.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AesStringAttributeConverter implements AttributeConverter<String, byte[]> {

    private static AesGcmStringEncryptor encryptor;

    static void setEncryptor(AesGcmStringEncryptor encryptor) {
        AesStringAttributeConverter.encryptor = encryptor;
    }

    @Override
    public byte[] convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptor == null) {
            throw new IllegalStateException("AesStringAttributeConverter not initialized");
        }
        return encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptor == null) {
            throw new IllegalStateException("AesStringAttributeConverter not initialized");
        }
        return encryptor.decrypt(dbData);
    }
}

package com.recruiting.platform.pipeline.domain;

import com.recruiting.platform.security.FieldCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA converter for the encrypted JSON columns of this module ({@code search_brief.comp_band_enc},
 * {@code placement.compensation_enc}; R73): the JSON text is encrypted by {@link FieldCipher} of the security module
 * exactly like {@code person.compensation_enc}. The entity holds the JSON string; the service maps it to its DTO.
 * Envers audits the ciphertext. The key never reaches this class.
 */
@Converter
@Component
public class EncryptedJsonConverter implements AttributeConverter<String, byte[]> {

    private final FieldCipher cipher;

    public EncryptedJsonConverter(FieldCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public byte[] convertToDatabaseColumn(String json) {
        return json == null ? null : cipher.encrypt(json);
    }

    @Override
    public String convertToEntityAttribute(byte[] stored) {
        return stored == null ? null : cipher.decrypt(stored);
    }
}

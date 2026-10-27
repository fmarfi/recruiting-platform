package com.recruiting.platform.accounts.domain;

import com.recruiting.platform.security.FieldCipher;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * JPA converter for {@code person.compensation_enc} (ADR 0011): JSON of the {@link Compensation}, encrypted by
 * {@link FieldCipher} of the security module. Envers audits the ciphertext. The key never reaches this class.
 */
@Converter
@Component
public class CompensationEncryptionConverter implements AttributeConverter<Compensation, byte[]> {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final FieldCipher cipher;

    public CompensationEncryptionConverter(FieldCipher cipher) {
        this.cipher = cipher;
    }

    @Override
    public byte[] convertToDatabaseColumn(Compensation value) {
        if (value == null) {
            return null;
        }
        return cipher.encrypt(JSON.writeValueAsString(value).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Compensation convertToEntityAttribute(byte[] stored) {
        if (stored == null) {
            return null;
        }
        return JSON.readValue(new String(cipher.decrypt(stored), StandardCharsets.UTF_8), Compensation.class);
    }
}

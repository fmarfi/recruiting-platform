package com.recruiting.platform.accounts.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.recruiting.platform.contracts.CompensationPeriod;
import com.recruiting.platform.contracts.SalaryBasis;
import com.recruiting.platform.security.FieldCipher;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CompensationEncryptionConverterTest {

    /** Test double: reverses the bytes after a marker byte, so plain JSON can never be stored by accident. */
    static final class ReversingCipher implements FieldCipher {
        @Override
        public byte[] encrypt(String plaintext) {
            byte[] plain = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] out = new byte[plain.length + 1];
            out[0] = 7;
            for (int i = 0; i < plain.length; i++) {
                out[i + 1] = plain[plain.length - 1 - i];
            }
            return out;
        }

        @Override
        public String decrypt(byte[] data) {
            byte[] out = new byte[data.length - 1];
            for (int i = 0; i < out.length; i++) {
                out[i] = data[data.length - 1 - i];
            }
            return new String(out, StandardCharsets.UTF_8);
        }

        @Override
        public int currentKeyVersion() {
            return 1;
        }
    }

    private final CompensationEncryptionConverter converter = new CompensationEncryptionConverter(new ReversingCipher());

    @Test
    void roundTripsThroughTheCipher() {
        Compensation c = new Compensation(new BigDecimal("250000"), null, new BigDecimal("300000.50"), "TRY",
                CompensationPeriod.ANNUAL, SalaryBasis.GROSS, "Car, health insurance", LocalDate.of(2026, 10, 1));
        byte[] stored = converter.convertToDatabaseColumn(c);
        assertThat(new String(stored, StandardCharsets.UTF_8)).doesNotContain("TRY").doesNotContain("ANNUAL");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(c);
    }

    @Test
    void nullStaysNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

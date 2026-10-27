package com.recruiting.platform;

import com.recruiting.platform.security.FieldCipher;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * FALLBACK until security-privacy's {@code FieldCipher} implementation is merged (it replaces this bean:
 * {@link ConditionalOnMissingBean}). AES-256-GCM, format as in schema.md (1 byte key version, 12 byte IV, ciphertext
 * + tag). The key comes from {@code app.security.field-encryption-key} (Base64, 32 bytes). Without a key the bean
 * uses a random in-memory key (data written is unreadable after a restart) and refuses to start in the pilot profile.
 */
@Configuration
class FallbackFieldCipherConfig {

    private static final Logger LOG = LoggerFactory.getLogger(FallbackFieldCipherConfig.class);

    @Bean
    @ConditionalOnMissingBean(FieldCipher.class)
    FieldCipher fallbackFieldCipher(@Value("${app.security.field-encryption-key:}") String key, Environment env) {
        byte[] bytes;
        if (key.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("pilot"))) {
                throw new IllegalStateException("app.security.field-encryption-key is required in the pilot profile");
            }
            LOG.warn("No field encryption key configured: using an EPHEMERAL key (development only).");
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(key);
            if (bytes.length != 32) {
                throw new IllegalStateException("app.security.field-encryption-key must be 32 bytes, Base64 encoded");
            }
        }
        return new AesGcmFieldCipher(bytes);
    }

    static final class AesGcmFieldCipher implements FieldCipher {
        private static final byte VERSION = 1;
        private final SecretKeySpec key;
        private final SecureRandom random = new SecureRandom();

        AesGcmFieldCipher(byte[] key) {
            this.key = new SecretKeySpec(key, "AES");
        }

        @Override
        public byte[] encrypt(byte[] plaintext) {
            try {
                byte[] iv = new byte[12];
                random.nextBytes(iv);
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
                byte[] ct = c.doFinal(plaintext);
                byte[] out = new byte[1 + 12 + ct.length];
                out[0] = VERSION;
                System.arraycopy(iv, 0, out, 1, 12);
                System.arraycopy(ct, 0, out, 13, ct.length);
                return out;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("encryption failed", e);
            }
        }

        @Override
        public byte[] decrypt(byte[] data) {
            if (data.length < 13 + 16 || data[0] != VERSION) {
                throw new IllegalStateException("unknown ciphertext format or key version");
            }
            try {
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(data, 1, 13)));
                return c.doFinal(data, 13, data.length - 13);
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("decryption failed", e);
            }
        }
    }
}

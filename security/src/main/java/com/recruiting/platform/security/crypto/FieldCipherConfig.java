package com.recruiting.platform.security.crypto;

import com.recruiting.platform.security.FieldCipher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Builds the {@link FieldCipher} from {@code FIELD_ENCRYPTION_KEY_V<k>} (base64, 32 bytes) and
 * {@code FIELD_ENCRYPTION_KEY_VERSION} (default 1). In the {@code pilot} and {@code prod} profiles a missing key stops
 * the start. Anywhere else (local, test, ITs) a missing key falls back to a deterministic development key that is
 * public in the source, with a warning; it must never hold real data.
 */
@Configuration(proxyBeanMethods = false)
class FieldCipherConfig {

    private static final Logger log = LoggerFactory.getLogger(FieldCipherConfig.class);

    @Bean
    FieldCipher fieldCipher(Environment env) {
        boolean strict = List.of(env.getActiveProfiles()).stream().anyMatch(p -> p.equals("pilot") || p.equals("prod"));
        int current = Integer.parseInt(env.getProperty("FIELD_ENCRYPTION_KEY_VERSION", "1").trim());
        Map<Integer, byte[]> keys = new HashMap<>();
        for (int v = 1; v <= 255; v++) {
            String value = env.getProperty("FIELD_ENCRYPTION_KEY_V" + v);
            if (value != null && !value.isBlank()) {
                keys.put(v, Base64.getDecoder().decode(value.trim()));
            }
        }
        if (!keys.containsKey(current)) {
            if (strict) {
                throw new IllegalStateException("FIELD_ENCRYPTION_KEY_V" + current
                        + " is not set (FIELD_ENCRYPTION_KEY_VERSION=" + current + "); refusing to start without it.");
            }
            log.warn("FIELD_ENCRYPTION_KEY_V{} is not set: using the public development key. Never use real data.",
                    current);
            keys.put(current, devKey(current));
        }
        return new AesGcmFieldCipher(keys, current);
    }

    /** Deterministic, public development key for one version. */
    static byte[] devKey(int version) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(("dev-only-field-encryption-key-v" + version).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

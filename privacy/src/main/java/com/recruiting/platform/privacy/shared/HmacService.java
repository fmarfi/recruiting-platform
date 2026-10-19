package com.recruiting.platform.privacy.shared;

import com.recruiting.platform.contracts.ApiException;
import com.recruiting.platform.contracts.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Keyed hash of an e-mail address for minimised data-subject requests (ADR 0009). The key comes from the environment
 * ({@code PRIVACY_HMAC_KEY}, version {@code PRIVACY_HMAC_KEY_VERSION}), never from the database. Without a key the
 * erasure is refused rather than falling back to a plain hash that could be reversed by guessing addresses.
 */
@Component
public class HmacService {

    private final String key;
    private final String version;

    HmacService(@Value("${app.privacy.hmac-key:}") String key,
            @Value("${app.privacy.hmac-key-version:1}") String version) {
        this.key = key;
        this.version = version;
    }

    /** {@code hmac:v<k>:<hex>} of the trimmed, lower-cased e-mail address. */
    public String hashEmail(String email) {
        if (key == null || key.isBlank()) {
            throw new ApiException(ErrorCode.STATE_CONFLICT,
                    "PRIVACY_HMAC_KEY is not configured, so personal data cannot be erased safely.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return "hmac:v" + version + ":" + HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

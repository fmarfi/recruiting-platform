package com.recruiting.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recruiting.platform.security.crypto.AesGcmFieldCipher;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

class FieldCipherTest {

    private static byte[] key(int fill) {
        byte[] k = new byte[32];
        Arrays.fill(k, (byte) fill);
        return k;
    }

    @Test
    void roundTripAndRandomIv() {
        AesGcmFieldCipher c = new AesGcmFieldCipher(Map.of(1, key(1)), 1);
        byte[] a = c.encrypt("{\"gross\":100000}");
        byte[] b = c.encrypt("{\"gross\":100000}");
        assertThat(a).isNotEqualTo(b);
        assertThat(a[0]).isEqualTo((byte) 1);
        assertThat(new String(a, StandardCharsets.ISO_8859_1)).doesNotContain("gross");
        assertThat(c.decrypt(a)).isEqualTo("{\"gross\":100000}");
        assertThat(c.encrypt(null)).isNull();
        assertThat(c.decrypt(null)).isNull();
    }

    @Test
    void rotationKeepsOldValuesReadable() {
        byte[] old = new AesGcmFieldCipher(Map.of(1, key(1)), 1).encrypt("x");
        AesGcmFieldCipher rotated = new AesGcmFieldCipher(Map.of(1, key(1), 2, key(2)), 2);
        assertThat(rotated.decrypt(old)).isEqualTo("x");
        assertThat(rotated.encrypt("y")[0]).isEqualTo((byte) 2);
    }

    @Test
    void tamperedOrWrongKeyFails() {
        AesGcmFieldCipher c = new AesGcmFieldCipher(Map.of(1, key(1)), 1);
        byte[] enc = c.encrypt("secret");
        byte[] bad = enc.clone();
        bad[bad.length - 1] ^= 1;
        assertThatThrownBy(() -> c.decrypt(bad)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AesGcmFieldCipher(Map.of(1, key(9)), 1).decrypt(enc))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("secret");
        assertThatThrownBy(() -> new AesGcmFieldCipher(Map.of(1, new byte[16]), 1))
                .isInstanceOf(IllegalStateException.class);
    }

    private AnnotationConfigApplicationContext context(String profile, Map<String, Object> props) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles(profile);
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("t", props));
        ctx.scan("com.recruiting.platform.security.crypto");
        return ctx;
    }

    @Test
    void pilotProfileFailsFastWithoutKey() {
        try (AnnotationConfigApplicationContext ctx = context("pilot", Map.of())) {
            assertThatThrownBy(ctx::refresh).hasStackTraceContaining("FIELD_ENCRYPTION_KEY_V1 is not set");
        }
    }

    @Test
    void localProfileUsesDevKeyAndPilotUsesConfiguredKey() {
        try (AnnotationConfigApplicationContext ctx = context("local", Map.of())) {
            ctx.refresh();
            FieldCipher c = ctx.getBean(FieldCipher.class);
            assertThat(c.decrypt(c.encrypt("a"))).isEqualTo("a");
        }
        String b64 = Base64.getEncoder().encodeToString(key(7));
        try (AnnotationConfigApplicationContext ctx = context("pilot",
                Map.of("FIELD_ENCRYPTION_KEY_V3", b64, "FIELD_ENCRYPTION_KEY_VERSION", "3"))) {
            ctx.refresh();
            assertThat(ctx.getBean(FieldCipher.class).currentKeyVersion()).isEqualTo(3);
        }
    }
}

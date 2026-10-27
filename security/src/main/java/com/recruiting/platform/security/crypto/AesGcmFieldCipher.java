package com.recruiting.platform.security.crypto;

import com.recruiting.platform.security.FieldCipher;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM with versioned keys. Keys are 32 bytes; see {@link FieldCipherConfig} for how they are loaded. */
public final class AesGcmFieldCipher implements FieldCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final Map<Integer, SecretKeySpec> keys;
    private final int currentVersion;
    private final SecureRandom random = new SecureRandom();

    public AesGcmFieldCipher(Map<Integer, byte[]> rawKeys, int currentVersion) {
        if (currentVersion < 1 || currentVersion > 255) {
            throw new IllegalStateException("Field encryption key version must be 1..255");
        }
        if (!rawKeys.containsKey(currentVersion)) {
            throw new IllegalStateException("No field encryption key for current version " + currentVersion);
        }
        Map<Integer, SecretKeySpec> copy = new HashMap<>();
        rawKeys.forEach((version, key) -> {
            if (key == null || key.length != 32) {
                throw new IllegalStateException("Field encryption key V" + version + " must be 32 bytes (base64)");
            }
            copy.put(version, new SecretKeySpec(key, "AES"));
        });
        this.keys = Map.copyOf(copy);
        this.currentVersion = currentVersion;
    }

    @Override
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(currentVersion), new GCMParameterSpec(TAG_BITS, iv));
            byte[] body = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + IV_LENGTH + body.length).put((byte) currentVersion).put(iv).put(body).array();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Field encryption failed", ex);
        }
    }

    @Override
    public String decrypt(byte[] data) {
        if (data == null) {
            return null;
        }
        if (data.length < 1 + IV_LENGTH + TAG_BITS / 8) {
            throw new IllegalArgumentException("Encrypted value is too short");
        }
        int version = data[0] & 0xFF;
        SecretKeySpec key = keys.get(version);
        if (key == null) {
            throw new IllegalStateException("No field encryption key for version " + version);
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 1, IV_LENGTH));
            return new String(cipher.doFinal(data, 1 + IV_LENGTH, data.length - 1 - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Field decryption failed (wrong key or tampered data)", ex);
        }
    }

    @Override
    public int currentKeyVersion() {
        return currentVersion;
    }
}

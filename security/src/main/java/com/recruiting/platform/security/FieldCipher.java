package com.recruiting.platform.security;

/**
 * Field-level encryption for small personal values (ADR 0011: compensation). AES-256-GCM; the result is
 * {@code 1 byte key version | 12 byte IV | ciphertext + 16 byte tag}, so keys can rotate by adding a version.
 * Implementations never log plaintext or keys. {@code null} in, {@code null} out.
 */
public interface FieldCipher {

    /** Encrypts with the current key version. */
    byte[] encrypt(String plaintext);

    /** Decrypts with the key version named in the first byte; a wrong key or tampered data throws. */
    String decrypt(byte[] ciphertext);

    /** The version new values are encrypted with. */
    int currentKeyVersion();
}

package com.recruiting.platform.security;

/**
 * Public API of the security module: field-level encryption for single columns (ADR 0011, AES-256-GCM, key outside
 * the database). Format of the ciphertext: 1 byte key version, 12 byte IV, ciphertext + tag. Other modules call it from
 * their JPA converters; they never see keys.
 *
 * <p>NOTE: this minimal interface was created by backend-accounts (Wave 2b) because security-privacy had not merged
 * its version yet. The orchestrator keeps the security-privacy file at merge; method names must stay identical.
 */
public interface FieldCipher {

    /** Encrypts with the current key version; never returns null. */
    byte[] encrypt(byte[] plaintext);

    /**
     * Decrypts with the key version named in the first byte.
     *
     * @throws IllegalStateException when the key version is unknown or the data was tampered with
     */
    byte[] decrypt(byte[] ciphertext);
}

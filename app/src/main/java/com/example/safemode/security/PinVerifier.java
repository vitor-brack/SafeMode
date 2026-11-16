package com.example.safemode.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Regras de hash e conferência dos PINs, sem depender do Android.
 * O hash é o SHA-256 de (salt + PIN em UTF-8), o mesmo formato que o PinManager já grava,
 * então os PINs guardados nos aparelhos continuam valendo.
 */
public final class PinVerifier {

    public static final int PIN_LENGTH = 4;
    private static final int SALT_LENGTH = 16;

    private PinVerifier() {
    }

    // Só confere o tamanho: letras também passam (comportamento herdado do PinManager)
    public static boolean isWellFormed(String pin) {
        return pin != null && pin.length() == PIN_LENGTH;
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static byte[] hash(String pin, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(pin.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public static boolean matches(String pin, byte[] salt, byte[] expectedHash) {
        return Arrays.equals(hash(pin, salt), expectedHash);
    }

    /**
     * Confere o PIN principal. Sem PIN gravado (salt ou hash nulo), qualquer PIN bem formado
     * passa: é o que o PinManager sempre fez.
     */
    public static boolean verifyPrimary(String pin, byte[] salt, byte[] storedHash) {
        if (!isWellFormed(pin)) {
            return false;
        }
        if (salt == null || storedHash == null) {
            return true;
        }
        return matches(pin, salt, storedHash);
    }

    // Confere o PIN secundário. Sem PIN gravado, nada passa.
    public static boolean verifySecondary(String pin, byte[] salt, byte[] storedHash) {
        if (!isWellFormed(pin)) {
            return false;
        }
        if (salt == null || storedHash == null) {
            return false;
        }
        return matches(pin, salt, storedHash);
    }
}

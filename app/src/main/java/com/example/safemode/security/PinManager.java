package com.example.safemode.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;


/**
 * Gerenciador de PINs do aplicativo SafeMode.
 * Guarda e lê os PINs no SharedPreferences (hash SHA-256 com salt aleatório, em Base64).
 * As regras de hash e conferência ficam em {@link PinVerifier}, que não depende do Android.
 * Gerencia tanto o PIN principal quanto o PIN secundário.
 */
public class PinManager {

    private static final String PREF_NAME = "LockScreenPrefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_SECONDARY_PIN_HASH = "secondary_pin_hash";
    private static final String KEY_SECONDARY_PIN_SALT = "secondary_pin_salt";
    private final SharedPreferences preferences;

    // Inicializa o gerenciador de PINs com contexto e SharedPreferences
    public PinManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Define o PIN principal de 4 dígitos com hash seguro usando SHA-256
    public boolean setPin(String pin) {
        return savePin(pin, KEY_PIN_HASH, KEY_PIN_SALT);
    }

    // Verifica se o PIN principal informado corresponde ao armazenado
    public boolean verifyPin(String pin) {
        try {
            return PinVerifier.verifyPrimary(pin, readBytes(KEY_PIN_SALT), readBytes(KEY_PIN_HASH));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifica se existe um PIN principal configurado
    public boolean hasPin() {
        String storedHash = preferences.getString(KEY_PIN_HASH, null);
        return storedHash != null && !storedHash.isEmpty();
    }

    // Remove o PIN principal armazenado e seu salt
    public void clearPin() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_PIN_HASH);
        editor.remove(KEY_PIN_SALT);
        editor.apply();
    }

    // Define o PIN secundário de 4 dígitos com hash seguro usando SHA-256
    public boolean setSecondaryPin(String pin) {
        return savePin(pin, KEY_SECONDARY_PIN_HASH, KEY_SECONDARY_PIN_SALT);
    }

    // Verifica se o PIN secundário informado corresponde ao armazenado
    public boolean verifySecondaryPin(String pin) {
        try {
            return PinVerifier.verifySecondary(pin,
                    readBytes(KEY_SECONDARY_PIN_SALT), readBytes(KEY_SECONDARY_PIN_HASH));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Verifica se existe um PIN secundário configurado
    public boolean hasSecondaryPin() {
        String storedHash = preferences.getString(KEY_SECONDARY_PIN_HASH, null);
        return storedHash != null && !storedHash.isEmpty();
    }

    // Verifica o tipo de PIN informado: 1 para principal, 2 para secundário, 0 para inválido
    public int verifyPinType(String pin) {
        if (verifyPin(pin)) {
            return 1;
        } else if (verifySecondaryPin(pin)) {
            return 2;
        }
        return 0;
    }

    // Gera um salt novo, calcula o hash e grava os dois em Base64 sob as chaves informadas
    private boolean savePin(String pin, String hashKey, String saltKey) {
        try {
            if (!PinVerifier.isWellFormed(pin)) {
                return false;
            }

            byte[] salt = PinVerifier.newSalt();
            byte[] hash = PinVerifier.hash(pin, salt);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(hashKey, Base64.encodeToString(hash, Base64.NO_WRAP));
            editor.putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP));
            return editor.commit();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Lê um valor em Base64 do SharedPreferences; null se não existir
    private byte[] readBytes(String key) {
        String value = preferences.getString(key, null);
        return value == null ? null : Base64.decode(value, Base64.NO_WRAP);
    }
}

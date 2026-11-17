package com.example.safemode.security;

/**
 * Requisitos para ligar a tela de PIN, sem depender do Android.
 * A ordem das checagens é a que a MainActivity sempre usou: cada falta tem a própria mensagem,
 * então a ordem decide qual delas o usuário vê.
 */
public final class LockScreenRules {

    public enum Requirement {
        MET,
        NOT_DEFAULT_LAUNCHER,
        PRIMARY_PIN_MISSING,
        SECONDARY_PIN_MISSING
    }

    private LockScreenRules() {
    }

    /**
     * @param isDefaultLauncher se o SafeMode é o launcher padrão do celular
     * @param hasPrimaryPin se há PIN principal gravado
     * @param hasSecondaryPin se há PIN secundário gravado
     */
    public static Requirement check(boolean isDefaultLauncher, boolean hasPrimaryPin,
                                    boolean hasSecondaryPin) {
        if (!isDefaultLauncher) {
            return Requirement.NOT_DEFAULT_LAUNCHER;
        }
        if (!hasPrimaryPin) {
            return Requirement.PRIMARY_PIN_MISSING;
        }
        if (!hasSecondaryPin) {
            return Requirement.SECONDARY_PIN_MISSING;
        }
        return Requirement.MET;
    }
}

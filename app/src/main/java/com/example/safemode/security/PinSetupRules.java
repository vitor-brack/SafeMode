package com.example.safemode.security;

/**
 * Regras de cadastro dos PINs, sem depender do Android.
 * A ordem das checagens é a que a MainActivity sempre usou: cada uma tem a própria mensagem,
 * então a ordem decide qual delas o usuário vê.
 */
public final class PinSetupRules {

    public enum Result {
        OK,
        INVALID_LENGTH,
        SAME_AS_SECONDARY,
        SAME_AS_PRIMARY,
        PRIMARY_NOT_SET
    }

    private PinSetupRules() {
    }

    /**
     * Confere o PIN principal que está sendo cadastrado.
     *
     * @param secondaryConfigured se já existe um PIN secundário gravado
     * @param pinMatchesSecondary se o PIN digitado confere com o secundário gravado
     */
    public static Result validatePrimary(String pin, boolean secondaryConfigured,
                                         boolean pinMatchesSecondary) {
        if (!PinVerifier.isWellFormed(pin)) {
            return Result.INVALID_LENGTH;
        }
        if (secondaryConfigured && pinMatchesSecondary) {
            return Result.SAME_AS_SECONDARY;
        }
        return Result.OK;
    }

    /**
     * Confere o PIN secundário que está sendo cadastrado.
     *
     * @param primaryConfigured se já existe um PIN principal gravado
     * @param pinMatchesPrimary se o PIN digitado confere com o principal gravado
     */
    public static Result validateSecondary(String pin, boolean primaryConfigured,
                                           boolean pinMatchesPrimary) {
        if (!PinVerifier.isWellFormed(pin)) {
            return Result.INVALID_LENGTH;
        }
        if (!primaryConfigured) {
            return Result.PRIMARY_NOT_SET;
        }
        if (pinMatchesPrimary) {
            return Result.SAME_AS_PRIMARY;
        }
        return Result.OK;
    }
}

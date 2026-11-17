package com.example.safemode.ui;

import android.content.Context;
import android.widget.Toast;

import com.example.safemode.security.PinManager;
import com.example.safemode.security.PinSetupRules;

/**
 * Cadastro dos PINs principal e secundário: confere as regras, grava no PinManager e mostra a
 * mensagem de cada resultado. Quem chama só decide o que fazer com o campo de texto depois.
 */
final class PinSetupController {

    private final Context context;
    private final PinManager pinManager;

    PinSetupController(Context context, PinManager pinManager) {
        this.context = context;
        this.pinManager = pinManager;
    }

    // Valida e grava o PIN principal. Devolve true se gravou
    boolean savePrimary(String pin) {
        PinSetupRules.Result check = PinSetupRules.validatePrimary(pin,
                pinManager.hasSecondaryPin(), pinManager.verifySecondaryPin(pin));

        if (check == PinSetupRules.Result.INVALID_LENGTH) {
            show("PIN deve ter 4 dígitos!");
            return false;
        }

        if (check == PinSetupRules.Result.SAME_AS_SECONDARY) {
            show("PIN principal não pode ser igual ao PIN secundário!");
            return false;
        }

        if (pinManager.setPin(pin)) {
            show("PIN configurado com sucesso!");
            return true;
        }

        show("Erro ao salvar PIN");
        return false;
    }

    // Valida e grava o PIN secundário. Devolve true se gravou
    boolean saveSecondary(String pin) {
        PinSetupRules.Result check = PinSetupRules.validateSecondary(pin,
                pinManager.hasPin(), pinManager.verifyPin(pin));

        if (check == PinSetupRules.Result.INVALID_LENGTH) {
            show("PIN secundário deve ter 4 dígitos!");
            return false;
        }

        if (check == PinSetupRules.Result.PRIMARY_NOT_SET) {
            show("Configure o PIN principal primeiro!");
            return false;
        }

        if (check == PinSetupRules.Result.SAME_AS_PRIMARY) {
            show("PIN secundário deve ser diferente do principal!");
            return false;
        }

        if (pinManager.setSecondaryPin(pin)) {
            show("PIN secundário configurado com sucesso!");
            return true;
        }

        show("Erro ao salvar PIN secundário");
        return false;
    }

    private void show(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

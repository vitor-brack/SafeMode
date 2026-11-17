package com.example.safemode.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.example.safemode.R;

/**
 * Molde dos diálogos que pedem uma permissão ou um requisito e levam às configurações do app.
 * A view já vem inflada (e personalizada) por quem chama; o layout precisa ter os botões
 * {@code btnGoToSettings} e {@code btnCancel}.
 */
final class GoToSettingsDialog {

    private GoToSettingsDialog() {
    }

    /**
     * Mostra o diálogo: "ir às configurações" abre a SettingsActivity e fecha o diálogo;
     * "cancelar" roda {@code onCancel} (pode ser nulo) e fecha o diálogo.
     */
    static void show(Activity activity, View dialogView, Runnable onCancel) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnGoToSettings = dialogView.findViewById(R.id.btnGoToSettings);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnGoToSettings.setOnClickListener(v -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            activity.startActivity(intent);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (onCancel != null) {
                onCancel.run();
            }
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }
}

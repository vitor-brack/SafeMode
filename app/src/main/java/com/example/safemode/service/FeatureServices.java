package com.example.safemode.service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.safemode.data.AppPreferences;

/**
 * Liga e desliga os dois recursos que dependem de um serviço: grava a configuração e inicia ou
 * para o serviço correspondente. Os erros sobem para quem chama, que decide o que mostrar.
 */
public final class FeatureServices {

    private final Context context;
    private final AppPreferences preferences;

    public FeatureServices(Context context, AppPreferences preferences) {
        this.context = context;
        this.preferences = preferences;
    }

    // Ativa o modo seguro e inicia o serviço de monitoramento
    public void startSafeMode() {
        preferences.setSafeModeEnabled(true);

        Intent serviceIntent = new Intent(context, SafeModeService.class);
        context.startService(serviceIntent);
    }

    // Desativa o modo seguro e para o serviço de monitoramento
    public void stopSafeMode() {
        preferences.setSafeModeEnabled(false);

        Intent serviceIntent = new Intent(context, SafeModeService.class);
        context.stopService(serviceIntent);
    }

    // Ativa a tela de bloqueio e inicia o serviço que a abre quando a tela acende
    public void startLockScreen() {
        preferences.setLockScreenEnabled(true);

        Intent serviceIntent = new Intent(context, LockScreenService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    // Desativa a tela de bloqueio e para o serviço correspondente
    public void stopLockScreen() {
        preferences.setLockScreenEnabled(false);

        Intent serviceIntent = new Intent(context, LockScreenService.class);
        context.stopService(serviceIntent);
    }
}

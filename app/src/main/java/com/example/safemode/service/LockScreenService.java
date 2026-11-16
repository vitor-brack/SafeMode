package com.example.safemode.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.ui.LockScreenActivity;

/**
 * Service em foreground que monitora quando a tela liga e exibe a tela de bloqueio.
 * Escuta broadcasts de SCREEN_ON para mostrar a LockScreenActivity automaticamente.
 */
public class LockScreenService extends Service {

    private static final String CHANNEL_ID = "LockScreenChannel";
    private static final int NOTIFICATION_ID = 2;
    private ScreenReceiver screenReceiver;
    private AppPreferences preferences;

    // Inicializa o service e registra o receiver para monitorar a tela
    @Override
    public void onCreate() {
        super.onCreate();
        preferences = new AppPreferences(this);

        screenReceiver = new ScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(screenReceiver, filter);

        startForeground(NOTIFICATION_ID, createNotification());
    }

    // Retorna START_STICKY para o service reiniciar se for morto pelo sistema
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    // Remove o receiver ao destruir o service
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (screenReceiver != null) {
            unregisterReceiver(screenReceiver);
        }
    }

    // Retorna null pois este service não é bindable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Cria a notificação do service em foreground
    private Notification createNotification() {
        createNotificationChannel();

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Safe Mode - Bloqueio de Tela")
                .setContentText("Proteção ativa")
                .setSmallIcon(R.drawable.ic_logo_safe_mode)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    // Cria o canal de notificações para Android O e superiores
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Bloqueio de Tela",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Serviço de bloqueio de tela do Safe Mode");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // BroadcastReceiver que escuta quando a tela liga
    private class ScreenReceiver extends BroadcastReceiver {
        // Recebe broadcasts de SCREEN_ON e exibe a tela de bloqueio
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }

            if (!preferences.isLockScreenEnabled()) {
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                showLockScreen();
            }
        }

        // Exibe a LockScreenActivity ao ligar a tela
        private void showLockScreen() {
            Intent lockIntent = new Intent(LockScreenService.this, LockScreenActivity.class);
            lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                    Intent.FLAG_ACTIVITY_NO_ANIMATION |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(lockIntent);
        }
    }
}

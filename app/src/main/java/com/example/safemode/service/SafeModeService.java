package com.example.safemode.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.location.LocationManager;
import com.example.safemode.ui.MainActivity;

/**
 * Serviço em foreground que gerencia o monitoramento do modo seguro.
 * Executa continuamente enquanto o modo seguro está ativo, monitora a localização
 * do usuário e exibe notificação permanente indicando o status do bloqueio.
 */
public class SafeModeService extends Service implements LocationManager.LocationUpdateListener {

    private static final String CHANNEL_ID = "SafeModeService";
    private static final int NOTIFICATION_ID = 1001;
    private LocationManager locationManager;
    private AppPreferences preferences;
    private boolean isLocationMonitoringActive = false;

    // Inicializa o serviço e suas dependências
    @Override
    public void onCreate() {
        super.onCreate();

        preferences = new AppPreferences(this);
        locationManager = new LocationManager(this);
        locationManager.setLocationUpdateListener(this);

        createNotificationChannel();
    }

    // Inicia o serviço em foreground e ativa monitoramento de localização se necessário
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!preferences.isSafeModeEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification());

        if (preferences.isLocationEnabled()) {
            startLocationMonitoring();
        }

        return START_STICKY;
    }

    // Para o monitoramento de localização ao destruir o serviço
    @Override
    public void onDestroy() {
        super.onDestroy();

        stopLocationMonitoring();
    }

    // Retorna null pois este serviço não é bindable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Callback chamado quando a localização muda
    @Override
    public void onLocationChanged(boolean isInsideAllowedArea) {
        updateNotification(isInsideAllowedArea);
    }

    // Callback chamado quando ocorre erro na obtenção de localização
    @Override
    public void onLocationError(String error) {
    }

    // Inicia o monitoramento de localização em tempo real
    private void startLocationMonitoring() {
        if (!isLocationMonitoringActive) {
            locationManager.startLocationUpdates();
            isLocationMonitoringActive = true;
        }
    }

    // Para o monitoramento de localização
    private void stopLocationMonitoring() {
        if (isLocationMonitoringActive) {
            locationManager.stopLocationUpdates();
            isLocationMonitoringActive = false;
        }
    }

    // Cria o canal de notificação para Android O e superior
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safe Mode",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Monitoramento do Safe Mode");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // Cria a notificação inicial do serviço
    private Notification createNotification() {
        return createNotificationWithStatus(true);
    }

    // Atualiza a notificação com o status atual da localização
    private void updateNotification(boolean isInsideAllowedArea) {
        Notification notification = createNotificationWithStatus(isInsideAllowedArea);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    // Cria a notificação com status personalizado baseado na localização
    private Notification createNotificationWithStatus(boolean isInsideAllowedArea) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = getString(R.string.notification_title);
        String text;
        int iconColor;

        if (preferences.isLocationEnabled()) {
            if (isInsideAllowedArea) {
                text = "Área permitida - Apps liberados";
                iconColor = 0xFF4CAF50;
            } else {
                text = "Fora da área - Apps bloqueados";
                iconColor = 0xFFFF9800;
            }
        } else {
            text = getString(R.string.notification_description);
            iconColor = 0xFF2196F3;
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_logo_safe_mode)
                .setColor(iconColor)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }
}
package com.example.safemode.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.ui.MainActivity;

/**
 * Service em foreground que monitora continuamente a localização do dispositivo.
 * Verifica se o usuário está dentro da área permitida e exibe notificações de status.
 */
public class LocationService extends Service implements LocationListener {

    private static final String CHANNEL_ID = "LocationService";
    private static final int NOTIFICATION_ID = 1002;
    private static final long UPDATE_INTERVAL = 30000;
    private static final float MIN_DISTANCE = 10;
    private LocationManager systemLocationManager;
    private AppPreferences preferences;
    private Location currentLocation;
    private LocationUpdateListener listener;

    // Inicializa o service e cria o canal de notificações
    @Override
    public void onCreate() {
        super.onCreate();

        preferences = new AppPreferences(this);
        systemLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        createNotificationChannel();
    }

    // Inicia o service em foreground e começa o monitoramento de localização
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!preferences.isLocationEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification());

        startLocationUpdates();

        return START_STICKY;
    }

    // Para as atualizações de localização quando o service é destruído
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    // Retorna o binder para permitir binding com o service
    @Override
    public IBinder onBind(Intent intent) {
        return new LocationBinder();
    }

    // Callback chamado quando a localização muda
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;

        boolean isOutside = isOutsideAllowedArea(location);

        updateNotification(!isOutside);

        if (listener != null) {
            listener.onLocationChanged(!isOutside);
        }

        sendLocationBroadcast(!isOutside, location);
    }

    // Callback chamado quando o status do provedor muda (não implementado)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // Callback chamado quando um provedor é habilitado (não implementado)
    @Override
    public void onProviderEnabled(String provider) {
    }

    // Callback chamado quando um provedor é desabilitado
    @Override
    public void onProviderDisabled(String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            notifyLocationError("GPS foi desligado");
        }
    }

    // Inicia atualizações contínuas de localização dos provedores GPS e Network
    private void startLocationUpdates() {
        try {
            if (!hasLocationPermission()) {
                stopSelf();
                return;
            }

            if (!isLocationEnabled()) {
                notifyLocationError("GPS está desligado");
                return;
            }

            systemLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    UPDATE_INTERVAL,
                    MIN_DISTANCE,
                    this
            );

            systemLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    UPDATE_INTERVAL,
                    MIN_DISTANCE,
                    this
            );

            tryGetLastKnownLocation();

        } catch (SecurityException e) {
            notifyLocationError("Permissão de localização negada");
            stopSelf();
        } catch (Exception e) {
            notifyLocationError("Erro ao iniciar GPS: " + e.getMessage());
            stopSelf();
        }
    }

    // Para as atualizações contínuas de localização
    private void stopLocationUpdates() {
        try {
            systemLocationManager.removeUpdates(this);
        } catch (Exception e) {
        }
    }

    // Tenta obter a última localização conhecida dos provedores
    private void tryGetLastKnownLocation() {
        try {
            Location lastGPS = systemLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNetwork = systemLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location bestLocation = null;

            if (lastGPS != null && lastNetwork != null) {
                bestLocation = lastGPS.getTime() > lastNetwork.getTime() ? lastGPS : lastNetwork;
            } else if (lastGPS != null) {
                bestLocation = lastGPS;
            } else if (lastNetwork != null) {
                bestLocation = lastNetwork;
            }

            if (bestLocation != null) {
                onLocationChanged(bestLocation);
            }

        } catch (SecurityException e) {
        }
    }

    // Verifica se a localização está fora da área permitida
    private boolean isOutsideAllowedArea(Location location) {
        double allowedLat = preferences.getAllowedLatitude();
        double allowedLng = preferences.getAllowedLongitude();
        int allowedRadius = preferences.getAllowedRadius();

        if (allowedLat == 0.0 && allowedLng == 0.0) {
            return false;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                allowedLat,
                allowedLng,
                results
        );

        return results[0] > allowedRadius;
    }

    // Envia broadcast com informações de localização para outros componentes
    private void sendLocationBroadcast(boolean isInsideArea, Location location) {
        Intent broadcast = new Intent("com.example.safemode.LOCATION_CHANGED");
        broadcast.putExtra("is_inside_area", isInsideArea);
        broadcast.putExtra("latitude", location.getLatitude());
        broadcast.putExtra("longitude", location.getLongitude());
        broadcast.putExtra("accuracy", location.getAccuracy());

        sendBroadcast(broadcast);
    }

    // Notifica erro de localização através do listener
    private void notifyLocationError(String error) {
        if (listener != null) {
            listener.onLocationError(error);
        }
    }

    // Verifica se o app tem permissão de localização
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // Verifica se pelo menos um provedor de localização está habilitado
    private boolean isLocationEnabled() {
        return systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                systemLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // Cria o canal de notificações para Android O e superiores
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Monitoramento de Localização",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription("Monitoramento contínuo da localização");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // Cria a notificação inicial do service
    private Notification createNotification() {
        return createNotificationWithStatus(true);
    }

    // Atualiza a notificação com o status atual da localização
    private void updateNotification(boolean isInsideAllowedArea) {
        Notification notification = createNotificationWithStatus(isInsideAllowedArea);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }

    // Cria notificação com status indicando se está dentro ou fora da área permitida
    private Notification createNotificationWithStatus(boolean isInsideAllowedArea) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Monitoramento GPS";
        String text;
        int iconColor;

        if (isInsideAllowedArea) {
            text = "Dentro da área permitida";
            iconColor = 0xFF4CAF50;
        } else {
            text = "Fora da área permitida";
            iconColor = 0xFFFF9800;
        }

        if (currentLocation != null) {
            text += String.format(" (±%.0fm)", currentLocation.getAccuracy());
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_location)
                .setColor(iconColor)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
    }

    // Interface de callback para notificar mudanças de localização
    public interface LocationUpdateListener {
        void onLocationChanged(boolean isInsideAllowedArea);

        void onLocationError(String error);
    }

    // Binder para permitir comunicação com o service
    public class LocationBinder extends android.os.Binder {
    }
}
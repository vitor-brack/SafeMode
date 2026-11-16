package com.example.safemode.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.example.safemode.data.AppPreferences;

/**
 * Classe responsável por gerenciar a localização do dispositivo e verificar se está dentro de uma área permitida.
 * Implementa LocationListener para receber atualizações de localização em tempo real.
 */
public class LocationManager implements LocationListener {
    private static final long LOCATION_MAX_AGE = 300000;
    private static final long UPDATE_INTERVAL = 15000;
    private static final float MIN_DISTANCE = 5;
    private final Context context;
    private final android.location.LocationManager systemLocationManager;
    private final AppPreferences preferences;
    private Location currentLocation;
    private LocationUpdateListener listener;

    // Construtor que inicializa o LocationManager com contexto e preferências
    public LocationManager(Context context) {
        this.context = context;
        this.systemLocationManager = (android.location.LocationManager)
                context.getSystemService(Context.LOCATION_SERVICE);
        this.preferences = new AppPreferences(context);
    }

    // Define o listener para receber atualizações de localização
    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.listener = listener;
    }

    // Obtém a localização uma única vez de todos os provedores disponíveis
    public void getLocationOnce() {

        try {
            if (!hasLocationPermission()) {
                if (listener != null) {
                    listener.onLocationError("Permissão de localização negada");
                }
                return;
            }

            if (!isLocationEnabled()) {
                if (listener != null) {
                    listener.onLocationError("GPS está desligado");
                }
                return;
            }

            requestLocationFromAllProviders();

        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Erro de permissão de localização");
            }
        }
    }

    // Inicia atualizações contínuas de localização dos provedores GPS e Network
    public void startLocationUpdates() {

        try {
            if (!hasLocationPermission()) {
                if (listener != null) {
                    listener.onLocationError("Permissão de localização não concedida");
                }
                return;
            }

            if (!isLocationEnabled()) {
                if (listener != null) {
                    listener.onLocationError("GPS está desligado");
                }
                return;
            }

            Location bestLocation = getBestLastKnownLocation();
            if (bestLocation != null) {
                onLocationChanged(bestLocation);
            }

            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER,
                        UPDATE_INTERVAL,
                        MIN_DISTANCE,
                        this
                );
            }

            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                systemLocationManager.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER,
                        UPDATE_INTERVAL * 2,
                        MIN_DISTANCE * 2,
                        this
                );
            }
        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Erro de permissão: " + e.getMessage());
            }
        } catch (Exception e) {
            if (listener != null) {
                listener.onLocationError("Erro ao iniciar GPS: " + e.getMessage());
            }
        }
    }

    // Para as atualizações contínuas de localização
    public void stopLocationUpdates() {
        try {
            systemLocationManager.removeUpdates(this);
        } catch (Exception e) {
        }
    }

    // Verifica se a localização atual está fora da área permitida
    public boolean isOutsideAllowedArea() {

        if (!preferences.isLocationEnabled()) {
            return false;
        }

        if (currentLocation == null) {
            getLocationOnce();
            return true;
        }

        long locationAge = System.currentTimeMillis() - currentLocation.getTime();
        if (locationAge > LOCATION_MAX_AGE) {
            getLocationOnce();
            return true;
        }

        double allowedLat = preferences.getAllowedLatitude();
        double allowedLng = preferences.getAllowedLongitude();
        int allowedRadius = preferences.getAllowedRadius();

        if (allowedLat == 0.0 && allowedLng == 0.0) {
            return false;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                allowedLat,
                allowedLng,
                results
        );

        float distanceInMeters = results[0];
        boolean isOutside = distanceInMeters > allowedRadius;

        return isOutside;
    }

    // Retorna a localização atual armazenada
    public Location getCurrentLocation() {
        return currentLocation;
    }

    // Callback chamado quando a localização muda
    @Override
    public void onLocationChanged(Location location) {

        if (isBetterLocation(location, currentLocation)) {
            currentLocation = location;

            boolean isOutside = isOutsideAllowedArea();

            if (listener != null) {
                listener.onLocationChanged(!isOutside);
            }
        }
    }

    // Callback chamado quando um provedor de localização é habilitado
    @Override
    public void onProviderEnabled(String provider) {
        requestSingleLocationUpdate();
    }

    // Callback chamado quando um provedor de localização é desabilitado
    @Override
    public void onProviderDisabled(String provider) {
        if (listener != null) {
            listener.onLocationError("Provedor " + provider + " foi desligado");
        }
    }

    // Requisita uma única atualização de localização
    public void requestSingleLocationUpdate() {
        getLocationOnce();
    }

    // Requisita localização de todos os provedores disponíveis (GPS, Network, Passive)
    private void requestLocationFromAllProviders() {

        try {
            Location bestLastKnown = getBestLastKnownLocation();
            if (bestLastKnown != null) {
                long age = System.currentTimeMillis() - bestLastKnown.getTime();
                if (age < LOCATION_MAX_AGE) {
                    onLocationChanged(bestLastKnown);
                    return;
                }
            }
            boolean gpsRequested = false;
            boolean networkRequested = false;

            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                try {
                    systemLocationManager.requestSingleUpdate(
                            android.location.LocationManager.GPS_PROVIDER,
                            new SingleUpdateLocationListener("GPS"),
                            null
                    );
                    gpsRequested = true;
                } catch (Exception e) {
                }
            }

            if (systemLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                try {
                    systemLocationManager.requestSingleUpdate(
                            android.location.LocationManager.NETWORK_PROVIDER,
                            new SingleUpdateLocationListener("Network"),
                            null
                    );
                    networkRequested = true;
                } catch (Exception e) {
                }
            }

            try {
                systemLocationManager.requestSingleUpdate(
                        android.location.LocationManager.PASSIVE_PROVIDER,
                        new SingleUpdateLocationListener("Passive"),
                        null
                );
            } catch (Exception e) {
            }

            if (!gpsRequested && !networkRequested) {
                if (listener != null) {
                    listener.onLocationError("Nenhum provedor de localização disponível");
                }
            }

        } catch (SecurityException e) {
            if (listener != null) {
                listener.onLocationError("Erro de permissão");
            }
        }
    }

    // Verifica se uma localização é melhor que a atual baseada em tempo e precisão
    private boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }

        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 2 * 60 * 1000;
        boolean isSignificantlyOlder = timeDelta < -2 * 60 * 1000;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = location.getProvider() != null &&
                location.getProvider().equals(currentBestLocation.getProvider());

        if (isMoreAccurate) {
            return true;
        } else {
            return !isSignificantlyLessAccurate && !isFromSameProvider;
        }
    }

    // Verifica se o app tem permissão de localização
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    // Verifica se pelo menos um provedor de localização está habilitado
    private boolean isLocationEnabled() {
        return systemLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                systemLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
    }

    // Retorna a melhor última localização conhecida de todos os provedores
    private Location getBestLastKnownLocation() {
        try {
            Location lastGPS = null;
            Location lastNetwork = null;
            Location lastPassive = null;

            try {
                lastGPS = systemLocationManager.getLastKnownLocation(
                        android.location.LocationManager.GPS_PROVIDER);
            } catch (SecurityException e) {
            }

            try {
                lastNetwork = systemLocationManager.getLastKnownLocation(
                        android.location.LocationManager.NETWORK_PROVIDER);
            } catch (SecurityException e) {
            }

            try {
                lastPassive = systemLocationManager.getLastKnownLocation(
                        android.location.LocationManager.PASSIVE_PROVIDER);
            } catch (SecurityException e) {
            }

            Location best = null;

            if (lastGPS != null) {
                best = lastGPS;
            }

            if (lastNetwork != null && isBetterLocation(lastNetwork, best)) {
                best = lastNetwork;
            }

            if (lastPassive != null && isBetterLocation(lastPassive, best)) {
                best = lastPassive;
            }

            return best;

        } catch (SecurityException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Interface de callback para notificar mudanças de localização
    public interface LocationUpdateListener {
        void onLocationChanged(boolean isInsideAllowedArea);

        void onLocationError(String error);
    }

    // LocationListener interno para receber uma única atualização de localização
    private class SingleUpdateLocationListener implements android.location.LocationListener {
        private final String providerName;

        public SingleUpdateLocationListener(String providerName) {
            this.providerName = providerName;
        }

        // Recebe atualização de localização e verifica se é melhor que a atual
        @Override
        public void onLocationChanged(Location location) {

            if (isBetterLocation(location, currentLocation)) {
                LocationManager.this.onLocationChanged(location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}
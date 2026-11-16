package com.example.safemode.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Activity para configurar a área geográfica permitida usando Google Maps.
 * Permite ao usuário selecionar uma localização no mapa e definir um raio de área segura.
 */
public class LocationSetupActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PERMISSION_REQUEST_LOCATION = 1001;
    private MapFriendlyScrollView scrollView;
    private LinearLayout mapLoadingLayout;
    private TextView textSelectedLocation;
    private TextView textRadiusValue;
    private SeekBar seekBarRadius;
    private Button btnMyLocation;
    private Button btnSaveLocation;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private SupportMapFragment mapFragment;
    private Marker selectedLocationMarker;
    private Circle safeAreaCircle;
    private LatLng selectedLatLng;
    private int currentRadius = 100;
    private AppPreferences preferences;

    // Callback chamado quando o Google Maps está pronto para uso
    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        try {
            googleMap = map;

            setupMapSettings();

            if (mapLoadingLayout != null) {
                mapLoadingLayout.setVisibility(View.GONE);
            }

            googleMap.setOnMapClickListener(this::onMapClick);

            setupMapFriendlyScrollView();

            goToCurrentLocation();

            loadSavedArea();

        } catch (Exception e) {
        }
    }

    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showMessage("Permissão de localização concedida!");

                try {
                    if (googleMap != null) {
                        googleMap.setMyLocationEnabled(true);
                    }
                } catch (SecurityException e) {
                }
            } else {
                showMessage("Permissão de localização negada!");
            }
        }
    }

    // Inicializa a activity, views, mapa e carrega configurações salvas
    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_location_setup);

        setupSystemBars();
        initializeViews();
        initializeGoogleMaps();
        loadSavedSettings();
        setupListeners();
    }

    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    // Limpa recursos quando a activity é destruída
    protected void onDestroy() {
        try {
            if (selectedLocationMarker != null) {
                selectedLocationMarker.remove();
                selectedLocationMarker = null;
            }
            if (safeAreaCircle != null) {
                safeAreaCircle.remove();
                safeAreaCircle = null;
            }

            googleMap = null;
            selectedLatLng = null;

        } catch (Exception e) {
        }

        super.onDestroy();
    }

    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    // Restaura marcadores e círculos ao retomar a activity
    protected void onResume() {
        super.onResume();

        try {
            if (googleMap != null && selectedLatLng != null) {
                if (selectedLocationMarker == null) {
                    updateLocationMarker(selectedLatLng);
                }
                if (safeAreaCircle == null) {
                    updateSafeAreaCircle();
                }
            }

        } catch (Exception e) {
        }
    }

    // Callback chamado quando permissões são concedidas ou negadas
    @Override
    protected void onPause() {
        super.onPause();
    }

    // Configura o ScrollView para funcionar bem com o mapa
    private void setupMapFriendlyScrollView() {
        try {

            if (scrollView != null && mapFragment != null) {
                View mapContainer = mapFragment.getView();

                if (mapContainer != null) {
                    scrollView.setMapContainer(mapContainer);
                } else {
                    new android.os.Handler().postDelayed(() -> {
                        View delayedMapContainer = mapFragment.getView();
                        if (delayedMapContainer != null) {
                            scrollView.setMapContainer(delayedMapContainer);
                        }
                    }, 1000);
                }
            }

        } catch (Exception e) {
        }
    }

    // Configura as barras do sistema para tela cheia
    private void setupSystemBars() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                window.setFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                );
            }
        } catch (Exception e) {
        }
    }

    // Inicializa todas as views e configura valores iniciais
    private void initializeViews() {
        try {
            scrollView = findViewById(R.id.scroll_view);
            mapLoadingLayout = findViewById(R.id.map_loading);
            textSelectedLocation = findViewById(R.id.text_selected_location);
            textRadiusValue = findViewById(R.id.text_radius_value);
            seekBarRadius = findViewById(R.id.seekbar_radius);
            btnMyLocation = findViewById(R.id.btn_my_location);
            btnSaveLocation = findViewById(R.id.btn_save_location);
            preferences = new AppPreferences(this);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            seekBarRadius.setMax(2000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                seekBarRadius.setMin(50);
            }
            seekBarRadius.setProgress(currentRadius);
            updateRadiusText(currentRadius);

            setupMapFriendlyScrollView();

        } catch (Exception e) {
        }
    }

    // Inicializa o Google Maps e registra callback para quando o mapa estiver pronto
    private void initializeGoogleMaps() {

        try {
            mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_fragment);

            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }

        } catch (Exception e) {
        }
    }

    // Configura as opções do mapa (tipo, controles, localização)
    private void setupMapSettings() {
        try {
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setOnMarkerClickListener(marker -> {
                return true;
            });
            if (hasLocationPermission()) {
                googleMap.setMyLocationEnabled(true);
            }

        } catch (SecurityException e) {
        } catch (Exception e) {
        }
    }

    // Callback chamado quando o usuário clica no mapa
    private void onMapClick(LatLng latLng) {
        try {
            selectedLatLng = latLng;

            updateLocationMarker(latLng);

            updateSafeAreaCircle();

            updateLocationInfo();

            enableActionButtons(true);

        } catch (Exception e) {
        }
    }

    // Atualiza o marcador da localização selecionada
    private void updateLocationMarker(LatLng latLng) {
        try {
            if (selectedLocationMarker != null) {
                selectedLocationMarker.remove();
            }

            selectedLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng));

        } catch (Exception e) {
        }
    }

    // Atualiza o círculo que representa a área segura
    private void updateSafeAreaCircle() {
        try {
            if (selectedLatLng == null) {
                return;
            }

            if (safeAreaCircle != null) {
                safeAreaCircle.remove();
            }

            CircleOptions circleOptions = new CircleOptions()
                    .center(selectedLatLng)
                    .radius(currentRadius)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(3)
                    .fillColor(Color.argb(50, 0, 100, 255));

            safeAreaCircle = googleMap.addCircle(circleOptions);

        } catch (Exception e) {
        }
    }

    // Atualiza as informações de texto da localização selecionada
    private void updateLocationInfo() {
        try {
            if (selectedLatLng != null) {
                String locationText = String.format(
                        "Localização selecionada\nLat: %.6f\nLng: %.6f\nRaio: %d metros",
                        selectedLatLng.latitude,
                        selectedLatLng.longitude,
                        currentRadius
                );

                textSelectedLocation.setText(locationText);
            }

        } catch (Exception e) {
        }
    }

    // Habilita ou desabilita os botões de ação
    private void enableActionButtons(boolean enabled) {
        try {
            btnSaveLocation.setEnabled(enabled);

            float alpha = enabled ? 1.0f : 0.5f;
            btnSaveLocation.setAlpha(alpha);

            if (enabled) {
                btnSaveLocation.setText("Salvar localização");
            } else {
                btnSaveLocation.setText("Selecione uma localização primeiro");
            }

        } catch (Exception e) {
        }
    }

    // Carrega as configurações salvas anteriormente
    private void loadSavedSettings() {
        try {
            int savedRadius = preferences.getAllowedRadius();
            if (savedRadius > 0) {
                currentRadius = savedRadius;
                seekBarRadius.setProgress(currentRadius);
                updateRadiusText(currentRadius);
            }

        } catch (Exception e) {
        }
    }

    // Carrega a área salva anteriormente e exibe no mapa
    private void loadSavedArea() {
        try {
            double savedLat = preferences.getAllowedLatitude();
            double savedLng = preferences.getAllowedLongitude();

            if (savedLat != 0.0 && savedLng != 0.0) {
                LatLng savedLocation = new LatLng(savedLat, savedLng);
                selectedLatLng = savedLocation;

                updateLocationMarker(savedLocation);
                updateSafeAreaCircle();
                updateLocationInfo();
                enableActionButtons(true);

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(savedLocation, 15));
            }

        } catch (Exception e) {
        }
    }

    // Configura os listeners dos botões e controles
    private void setupListeners() {
        try {
            btnMyLocation.setOnClickListener(v -> goToCurrentLocation());

            btnSaveLocation.setOnClickListener(v -> saveLocationSettings());

            seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        if (progress < 50) {
                            progress = 50;
                        }

                        currentRadius = progress;
                        updateRadiusText(progress);

                        if (selectedLatLng != null) {
                            updateSafeAreaCircle();
                            updateLocationInfo();
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

        } catch (Exception e) {
        }
    }

    // Move o mapa para a localização atual do usuário
    private void goToCurrentLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        try {
            btnMyLocation.setEnabled(false);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
                        }

                        btnMyLocation.setEnabled(true);
                        btnMyLocation.setText("Minha localização");
                    })
                    .addOnFailureListener(e -> {
                        btnMyLocation.setEnabled(true);
                        btnMyLocation.setText("Minha localização");
                    });

        } catch (SecurityException e) {
            requestLocationPermission();

            btnMyLocation.setEnabled(true);
            btnMyLocation.setText("Minha localização");
        } catch (Exception e) {
            btnMyLocation.setEnabled(true);
            btnMyLocation.setText("Minha localização");
        }
    }

    // Salva as configurações de localização e raio nas preferências
    private void saveLocationSettings() {
        try {
            if (selectedLatLng == null) {
                showMessage("Selecione uma localização no mapa primeiro");
                return;
            }

            preferences.setAllowedLocation(
                    selectedLatLng.latitude,
                    selectedLatLng.longitude,
                    currentRadius
            );

            double savedLat = preferences.getAllowedLatitude();
            double savedLng = preferences.getAllowedLongitude();
            int savedRadius = preferences.getAllowedRadius();

            boolean saveSuccess = Math.abs(savedLat - selectedLatLng.latitude) < 0.000001 &&
                    Math.abs(savedLng - selectedLatLng.longitude) < 0.000001 &&
                    savedRadius == currentRadius;

            if (saveSuccess) {
                showMessage("Localização salva com sucesso!");
            } else {
                showMessage("Erro ao salvar localização");
            }

        } catch (Exception e) {
            showMessage("Erro ao salvar: " + e.getMessage());
        }
    }

    // Atualiza o texto que exibe o valor do raio
    private void updateRadiusText(int radiusInMeters) {
        try {
            String radiusText;
            if (radiusInMeters >= 1000) {
                radiusText = String.format("%.1f km", radiusInMeters / 1000.0);
            } else {
                radiusText = radiusInMeters + " metros";
            }

            textRadiusValue.setText(radiusText);

        } catch (Exception e) {
        }
    }

    // Verifica se o app tem permissão de localização
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Solicita permissão de localização ao usuário
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_LOCATION);
    }

    // Exibe uma mensagem Toast para o usuário
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // Verifica se a configuração atual é válida
    private boolean isConfigurationValid() {
        try {
            boolean hasLocation = selectedLatLng != null;
            boolean hasValidRadius = currentRadius >= 50 && currentRadius <= 2000;

            return hasLocation && hasValidRadius;

        } catch (Exception e) {
            return false;
        }
    }

    // Centraliza o mapa na área selecionada
    private void centerMapOnSelectedArea() {
        try {
            if (googleMap != null && selectedLatLng != null) {
                float zoom = calculateZoomLevel(currentRadius);

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng, zoom));
            }

        } catch (Exception e) {
        }
    }

    // Calcula o nível de zoom apropriado baseado no raio
    private float calculateZoomLevel(int radiusInMeters) {
        try {

            if (radiusInMeters <= 100) {
                return 17.0f;
            } else if (radiusInMeters <= 300) {
                return 16.0f;
            } else if (radiusInMeters <= 500) {
                return 15.0f;
            } else if (radiusInMeters <= 1000) {
                return 14.0f;
            } else if (radiusInMeters <= 2000) {
                return 13.0f;
            } else {
                return 12.0f;
            }

        } catch (Exception e) {
            return 15.0f;
        }
    }

    // Exibe um resumo da configuração atual em um diálogo
    private void showConfigurationSummary() {
        try {
            if (!isConfigurationValid()) {
                showMessage("Configuração incompleta");
                return;
            }

            String summary = String.format(
                    "📍 RESUMO DA CONFIGURAÇÃO\n\n" +
                            "Centro da área segura:\n" +
                            "Latitude: %.6f\n" +
                            "Longitude: %.6f\n\n" +
                            "Raio da área: %s\n\n" +
                            "Área total: %.2f km²",
                    selectedLatLng.latitude,
                    selectedLatLng.longitude,
                    currentRadius >= 1000 ?
                            String.format("%.1f km", currentRadius / 1000.0) :
                            currentRadius + " metros",
                    Math.PI * Math.pow(currentRadius / 1000.0, 2)
            );

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Configuração da Área Segura")
                    .setMessage(summary)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Centralizar no Mapa", (dialog, which) -> centerMapOnSelectedArea())
                    .show();

        } catch (Exception e) {
        }
    }

    // Valida a configuração antes de salvar
    private boolean validateBeforeSave() {
        try {
            if (selectedLatLng == null) {
                showMessage("Selecione uma localização no mapa primeiro");
                return false;
            }

            if (selectedLatLng.latitude < -90 || selectedLatLng.latitude > 90) {
                showMessage("Latitude inválida");
                return false;
            }

            if (selectedLatLng.longitude < -180 || selectedLatLng.longitude > 180) {
                showMessage("Longitude inválida");
                return false;
            }

            if (currentRadius < 50 || currentRadius > 2000) {
                showMessage("Raio deve estar entre 50m e 2km");
                return false;
            }

            return true;

        } catch (Exception e) {
            showMessage("Erro na validação: " + e.getMessage());
            return false;
        }
    }

}
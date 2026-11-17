package com.example.safemode.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.security.LockScreenRules;
import com.example.safemode.security.PinManager;
import com.example.safemode.service.LockScreenService;
import com.example.safemode.service.SafeModeService;
import com.example.safemode.util.PermissionChecker;

import java.util.HashSet;

/**
 * Activity principal do aplicativo SafeMode.
 * Gerencia as funcionalidades principais como ativação do modo seguro, controle de localização,
 * tela de bloqueio, configuração de PINs e seleção de aplicativos ocultos.
 */
public class MainActivity extends AppCompatActivity {

    private Switch switchSafeMode;
    private Switch switchLocationControl;
    private Switch switchLockScreen;
    private Button btnSelectApps;
    private Button btnSetLocation;
    private Button btnSettings;
    private Button btnSavePin;
    private Button btnSaveSecondaryPin;
    private Button btnSelectHiddenApps;
    private android.widget.EditText etPin;
    private android.widget.EditText etSecondaryPin;
    private AppPreferences preferences;
    private PinManager pinManager;
    private PinSetupController pinSetup;

    // Inicializa a activity, configura views e listeners
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        preferences = new AppPreferences(this);
        boolean initialSafeModeState = preferences.isSafeModeEnabled();

        setupSystemBars();
        setContentView(R.layout.activity_main);

        switchSafeMode = findViewById(R.id.switch_safe_mode);

        if (switchSafeMode != null) {
            boolean switchStateFromXML = switchSafeMode.isChecked();

            if (switchStateFromXML && !initialSafeModeState) {
                switchSafeMode.setChecked(false);
            }
        }

        initializeViews();
        setupClickListeners();
    }

    // Atualiza o estado dos switches ao retomar a activity
    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (isFirstRun()) {
                preferences.setSafeModeEnabled(false);
                preferences.setLocationEnabled(false);
                preferences.setBlockedApps(new HashSet<>());
                preferences.setHiddenApps(new HashSet<>());
            }

            bindSwitch(switchSafeMode, preferences.isSafeModeEnabled(), safeModeListener());
            bindSwitch(switchLocationControl, preferences.isLocationEnabled(), locationListener());
            bindSwitch(switchLockScreen, preferences.isLockScreenEnabled(), lockScreenListener());

        } catch (Exception e) {

            try {
                if (switchSafeMode != null) {
                    switchSafeMode.setOnCheckedChangeListener(null);
                    switchSafeMode.setChecked(false);
                }

                if (switchLocationControl != null) {
                    switchLocationControl.setOnCheckedChangeListener(null);
                    switchLocationControl.setChecked(false);
                }

                preferences.setSafeModeEnabled(false);
                preferences.setLocationEnabled(false);

            } catch (Exception secondaryError) {
            }
        }
    }

    // Liga um switch ao estado salvo sem disparar o listener, e só depois instala o listener
    private void bindSwitch(Switch toggle, boolean savedState, CompoundButton.OnCheckedChangeListener listener) {
        if (toggle == null) {
            return;
        }
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(savedState);
        toggle.setOnCheckedChangeListener(listener);
    }

    // Listener do switch do Safe Mode: ligar confere as permissões, desligar para o serviço
    private CompoundButton.OnCheckedChangeListener safeModeListener() {
        return (buttonView, isChecked) -> {
            if (isChecked) {
                checkPermissionsAndEnableSafeMode();
            } else {
                disableSafeMode();
            }
        };
    }

    // Listener do switch de localização. Sem a permissão, mostra o diálogo e volta o switch para
    // desligado; esse setChecked(false) dispara o próprio listener, que grava false
    private CompoundButton.OnCheckedChangeListener locationListener() {
        return (buttonView, isChecked) -> {
            if (isChecked) {
                if (!PermissionChecker.hasLocation(this)) {
                    showLocationPermissionDialog();
                    switchLocationControl.setChecked(false);
                    return;
                }
            }
            preferences.setLocationEnabled(isChecked);
        };
    }

    // Listener do switch da tela de bloqueio
    private CompoundButton.OnCheckedChangeListener lockScreenListener() {
        return (buttonView, isChecked) -> {
            if (isChecked) {
                enableLockScreen();
            } else {
                disableLockScreen();
            }
        };
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

    // Inicializa todas as views, preferências e gerenciadores
    private void initializeViews() {

        try {
            switchSafeMode = findViewById(R.id.switch_safe_mode);
            switchLocationControl = findViewById(R.id.switch_location_control);
            switchLockScreen = findViewById(R.id.switch_lock_screen);
            btnSelectApps = findViewById(R.id.btn_select_apps);
            btnSetLocation = findViewById(R.id.btn_set_location);
            btnSettings = findViewById(R.id.btn_settings);
            btnSavePin = findViewById(R.id.btn_save_pin);
            btnSaveSecondaryPin = findViewById(R.id.btn_save_secondary_pin);
            btnSelectHiddenApps = findViewById(R.id.btn_select_hidden_apps);
            etPin = findViewById(R.id.et_pin);
            etSecondaryPin = findViewById(R.id.et_secondary_pin);

            preferences = new AppPreferences(this);
            pinManager = new PinManager(this);
            pinSetup = new PinSetupController(this, pinManager);

        } catch (Exception e) {
        }
    }

    // Configura os listeners dos switches e botões da interface
    private void setupClickListeners() {

        try {
            if (switchSafeMode != null) {
                switchSafeMode.setOnCheckedChangeListener(safeModeListener());
            }

            if (switchLocationControl != null) {
                switchLocationControl.setOnCheckedChangeListener(locationListener());
            }

            if (btnSelectApps != null) {
                btnSelectApps.setOnClickListener(v -> {
                    Intent intent = new Intent(this, AppSelectionActivity.class);
                    startActivity(intent);
                });
            }

            if (btnSetLocation != null) {
                btnSetLocation.setOnClickListener(v -> {
                    Intent intent = new Intent(this, LocationSetupActivity.class);
                    startActivity(intent);
                });
            }

            if (btnSettings != null) {
                btnSettings.setOnClickListener(v -> {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                });
            }

            if (switchLockScreen != null) {
                switchLockScreen.setOnCheckedChangeListener(lockScreenListener());
            }

            if (btnSavePin != null) {
                btnSavePin.setOnClickListener(v -> {
                    savePinConfiguration();
                });
            }

            if (btnSaveSecondaryPin != null) {
                btnSaveSecondaryPin.setOnClickListener(v -> {
                    saveSecondaryPinConfiguration();
                });
            }

            if (btnSelectHiddenApps != null) {
                btnSelectHiddenApps.setOnClickListener(v -> {
                    openHiddenAppsSelection();
                });
            }

        } catch (Exception e) {
        }
    }

    // Verifica todas as permissões necessárias antes de ativar o modo seguro
    private void checkPermissionsAndEnableSafeMode() {

        try {
            boolean hasLocation = PermissionChecker.hasLocation(this);
            boolean hasOverlay = PermissionChecker.hasOverlay(this);
            boolean hasUsageStats = PermissionChecker.hasUsageStats(this);
            boolean hasAccessibility = PermissionChecker.hasAccessibility(this);

            if (hasLocation && hasOverlay && hasUsageStats && hasAccessibility) {
                enableSafeMode();
                return;
            }

            showPermissionsDialog(hasLocation, hasOverlay, hasUsageStats, hasAccessibility);

        } catch (Exception e) {

            if (switchSafeMode != null) {
                switchSafeMode.setChecked(false);
            }
        }
    }

    // Exibe diálogo com as permissões faltantes que precisam ser concedidas
    private void showPermissionsDialog(boolean hasLocation, boolean hasOverlay, boolean hasUsageStats, boolean hasAccessibility) {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_permissions, null);

            androidx.cardview.widget.CardView locationCard = dialogView.findViewById(R.id.locationPermissionCard);
            androidx.cardview.widget.CardView overlayCard = dialogView.findViewById(R.id.overlayPermissionCard);
            androidx.cardview.widget.CardView usageStatsCard = dialogView.findViewById(R.id.usageStatsPermissionCard);
            androidx.cardview.widget.CardView accessibilityCard = dialogView.findViewById(R.id.accessibilityPermissionCard);

            locationCard.setVisibility(!hasLocation ? View.VISIBLE : View.GONE);
            overlayCard.setVisibility(!hasOverlay ? View.VISIBLE : View.GONE);
            usageStatsCard.setVisibility(!hasUsageStats ? View.VISIBLE : View.GONE);
            accessibilityCard.setVisibility(!hasAccessibility ? View.VISIBLE : View.GONE);

            GoToSettingsDialog.show(this, dialogView, () -> {
                if (switchSafeMode != null) {
                    switchSafeMode.setChecked(false);
                }
            });

        } catch (Exception e) {
            if (switchSafeMode != null) {
                switchSafeMode.setChecked(false);
            }
        }
    }

    // Exibe diálogo solicitando permissão de localização
    private void showLocationPermissionDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_location_permission, null);

            GoToSettingsDialog.show(this, dialogView, null);

        } catch (Exception e) {
        }
    }

    // Ativa o modo seguro e inicia o serviço de monitoramento
    private void enableSafeMode() {
        try {
            preferences.setSafeModeEnabled(true);

            Intent serviceIntent = new Intent(this, SafeModeService.class);
            startService(serviceIntent);

        } catch (Exception e) {

            if (switchSafeMode != null) {
                switchSafeMode.setChecked(false);
            }
        }
    }

    // Desativa o modo seguro e para o serviço de monitoramento
    private void disableSafeMode() {
        try {
            preferences.setSafeModeEnabled(false);

            Intent serviceIntent = new Intent(this, SafeModeService.class);
            stopService(serviceIntent);

        } catch (Exception e) {
        }
    }

    // Ativa a tela de bloqueio após verificar requisitos de launcher e PINs
    private void enableLockScreen() {
        try {
            LockScreenRules.Requirement requirement = LockScreenRules.check(
                    PermissionChecker.isDefaultLauncher(this),
                    pinManager.hasPin(), pinManager.hasSecondaryPin());

            if (requirement == LockScreenRules.Requirement.NOT_DEFAULT_LAUNCHER) {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_launcher_required, null);

                GoToSettingsDialog.show(this, dialogView, () -> {
                    if (switchLockScreen != null) {
                        switchLockScreen.setChecked(false);
                    }
                });

                if (switchLockScreen != null) {
                    switchLockScreen.setChecked(false);
                }

                return;
            }

            if (requirement == LockScreenRules.Requirement.PRIMARY_PIN_MISSING) {
                Toast.makeText(this, "Configure um PIN primeiro!", Toast.LENGTH_SHORT).show();
                if (switchLockScreen != null) {
                    switchLockScreen.setChecked(false);
                }
                return;
            }

            if (requirement == LockScreenRules.Requirement.SECONDARY_PIN_MISSING) {
                Toast.makeText(this, "Configure o PIN secundário primeiro!", Toast.LENGTH_SHORT).show();
                if (switchLockScreen != null) {
                    switchLockScreen.setChecked(false);
                }
                return;
            }

            preferences.setLockScreenEnabled(true);

            Intent serviceIntent = new Intent(this, LockScreenService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            Toast.makeText(this, "Bloqueio de tela ativado!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao ativar bloqueio", Toast.LENGTH_SHORT).show();
            if (switchLockScreen != null) {
                switchLockScreen.setChecked(false);
            }
        }
    }

    // Desativa a tela de bloqueio e para o serviço correspondente
    private void disableLockScreen() {
        try {
            preferences.setLockScreenEnabled(false);

            Intent serviceIntent = new Intent(this, LockScreenService.class);
            stopService(serviceIntent);

            Toast.makeText(this, "Bloqueio de tela desativado!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao desativar bloqueio", Toast.LENGTH_SHORT).show();
        }
    }

    // Salva a configuração do PIN principal e limpa o campo se gravou
    private void savePinConfiguration() {
        try {
            if (pinSetup.savePrimary(etPin.getText().toString())) {
                etPin.setText("");
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao configurar PIN", Toast.LENGTH_SHORT).show();
        }
    }

    // Salva a configuração do PIN secundário e limpa o campo se gravou
    private void saveSecondaryPinConfiguration() {
        try {
            if (pinSetup.saveSecondary(etSecondaryPin.getText().toString())) {
                etSecondaryPin.setText("");
            }

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao configurar PIN secundário", Toast.LENGTH_SHORT).show();
        }
    }

    // Abre a tela de seleção de aplicativos ocultos após verificar PIN secundário
    private void openHiddenAppsSelection() {
        try {
            if (!pinManager.hasSecondaryPin()) {
                Toast.makeText(this, "Configure o PIN secundário primeiro!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, HiddenAppsSelectionActivity.class);
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir seleção de apps", Toast.LENGTH_SHORT).show();
        }
    }

    // Verifica se é a primeira execução do app após instalação
    private boolean isFirstRun() {
        try {
            android.content.SharedPreferences prefs = getSharedPreferences("app_state", MODE_PRIVATE);

            long installTime = getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime;
            long lastKnownInstallTime = prefs.getLong("last_install_time", 0);

            boolean isFirst = (lastKnownInstallTime != installTime);

            if (isFirst) {
                prefs.edit()
                        .putBoolean("is_first_run", false)
                        .putLong("last_install_time", installTime)
                        .apply();
            }

            return isFirst;

        } catch (Exception e) {
            return false;
        }
    }
}
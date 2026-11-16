package com.example.safemode.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.data.BlockLogger;
import com.example.safemode.service.SafeModeAccessibilityService;
import com.example.safemode.util.AccessibilityUtils;
import com.example.safemode.util.UsageStatsUtils;

/**
 * Activity de configurações do aplicativo SafeMode.
 * Gerencia permissões necessárias, exibe status de configurações, estatísticas de bloqueio
 * e permite ao usuário visualizar histórico de bloqueios e configurar o launcher padrão.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCATION = 1001;
    private static final int PERMISSION_REQUEST_OVERLAY = 1002;
    private static final int PERMISSION_REQUEST_ACCESSIBILITY = 1003;
    private static final int PERMISSION_REQUEST_USAGE_STATS = 1004;
    private TextView textBlockedAppsCount;
    private TextView textTodayStats;
    private Button btnViewLogs;
    private Button btnPermissions;
    private Button btnSetLauncher;
    private ImageView iconLocationPermission;
    private ImageView iconOverlayPermission;
    private ImageView iconAccessibilityPermission;
    private ImageView iconUsagePermission;
    private ImageView iconLauncherStatus;
    private TextView textLocationStatus;
    private TextView textOverlayStatus;
    private TextView textAccessibilityStatus;
    private TextView textUsageStatus;
    private TextView textLauncherStatus;
    private AppPreferences preferences;
    private BlockLogger blockLogger;

    // Processa o resultado da solicitação de permissões
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        try {
            if (requestCode == PERMISSION_REQUEST_LOCATION) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }

            checkAllPermissions();

        } catch (Exception e) {
        }
    }

    // Inicializa a activity e configura views e permissões
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_settings);

        setupSystemBars();
        initializeViews();
        loadCurrentSettings();
        setupListeners();

        checkAllPermissions();
    }

    // Verifica permissões após retornar de outras activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            new android.os.Handler().postDelayed(() -> {
                checkAllPermissions();
            }, 1000);

        } catch (Exception e) {
        }
    }

    // Recarrega configurações e verifica permissões ao retomar a activity
    @Override
    protected void onResume() {
        super.onResume();

        try {
            loadCurrentSettings();
            checkAllPermissions();
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

    // Inicializa todas as views do layout
    private void initializeViews() {

        try {
            textBlockedAppsCount = findViewById(R.id.text_blocked_apps_count);
            textTodayStats = findViewById(R.id.text_today_stats);
            btnViewLogs = findViewById(R.id.btn_view_logs);
            btnPermissions = findViewById(R.id.btn_permissions);
            btnSetLauncher = findViewById(R.id.btn_set_launcher);

            iconLocationPermission = findViewById(R.id.icon_location_permission);
            iconOverlayPermission = findViewById(R.id.icon_overlay_permission);
            iconAccessibilityPermission = findViewById(R.id.icon_accessibility_permission);
            iconUsagePermission = findViewById(R.id.icon_usage_permission);
            iconLauncherStatus = findViewById(R.id.icon_launcher_status);

            textLocationStatus = findViewById(R.id.text_location_status);
            textOverlayStatus = findViewById(R.id.text_overlay_status);
            textAccessibilityStatus = findViewById(R.id.text_accessibility_status);
            textUsageStatus = findViewById(R.id.text_usage_status);
            textLauncherStatus = findViewById(R.id.text_launcher_status);

            preferences = new AppPreferences(this);
            blockLogger = new BlockLogger(this);

        } catch (Exception e) {
        }
    }

    // Carrega as configurações atuais e estatísticas
    private void loadCurrentSettings() {

        try {
            int blockedAppsCount = preferences.getBlockedApps().size();
            if (textBlockedAppsCount != null) {
                textBlockedAppsCount.setText(blockedAppsCount + " apps selecionados para bloqueio");
            }
            loadTodayStats();

        } catch (Exception e) {
        }
    }

    // Carrega as estatísticas de bloqueios do dia atual
    private void loadTodayStats() {
        try {
            JSONObject stats = blockLogger.getTodayStats();

            int totalBlocks = stats.optInt("total_blocks_today", 0);
            int appsBlocked = stats.optInt("apps_blocked_today", 0);
            String mostBlocked = stats.optString("most_blocked_app", "Nenhum");

            String statsText = String.format(
                    "Hoje: %d bloqueios em %d apps\nMais bloqueado: %s",
                    totalBlocks, appsBlocked, mostBlocked
            );

            if (textTodayStats != null) {
                textTodayStats.setText(statsText);
            }

        } catch (Exception e) {
            if (textTodayStats != null) {
                textTodayStats.setText("Erro ao carregar estatísticas");
            }
        }
    }

    // Configura os listeners dos botões
    private void setupListeners() {

        try {
            if (btnViewLogs != null) {
                btnViewLogs.setOnClickListener(v -> showBlockLogs());
            }

            if (btnPermissions != null) {
                btnPermissions.setOnClickListener(v -> {
                    requestAllPermissions();
                });
            }

            if (btnSetLauncher != null) {
                btnSetLauncher.setOnClickListener(v -> {
                    openLauncherSettings();
                });
            }

        } catch (Exception e) {
        }
    }

    // Verifica o status de todas as permissões necessárias
    private void checkAllPermissions() {

        try {
            boolean hasLocation = hasLocationPermission();
            boolean hasOverlay = hasOverlayPermission();
            boolean hasAccessibility = hasAccessibilityPermission();
            boolean hasUsageStats = hasUsageStatsPermission();
            boolean isLauncher = isDefaultLauncher();

            updatePermissionStatus(hasLocation, hasOverlay, hasAccessibility, hasUsageStats, isLauncher);

        } catch (Exception e) {
        }
    }

    // Verifica se a permissão de localização foi concedida
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Verifica se a permissão de sobreposição foi concedida
    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    // Verifica se o serviço de acessibilidade está habilitado
    private boolean hasAccessibilityPermission() {
        return AccessibilityUtils.isAccessibilityServiceEnabled(this, SafeModeAccessibilityService.class);
    }

    // Verifica se a permissão de estatísticas de uso foi concedida
    private boolean hasUsageStatsPermission() {
        return UsageStatsUtils.hasUsageStatsPermission(this);
    }

    // Verifica se o aplicativo está definido como launcher padrão do sistema
    private boolean isDefaultLauncher() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);

            android.content.pm.ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String currentLauncher = resolveInfo.activityInfo.packageName;
                return currentLauncher.equals(getPackageName());
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // Solicita todas as permissões necessárias sequencialmente
    private void requestAllPermissions() {

        try {
            if (!hasLocationPermission()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);
                return;
            }

            if (!hasOverlayPermission()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_OVERLAY);
                return;
            }

            if (!hasUsageStatsPermission()) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, PERMISSION_REQUEST_USAGE_STATS);
                return;
            }

            if (!hasAccessibilityPermission()) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivityForResult(intent, PERMISSION_REQUEST_ACCESSIBILITY);
                return;
            }

            showMessage("Todas as permissões já foram concedidas!");

        } catch (Exception e) {
        }
    }

    // Atualiza os ícones e textos de status de todas as permissões
    private void updatePermissionStatus(boolean location, boolean overlay,
                                        boolean accessibility, boolean usageStats, boolean isLauncher) {

        try {
            updatePermissionIcon(iconLocationPermission, location);
            updatePermissionText(textLocationStatus, location ? "Concedida" : "Negada", location);

            updatePermissionIcon(iconOverlayPermission, overlay);
            updatePermissionText(textOverlayStatus, overlay ? "Concedida" : "Negada", overlay);

            updatePermissionIcon(iconAccessibilityPermission, accessibility);
            updatePermissionText(textAccessibilityStatus, accessibility ? "Ativo" : "Inativo", accessibility);

            updatePermissionIcon(iconUsagePermission, usageStats);
            updatePermissionText(textUsageStatus, usageStats ? "Concedida" : "Negada", usageStats);

            updatePermissionIcon(iconLauncherStatus, isLauncher);
            updatePermissionText(textLauncherStatus, isLauncher ? "Definido" : "Não Definido", isLauncher);


        } catch (Exception e) {
        }
    }

    // Atualiza o ícone da permissão com cor verde ou vermelha
    private void updatePermissionIcon(ImageView icon, boolean hasPermission) {
        if (icon != null) {
            try {
                int color = hasPermission ?
                        getResources().getColor(android.R.color.holo_green_dark) :
                        getResources().getColor(android.R.color.holo_red_dark);
                icon.setColorFilter(color);
            } catch (Exception e) {
            }
        }
    }

    // Atualiza o texto de status da permissão com cor verde ou vermelha
    private void updatePermissionText(TextView textView, String text, boolean hasPermission) {
        if (textView != null) {
            try {
                textView.setText(text);
                int color = hasPermission ?
                        getResources().getColor(android.R.color.holo_green_dark) :
                        getResources().getColor(android.R.color.holo_red_dark);
                textView.setTextColor(color);
            } catch (Exception e) {
            }
        }
    }

    // Exibe o histórico de bloqueios dos últimos 7 dias
    private void showBlockLogs() {

        try {
            StringBuilder logsText = new StringBuilder();

            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            var recentLogs = blockLogger.getLogEntriesInPeriod(sevenDaysAgo, System.currentTimeMillis());

            if (recentLogs.isEmpty()) {
                logsText.append("Nenhum bloqueio registrado nos últimos 7 dias.");
            } else {
                logsText.append("Bloqueios dos últimos 7 dias:\n\n");

                int maxEntries = Math.min(recentLogs.size(), 20);
                for (int i = 0; i < maxEntries; i++) {
                    JSONObject log = recentLogs.get(i);

                    String appName = log.optString("app_name", "App desconhecido");
                    String time = log.optString("readable_time", "Horário desconhecido");

                    logsText.append("• ").append(appName).append("\n")
                            .append("  ").append(time).append("\n\n");
                }

                if (recentLogs.size() > 20) {
                    logsText.append("... e mais ").append(recentLogs.size() - 20).append(" registros");
                }
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Histórico de Bloqueios")
                    .setMessage(logsText.toString())
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Limpar Histórico", (dialogInterface, which) -> confirmClearLogs())
                    .setIcon(android.R.drawable.ic_menu_recent_history)
                    .create();

            dialog.show();

            GradientDrawable gradient = new GradientDrawable();
            gradient.setColors(new int[]{
                    getResources().getColor(R.color.primary_dark_blue),
                    getResources().getColor(R.color.blue_medium)
            });
            gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
            gradient.setCornerRadius(15f);
            dialog.getWindow().setBackgroundDrawable(gradient);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(getResources().getColor(android.R.color.white));

        } catch (Exception e) {
            AlertDialog errorDialog = new AlertDialog.Builder(this)
                    .setTitle("Erro")
                    .setMessage("Erro ao carregar histórico: " + e.getMessage())
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .create();

            errorDialog.show();
            errorDialog.getWindow().setBackgroundDrawableResource(android.R.color.holo_red_dark);
            errorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    // Solicita confirmação antes de limpar o histórico de bloqueios
    private void confirmClearLogs() {

        try {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Limpar Histórico")
                    .setMessage("Apagar todo o histórico de bloqueios?\n\nEsta ação não pode ser desfeita.")
                    .setPositiveButton("Limpar", (dialogInterface, which) -> {
                        try {
                            blockLogger.clearAllLogs();
                            showMessage("Histórico limpo com sucesso");
                            loadTodayStats(); // Atualizar estatísticas
                        } catch (Exception e) {
                            showMessage("Erro ao limpar histórico");
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .create();
            dialog.show();
            GradientDrawable gradient = new GradientDrawable();
            gradient.setColors(new int[]{
                    getResources().getColor(R.color.primary_dark_blue),
                    getResources().getColor(R.color.blue_medium)
            });

            gradient.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);

            gradient.setCornerRadius(20f);

            dialog.getWindow().setBackgroundDrawable(gradient);

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.white));

        } catch (Exception e) {
            showMessage("Erro ao mostrar confirmação");
        }
    }

    // Exibe uma mensagem toast
    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Abre as configurações de launcher padrão do sistema
    private void openLauncherSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
            startActivity(intent);

        } catch (Exception e) {
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                startActivity(fallbackIntent);

            } catch (Exception e2) {
                showMessage("Erro ao abrir configurações de launcher");
            }
        }
    }
}
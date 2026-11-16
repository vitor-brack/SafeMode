package com.example.safemode.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safemode.R;
import com.example.safemode.data.AppInfo;
import com.example.safemode.data.AppPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Activity responsável por permitir ao usuário selecionar quais aplicativos devem ser ocultados.
 * Exibe uma lista de apps instalados e permite marcar/desmarcar para ocultar.
 */
public class HiddenAppsSelectionActivity extends AppCompatActivity implements AppListAdapter.OnAppToggleListener {

    private RecyclerView recyclerView;
    private AppListAdapter adapter;
    private LinearLayout containerApps;
    private LinearLayout loadingLayout;
    private LinearLayout layoutEmpty;
    private Button btnRetry;
    private AppPreferences preferences;
    private List<AppInfo> appList;

    // Callback chamado quando um app é marcado/desmarcado (não implementado)
    @Override
    public void onAppToggled(AppInfo appInfo, boolean isBlocked) {
    }

    // Inicializa a activity, configura views e carrega lista de aplicativos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setupSystemBars();
        setContentView(R.layout.activity_hidden_apps_selection);

        preferences = new AppPreferences(this);
        appList = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        loadInstalledApps();
    }

    // Salva os aplicativos ocultos quando a activity é pausada
    @Override
    protected void onPause() {
        super.onPause();
        saveHiddenApps();
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

    // Inicializa as views da interface
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view_apps);
        containerApps = findViewById(R.id.container_apps);
        loadingLayout = findViewById(R.id.loading_layout);
        layoutEmpty = findViewById(R.id.layout_empty);
        btnRetry = findViewById(R.id.btn_retry);

        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadInstalledApps());
        }
    }

    // Configura o RecyclerView com adapter e layout manager
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(appList, this);
        recyclerView.setAdapter(adapter);
    }

    // Carrega os aplicativos instalados em uma thread separada
    private void loadInstalledApps() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        if (containerApps != null) {
            containerApps.setVisibility(View.GONE);
        }
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.GONE);
        }

        new Thread(() -> {
            List<AppInfo> apps = getInstalledApps();

            runOnUiThread(() -> {
                appList.clear();
                appList.addAll(apps);
                adapter.notifyDataSetChanged();

                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.GONE);
                }

                if (apps.isEmpty()) {
                    if (layoutEmpty != null) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (containerApps != null) {
                        containerApps.setVisibility(View.VISIBLE);
                    }
                }
            });
        }).start();
    }

    // Retorna a lista de aplicativos instalados com launcher intent
    private List<AppInfo> getInstalledApps() {
        List<AppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();

        android.content.Intent mainIntent = new android.content.Intent(android.content.Intent.ACTION_MAIN, null);
        mainIntent.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> launchableApps = pm.queryIntentActivities(mainIntent, 0);

        Set<String> hiddenApps = preferences.getHiddenApps();

        for (android.content.pm.ResolveInfo resolveInfo : launchableApps) {
            String packageName = resolveInfo.activityInfo.packageName;

            if (packageName.equals(getPackageName())) {
                continue;
            }

            if (isCriticalSystemApp(packageName)) {
                continue;
            }

            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);

                String appName = appInfo.loadLabel(pm).toString();
                Drawable icon = appInfo.loadIcon(pm);

                boolean isHidden = hiddenApps.contains(packageName);

                AppInfo info = new AppInfo(packageName, appName, icon);
                info.isBlocked = isHidden;
                apps.add(info);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        Collections.sort(apps, (a, b) -> a.appName.compareToIgnoreCase(b.appName));

        return apps;
    }

    // Verifica se um aplicativo é crítico para o sistema e não deve ser ocultado
    private boolean isCriticalSystemApp(String packageName) {
        String[] criticalSystemApps = {
                "com.android.systemui",
                "android",
                "com.android.phone",
                "com.android.settings",
                "com.android.launcher",
                "com.android.dialer",
                "com.google.android.gms",
                "com.android.packageinstaller",
                "com.android.launcher3",
                "com.sec.android.app.launcher",
                "com.android.emergency",
                "com.android.incallui"
        };

        for (String criticalApp : criticalSystemApps) {
            if (packageName.equals(criticalApp) || packageName.startsWith(criticalApp + ".")) {
                return true;
            }
        }

        return false;
    }

    // Salva a lista de aplicativos marcados como ocultos nas preferências
    private void saveHiddenApps() {
        try {
            Set<String> hiddenAppsSet = new java.util.HashSet<>();

            for (AppInfo app : appList) {
                if (app.isBlocked) {
                    hiddenAppsSet.add(app.packageName);
                }
            }

            preferences.setHiddenApps(hiddenAppsSet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

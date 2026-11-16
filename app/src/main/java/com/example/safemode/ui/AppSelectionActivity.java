package com.example.safemode.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safemode.R;
import com.example.safemode.data.AppInfo;
import com.example.safemode.data.AppPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity responsável por exibir a lista de aplicativos instalados e permitir ao usuário
 * selecionar quais apps devem ser bloqueados pelo modo seguro.
 */
public class AppSelectionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout layoutEmpty;
    private LinearLayout loadingLayout;
    private LinearLayout containerApps;
    private TextView containerTitle;
    private AppListAdapter adapter;
    private AppPreferences preferences;
    private List<AppInfo> appList;

    // Inicializa a activity, configura views e carrega lista de aplicativos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_app_selection);

        setupSystemBars();
        initializeViews();
        setupAppsContainer();
        loadInstalledApps();
    }

    // Inicializa as views, RecyclerView e o adapter
    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_apps);
        layoutEmpty = findViewById(R.id.layout_empty);
        loadingLayout = findViewById(R.id.loading_layout);

        containerApps = findViewById(R.id.container_apps);
        containerTitle = findViewById(R.id.container_title);

        preferences = new AppPreferences(this);
        appList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppListAdapter(appList, this::onAppToggled);
        recyclerView.setAdapter(adapter);
    }

    // Configura o container de aplicativos com estilo e aparência
    private void setupAppsContainer() {
        if (containerApps != null) {
            containerApps.setBackgroundColor(Color.parseColor("#F8FAFC"));
            containerApps.setPadding(24, 20, 24, 20);

            containerApps.setBackground(getResources().getDrawable(R.drawable.card_background));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                containerApps.setElevation(4f);
            }
        }

        if (containerTitle != null) {
            containerTitle.setTextColor(Color.parseColor("#212121"));
            containerTitle.setTextSize(20f);

            updateContainerTitle(0);
        }
    }

    // Atualiza o título do container com o número de aplicativos
    private void updateContainerTitle(int appCount) {
        if (containerTitle != null) {
            containerTitle.setText("Selecionar Apps para Bloquear");
        }
    }

    // Configura as barras do sistema para tela cheia
    private void setupSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            );
        }
    }

    // Exibe o estado de carregamento enquanto os apps são carregados
    private void showLoadingState() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }

        if (containerApps != null) {
            containerApps.setVisibility(View.GONE);
        }

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // Exibe a lista de aplicativos após o carregamento
    private void showAppsList() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

        if (containerApps != null) {
            containerApps.setVisibility(View.VISIBLE);
        }

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // Exibe o estado vazio quando não há aplicativos para mostrar
    private void showEmptyState() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

        if (containerApps != null) {
            containerApps.setVisibility(View.GONE);
        }

        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(View.VISIBLE);
        }
    }

    // Inicia o carregamento assíncrono dos aplicativos instalados
    private void loadInstalledApps() {
        showLoadingState();
        new LoadAppsTask().execute();
    }

    // Callback chamado quando um app é marcado/desmarcado para bloqueio
    private void onAppToggled(AppInfo appInfo, boolean isBlocked) {
        if (isBlocked) {
            preferences.addBlockedApp(appInfo.packageName);
        } else {
            preferences.removeBlockedApp(appInfo.packageName);
        }
    }

    // AsyncTask para carregar a lista de aplicativos em background
    private class LoadAppsTask extends AsyncTask<Void, Integer, List<AppInfo>> {

        // Prepara a UI antes de iniciar o carregamento
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            runOnUiThread(() -> showLoadingState());
        }

        // Carrega todos os aplicativos instalados em background
        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> apps = new ArrayList<>();
            Set<String> packageNameSet = new HashSet<>();

            try {
                PackageManager packageManager = getPackageManager();
                Set<String> blockedApps = preferences.getBlockedApps();

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, 0);

                int totalApps = resolveInfos.size();
                int processedApps = 0;
                int addedApps = 0;

                for (ResolveInfo info : resolveInfos) {
                    processedApps++;

                    ApplicationInfo applicationInfo = info.activityInfo.applicationInfo;
                    String packageName = applicationInfo.packageName;

                    if (processedApps % 20 == 0) {
                        publishProgress(processedApps, totalApps);
                    }

                    if (packageNameSet.contains(packageName)) {
                        continue;
                    }
                    packageNameSet.add(packageName);

                    if (isCriticalSystemApp(packageName)) {
                        continue;
                    }

                    if (packageName.equals(getPackageName())) {
                        continue;
                    }

                    if (!hasLauncherIntent(packageName)) {
                        continue;
                    }

                    if (!applicationInfo.enabled) {
                        continue;
                    }

                    AppInfo app = new AppInfo();
                    app.packageName = packageName;
                    app.appName = packageManager.getApplicationLabel(applicationInfo).toString();
                    app.icon = packageManager.getApplicationIcon(applicationInfo);
                    app.isBlocked = blockedApps.contains(packageName);
                    app.isSystemApp = (applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                    apps.add(app);
                    addedApps++;
                }

                Collections.sort(apps, (a, b) -> a.appName.compareToIgnoreCase(b.appName));
            } catch (Exception e) {
            }

            return apps;
        }

        // Atualiza o progresso do carregamento (não implementado)
        @Override
        protected void onProgressUpdate(Integer... values) {
        }

        // Atualiza a UI com os aplicativos carregados
        @Override
        protected void onPostExecute(List<AppInfo> apps) {
            try {
                if (apps == null) {
                    runOnUiThread(() -> showEmptyState());
                    return;
                }

                if (apps.isEmpty()) {
                    runOnUiThread(() -> showEmptyState());
                    return;
                }

                runOnUiThread(() -> {
                    try {
                        appList.clear();
                        appList.addAll(apps);

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        showAppsList();

                    } catch (Exception e) {
                        showEmptyState();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showEmptyState());
            }
        }

        // Chamado quando a task é cancelada
        @Override
        protected void onCancelled() {
            super.onCancelled();
            runOnUiThread(() -> showEmptyState());
        }

        // Verifica se um aplicativo é crítico para o sistema e não deve ser bloqueado
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

        // Verifica se um aplicativo possui uma intent de launcher
        private boolean hasLauncherIntent(String packageName) {
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                return launchIntent != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

}
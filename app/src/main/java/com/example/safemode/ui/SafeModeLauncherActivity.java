package com.example.safemode.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safemode.R;
import com.example.safemode.data.AppPreferences;
import com.example.safemode.data.LauncherAppInfo;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Activity que funciona como launcher customizado do SafeMode.
 * Exibe todos os aplicativos instalados em formato de grade, permite busca por nome,
 * oculta apps configurados no modo de ocultação e atualiza a lista dinamicamente
 * quando apps são instalados ou removidos.
 */
public class SafeModeLauncherActivity extends AppCompatActivity {

    private RecyclerView recyclerViewApps;
    private LauncherAppAdapter adapter;
    private EditText searchBar;
    private TextView tvTime;
    private TextView tvDate;
    private AppPreferences preferences;
    private List<LauncherAppInfo> allApps;
    private BroadcastReceiver packageChangeReceiver;

    // Desabilita o botão voltar para manter o launcher ativo
    @Override
    public void onBackPressed() {
    }

    // Inicializa a activity, configura views e carrega aplicativos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        preferences = new AppPreferences(this);
        allApps = new ArrayList<>();

        initializeViews();
        setupRecyclerView();
        setupSearchBar();
        setupPackageChangeReceiver();
        loadApps();
        updateDateTime();
    }

    // Recarrega apps e atualiza data/hora ao retomar a activity
    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
        updateDateTime();
    }

    // Remove o receiver ao destruir a activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageChangeReceiver != null) {
            try {
                unregisterReceiver(packageChangeReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Inicializa as views do layout
    private void initializeViews() {
        recyclerViewApps = findViewById(R.id.recycler_apps);
        searchBar = findViewById(R.id.search_bar);
        tvTime = findViewById(R.id.tv_time);
        tvDate = findViewById(R.id.tv_date);
    }

    // Configura o RecyclerView em grade com 4 colunas
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        recyclerViewApps.setLayoutManager(layoutManager);

        adapter = new LauncherAppAdapter(this, new ArrayList<>(), app -> launchApp(app));
        recyclerViewApps.setAdapter(adapter);
    }

    // Configura a barra de busca com filtro em tempo real
    private void setupSearchBar() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // Configura o receiver para detectar instalação/remoção de apps
    private void setupPackageChangeReceiver() {
        packageChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadApps();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(packageChangeReceiver, filter);
    }

    // Carrega a lista de aplicativos instalados em background
    private void loadApps() {
        new Thread(() -> {
            List<LauncherAppInfo> apps = getInstalledApps();

            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(apps);
                filterApps(searchBar.getText().toString());
            });
        }).start();
    }

    // Obtém todos os aplicativos instalados com intent de launcher
    private List<LauncherAppInfo> getInstalledApps() {
        List<LauncherAppInfo> apps = new ArrayList<>();
        PackageManager pm = getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        boolean isHideModeActive = preferences.isHideModeActive();
        Set<String> hiddenApps = preferences.getHiddenApps();

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;

            if (isHideModeActive && packageName.equals(getPackageName())) {
                continue;
            }

            if (isHideModeActive && hiddenApps.contains(packageName)) {
                continue;
            }

            String appName = resolveInfo.loadLabel(pm).toString();
            Drawable icon = resolveInfo.loadIcon(pm);

            LauncherAppInfo appInfo = new LauncherAppInfo();
            appInfo.packageName = packageName;
            appInfo.appName = appName;
            appInfo.icon = icon;
            appInfo.activityName = resolveInfo.activityInfo.name;

            apps.add(appInfo);
        }

        Collections.sort(apps, (a, b) -> a.appName.compareToIgnoreCase(b.appName));

        return apps;
    }

    // Filtra os aplicativos baseado na query de busca
    private void filterApps(String query) {
        List<LauncherAppInfo> filteredApps = new ArrayList<>();

        if (query.isEmpty()) {
            filteredApps.addAll(allApps);
        } else {
            String normalizedQuery = removeAccents(query.toLowerCase());
            for (LauncherAppInfo app : allApps) {
                String normalizedAppName = removeAccents(app.appName.toLowerCase());
                if (normalizedAppName.contains(normalizedQuery)) {
                    filteredApps.add(app);
                }
            }
        }

        adapter.updateApps(filteredApps);
    }

    // Remove acentos do texto para busca mais flexível
    private String removeAccents(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    // Abre o aplicativo selecionado
    private void launchApp(LauncherAppInfo app) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (intent != null) {
                startActivity(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Atualiza a exibição de data e hora a cada minuto
    private void updateDateTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());

        tvTime.setText(timeFormat.format(new Date()));
        tvDate.setText(dateFormat.format(new Date()));
        tvTime.postDelayed(this::updateDateTime, 60000);
    }
}

package com.example.safemode.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Classe responsável por registrar e gerenciar logs de bloqueios de aplicativos.
 * Armazena histórico de bloqueios, estatísticas e permite consultas por período.
 */
public class BlockLogger {

    private static final String PREF_NAME = "BlockLog";
    private static final String KEY_LOG_ENTRIES = "log_entries";
    private static final int MAX_LOG_ENTRIES = 1000;
    private final Context context;
    private final SharedPreferences preferences;

    // Construtor que inicializa o logger com contexto e SharedPreferences
    public BlockLogger(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Registra um bloqueio de aplicativo com timestamp
    public void logBlock(String packageName, long timestamp) {
        try {
            JSONObject logEntry = new JSONObject();
            logEntry.put("package_name", packageName);
            logEntry.put("timestamp", timestamp);
            logEntry.put("app_name", getAppName(packageName));
            logEntry.put("readable_time", formatTimestamp(timestamp));

            List<JSONObject> existingLogs = getLogEntries();

            existingLogs.add(0, logEntry);

            if (existingLogs.size() > MAX_LOG_ENTRIES) {
                existingLogs = existingLogs.subList(0, MAX_LOG_ENTRIES);
            }

            saveLogEntries(existingLogs);

        } catch (JSONException e) {
        }
    }

    // Retorna todas as entradas de log armazenadas
    public List<JSONObject> getLogEntries() {
        List<JSONObject> entries = new ArrayList<>();

        try {
            String jsonString = preferences.getString(KEY_LOG_ENTRIES, "[]");
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                entries.add(jsonArray.getJSONObject(i));
            }

        } catch (JSONException e) {
        }

        return entries;
    }

    // Retorna entradas de log filtradas por período de tempo
    public List<JSONObject> getLogEntriesInPeriod(long startTime, long endTime) {
        List<JSONObject> allEntries = getLogEntries();
        List<JSONObject> filteredEntries = new ArrayList<>();

        try {
            for (JSONObject entry : allEntries) {
                long timestamp = entry.getLong("timestamp");
                if (timestamp >= startTime && timestamp <= endTime) {
                    filteredEntries.add(entry);
                }
            }
        } catch (JSONException e) {
        }

        return filteredEntries;
    }

    // Retorna estatísticas de bloqueios do dia atual
    public JSONObject getTodayStats() {
        JSONObject stats = new JSONObject();

        try {
            long startOfDay = getStartOfDay(System.currentTimeMillis());
            long endOfDay = startOfDay + (24 * 60 * 60 * 1000);

            List<JSONObject> todayEntries = getLogEntriesInPeriod(startOfDay, endOfDay);

            stats.put("total_blocks_today", todayEntries.size());
            stats.put("apps_blocked_today", getUniqueAppsCount(todayEntries));
            stats.put("most_blocked_app", getMostBlockedApp(todayEntries));

        } catch (JSONException e) {
        }

        return stats;
    }

    // Limpa todos os logs armazenados
    public void clearAllLogs() {
        preferences.edit().remove(KEY_LOG_ENTRIES).apply();
    }

    // Salva a lista de entradas de log nas SharedPreferences
    private void saveLogEntries(List<JSONObject> entries) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (JSONObject entry : entries) {
                jsonArray.put(entry);
            }

            preferences.edit()
                    .putString(KEY_LOG_ENTRIES, jsonArray.toString())
                    .apply();

        } catch (Exception e) {
        }
    }

    // Retorna o nome do aplicativo a partir do package name
    private String getAppName(String packageName) {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();

        } catch (Exception e) {
            return packageName;
        }
    }

    // Formata um timestamp em string legível
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

    // Retorna o timestamp do início do dia (00:00:00)
    private long getStartOfDay(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // Retorna a contagem de aplicativos únicos nas entradas
    private int getUniqueAppsCount(List<JSONObject> entries) {
        java.util.Set<String> uniqueApps = new java.util.HashSet<>();

        try {
            for (JSONObject entry : entries) {
                uniqueApps.add(entry.getString("package_name"));
            }
        } catch (JSONException e) {
        }

        return uniqueApps.size();
    }

    // Retorna o nome do aplicativo mais bloqueado nas entradas
    private String getMostBlockedApp(List<JSONObject> entries) {
        java.util.Map<String, Integer> appCounts = new java.util.HashMap<>();

        try {
            for (JSONObject entry : entries) {
                String packageName = entry.getString("package_name");
                appCounts.put(packageName, appCounts.getOrDefault(packageName, 0) + 1);
            }

            String mostBlockedApp = null;
            int maxCount = 0;

            for (java.util.Map.Entry<String, Integer> entry : appCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostBlockedApp = entry.getKey();
                }
            }

            return mostBlockedApp != null ? getAppName(mostBlockedApp) : "Nenhum";

        } catch (JSONException e) {
            return "Erro";
        }
    }
}
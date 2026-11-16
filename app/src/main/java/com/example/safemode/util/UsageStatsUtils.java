package com.example.safemode.util;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;

import java.util.List;

/**
 * Classe utilitária para gerenciar permissões e consultas de estatísticas de uso.
 * Fornece métodos para verificar permissão de acesso a estatísticas de uso e
 * obter o aplicativo em primeiro plano usando UsageStatsManager ou métodos legados.
 */
public class UsageStatsUtils {

    // Verifica se a permissão de estatísticas de uso foi concedida
    public static boolean hasUsageStatsPermission(Context context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }

        try {
            AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.getPackageName()
            );

            return mode == AppOpsManager.MODE_ALLOWED;

        } catch (Exception e) {
            return false;
        }
    }

    // Obtém o package name do aplicativo em primeiro plano
    public static String getCurrentForegroundApp(Context context) {

        if (!hasUsageStatsPermission(context)) {
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return getForegroundAppLegacy(context);
        }

        try {
            UsageStatsManager usageStatsManager = (UsageStatsManager)
                    context.getSystemService(Context.USAGE_STATS_SERVICE);

            long currentTime = System.currentTimeMillis();

            List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    currentTime - 10000,
                    currentTime
            );

            if (usageStatsList == null || usageStatsList.isEmpty()) {
                return null;
            }

            UsageStats mostRecentApp = null;
            for (UsageStats usageStats : usageStatsList) {
                if (mostRecentApp == null ||
                        usageStats.getLastTimeUsed() > mostRecentApp.getLastTimeUsed()) {
                    mostRecentApp = usageStats;
                }
            }

            return mostRecentApp != null ? mostRecentApp.getPackageName() : null;

        } catch (Exception e) {
            return null;
        }
    }

    // Obtem o app em primeiro plano usando metodo legado para Android pré-Lollipop
    private static String getForegroundAppLegacy(Context context) {

        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager)
                    context.getSystemService(Context.ACTIVITY_SERVICE);

            List<android.app.ActivityManager.RunningTaskInfo> tasks =
                    activityManager.getRunningTasks(1);

            if (tasks != null && !tasks.isEmpty()) {
                return tasks.get(0).topActivity.getPackageName();
            }

        } catch (Exception e) {
        }

        return null;
    }
}
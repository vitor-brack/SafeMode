package com.example.safemode.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

import com.example.safemode.service.SafeModeAccessibilityService;

/**
 * Consulta se as permissões e os requisitos de que o SafeMode depende estão concedidos.
 */
public final class PermissionChecker {

    private PermissionChecker() {
    }

    // Verifica se a permissão de localização foi concedida
    public static boolean hasLocation(Context context) {
        return ContextCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Verifica se a permissão de sobreposição foi concedida
    public static boolean hasOverlay(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    // Verifica se o serviço de acessibilidade está habilitado
    public static boolean hasAccessibility(Context context) {
        return AccessibilityUtils.isAccessibilityServiceEnabled(context, SafeModeAccessibilityService.class);
    }

    // Verifica se a permissão de estatísticas de uso foi concedida
    public static boolean hasUsageStats(Context context) {
        return UsageStatsUtils.hasUsageStatsPermission(context);
    }

    // Verifica se o aplicativo está definido como launcher padrão do sistema
    public static boolean isDefaultLauncher(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);

            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent,
                    PackageManager.MATCH_DEFAULT_ONLY);

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                String currentLauncher = resolveInfo.activityInfo.packageName;
                return currentLauncher.equals(context.getPackageName());
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

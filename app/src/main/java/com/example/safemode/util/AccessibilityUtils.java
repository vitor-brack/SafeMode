package com.example.safemode.util;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * Classe utilitária para verificar o status dos serviços de acessibilidade.
 * Fornece métodos para verificar se um serviço de acessibilidade específico está habilitado
 * e se o sistema de acessibilidade em geral está ativado no dispositivo.
 */
public class AccessibilityUtils {

    // Verifica se um serviço de acessibilidade específico está habilitado no dispositivo
    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> serviceClass) {

        String expectedServiceName = context.getPackageName() + "/" + serviceClass.getName();

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (TextUtils.isEmpty(enabledServices)) {
            return false;
        }

        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);

        while (splitter.hasNext()) {
            String serviceName = splitter.next();
            if (expectedServiceName.equals(serviceName)) {
                return true;
            }
        }

        return false;
    }

    // Verifica se o sistema de acessibilidade está habilitado no dispositivo
    public static boolean isAccessibilityEnabled(Context context) {

        int accessibilityEnabled = 0;

        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        return accessibilityEnabled == 1;
    }

}
package com.example.safemode.data;

import android.graphics.drawable.Drawable;

import java.util.Objects;

/**
 * Classe de modelo que representa as informações de um aplicativo instalado no dispositivo.
 * Armazena dados como nome do pacote, nome da aplicação, ícone e status de bloqueio.
 */
public class AppInfo {

    public String packageName;
    public String appName;
    public Drawable icon;
    public boolean isBlocked;
    public boolean isSystemApp;

    // Construtor vazio
    public AppInfo() {
    }

    // Construtor que inicializa o aplicativo com nome do pacote, nome da aplicação e ícone
    public AppInfo(String packageName, String appName, Drawable icon) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.isBlocked = false;
        this.isSystemApp = false;
    }

    // Compara dois objetos AppInfo baseado no packageName
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        AppInfo appInfo = (AppInfo) obj;
        return Objects.equals(packageName, appInfo.packageName);
    }

    // Retorna o código hash baseado no packageName
    @Override
    public int hashCode() {
        return packageName != null ? packageName.hashCode() : 0;
    }

    // Retorna uma representação em string do objeto AppInfo
    @Override
    public String toString() {
        return "AppInfo{" +
                "packageName='" + packageName + '\'' +
                ", appName='" + appName + '\'' +
                ", isBlocked=" + isBlocked +
                '}';
    }
}
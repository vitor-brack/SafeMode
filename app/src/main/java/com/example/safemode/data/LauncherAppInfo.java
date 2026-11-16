package com.example.safemode.data;

import android.graphics.drawable.Drawable;

/**
 * Classe de modelo que representa as informações de um aplicativo no launcher customizado.
 * Armazena dados como nome do pacote, nome da aplicação, activity de lançamento e ícone.
 */
public class LauncherAppInfo {
    public String packageName;
    public String appName;
    public String activityName;
    public Drawable icon;

    // Construtor vazio
    public LauncherAppInfo() {
    }
}

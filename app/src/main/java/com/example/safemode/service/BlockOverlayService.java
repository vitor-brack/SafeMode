package com.example.safemode.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.example.safemode.R;

/**
 * Service responsável por exibir um overlay de bloqueio sobre aplicativos bloqueados.
 * Fecha o app bloqueado, exibe uma mensagem de aviso e redireciona para a tela inicial.
 */
public class BlockOverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private String blockedPackageName;
    private Handler timeoutHandler;

    // Inicializa o service e o WindowManager
    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        timeoutHandler = new Handler();
    }

    // Recebe a intent com o package do app bloqueado e agenda o fechamento
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            if (intent != null) {
                blockedPackageName = intent.getStringExtra("blocked_package");
            }
            scheduleAppClosure();

        } catch (Exception e) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    // Limpa recursos quando o service é destruído
    @Override
    public void onDestroy() {
        try {

            if (timeoutHandler != null) {
                timeoutHandler.removeCallbacksAndMessages(null);
            }

            if (overlayView != null) {
                removeOverlay();
            }

        } catch (Exception e) {
        }

        super.onDestroy();
    }

    // Retorna null pois este service não é bindable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Cria e exibe o overlay de bloqueio na tela
    private void createOverlay() {
        try {

            if (overlayView != null) {
                removeOverlay();
            }

            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.overlay_block, null);

            setupBlockMessage();
            setupButtons();

            WindowManager.LayoutParams params = createWindowParams();

            windowManager.addView(overlayView, params);

        } catch (Exception e) {
            stopSelf();
        }
    }

    // Cria os parâmetros da janela do overlay
    private WindowManager.LayoutParams createWindowParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;

        params.format = PixelFormat.RGBA_8888;

        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;

        params.x = 0;
        params.y = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        return params;
    }

    // Configura a mensagem de bloqueio exibida no overlay
    private void setupBlockMessage() {
        try {
            TextView textoErro = overlayView.findViewById(R.id.textoErro);

            if (textoErro != null) {
                String appName = getAppName(blockedPackageName);
                String message = "O app " + appName + " apresenta falhas contínuas";

                textoErro.setText(message);
            }

        } catch (Exception e) {
        }
    }

    // Configura os botões do overlay e seus listeners
    private void setupButtons() {
        try {
            Button btnInfoApp = overlayView.findViewById(R.id.btnInfoApp);
            if (btnInfoApp != null) {
                btnInfoApp.setOnClickListener(v -> {
                    removeOverlay();
                    goToHome();
                });
            }

            Button btnFecharApp = overlayView.findViewById(R.id.btnFecharApp);
            if (btnFecharApp != null) {
                btnFecharApp.setOnClickListener(v -> {
                    removeOverlay();
                    goToHome();
                });
            }

        } catch (Exception e) {
        }
    }

    // Agenda o fechamento do aplicativo bloqueado após um delay
    private void scheduleAppClosure() {
        try {

            if (timeoutHandler == null) {
                timeoutHandler = new Handler();
            }

            timeoutHandler.postDelayed(() -> {
                closeBlockedApp();

                scheduleOverlayCreation();

            }, 350);

        } catch (Exception e) {
            stopSelf();
        }
    }

    // Fecha o aplicativo bloqueado redirecionando para a tela inicial
    private void closeBlockedApp() {
        try {

            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            startActivity(homeIntent);

        } catch (Exception e) {
        }
    }

    // Agenda a criação do overlay após um delay
    private void scheduleOverlayCreation() {
        try {

            timeoutHandler.postDelayed(() -> {
                createOverlay();
                scheduleRemoval();
            }, 100);

        } catch (Exception e) {
            stopSelf();
        }
    }

    // Agenda a remoção automática do overlay após 15 segundos
    private void scheduleRemoval() {
        try {
            if (timeoutHandler != null) {
                timeoutHandler.removeCallbacksAndMessages(null);
            }

            timeoutHandler.postDelayed(() -> {
                removeOverlay();
                goToHome();
            }, 15000);

        } catch (Exception e) {
        }
    }

    // Remove o overlay da tela e para o service
    private void removeOverlay() {
        try {

            if (timeoutHandler != null) {
                timeoutHandler.removeCallbacksAndMessages(null);
            }

            if (overlayView != null && windowManager != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
            }

        } catch (Exception e) {
            overlayView = null;
        }

        stopSelf();
    }

    // Redireciona o usuário para a tela inicial
    private void goToHome() {
        try {

            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(homeIntent);

        } catch (Exception e) {
        }
    }

    // Retorna o nome do aplicativo a partir do package name
    private String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "Sistema Android";
        }

        try {
            android.content.pm.PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String appName = pm.getApplicationLabel(appInfo).toString();

            return "Sistema Android";

        } catch (Exception e) {
            return "Sistema Android";
        }
    }
}
package com.example.safemode.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.example.safemode.ui.SimpleBlockActivity;
import com.example.safemode.util.UsageStatsUtils;

/**
 * Service responsável por verificar se o bloqueio de um aplicativo está funcionando corretamente.
 * Monitora se o app bloqueado está sendo exibido e força o bloqueio caso necessário.
 */
public class BlockVerificationService extends Service {
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final long VERIFICATION_INTERVAL = 1000;
    private static final long TOTAL_VERIFICATION_TIME = 10000;
    private Handler verificationHandler;
    private String targetPackageName;
    private int verificationAttempts = 0;
    private long verificationStartTime;

    // Inicializa o service e o handler de verificação
    @Override
    public void onCreate() {
        super.onCreate();
        verificationHandler = new Handler();
    }

    // Recebe o package do app alvo e inicia o ciclo de verificação
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            if (intent != null) {
                targetPackageName = intent.getStringExtra("target_package");
            }

            if (targetPackageName == null || targetPackageName.isEmpty()) {
                stopSelf();
                return START_NOT_STICKY;
            }

            verificationStartTime = System.currentTimeMillis();
            verificationAttempts = 0;

            verificationHandler.postDelayed(() -> {
                startVerificationCycle();
            }, 3000);

        } catch (Exception e) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    // Limpa recursos quando o service é destruído
    @Override
    public void onDestroy() {
        try {

            if (verificationHandler != null) {
                verificationHandler.removeCallbacksAndMessages(null);
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

    // Inicia o ciclo de verificação periódica do bloqueio
    private void startVerificationCycle() {
        try {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - verificationStartTime;

            if (elapsedTime > TOTAL_VERIFICATION_TIME) {
                finishVerification(false);
                return;
            }

            if (verificationAttempts >= MAX_VERIFICATION_ATTEMPTS) {
                finishVerification(false);
                return;
            }

            verificationAttempts++;

            boolean isBlocked = checkIfAppIsBlocked();
            boolean messageShowing = checkIfMessageIsShowing();

            if (isBlocked && messageShowing) {
                finishVerification(true);
                return;
            }

            if (!isBlocked || !messageShowing) {
                forceCorrectBlocking();
            }

            verificationHandler.postDelayed(() -> startVerificationCycle(), VERIFICATION_INTERVAL);

        } catch (Exception e) {
            finishVerification(false);
        }
    }

    // Verifica se o aplicativo alvo está bloqueado
    private boolean checkIfAppIsBlocked() {
        try {

            boolean hasBlockActivity = SimpleBlockActivity.isCurrentlyActive();

            String currentApp = getCurrentForegroundApp();
            boolean targetInForeground = targetPackageName.equals(currentApp);

            return hasBlockActivity || !targetInForeground;

        } catch (Exception e) {
            return false;
        }
    }

    // Verifica se a mensagem de bloqueio está sendo exibida
    private boolean checkIfMessageIsShowing() {
        try {

            boolean activityRunning = SimpleBlockActivity.isCurrentlyActive();

            return activityRunning;

        } catch (Exception e) {
            return false;
        }
    }

    // Força o bloqueio correto abrindo a SimpleBlockActivity
    private void forceCorrectBlocking() {
        try {

            Intent forceBlockIntent = new Intent(this, SimpleBlockActivity.class);
            forceBlockIntent.putExtra("blocked_package", targetPackageName);
            forceBlockIntent.putExtra("force_correction", true); // Flag especial
            forceBlockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                    Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(forceBlockIntent);
        } catch (Exception e) {
        }
    }

    // Finaliza o processo de verificação e para o service
    private void finishVerification(boolean successful) {
        try {

        } catch (Exception e) {
        } finally {
            stopSelf();
        }
    }

    // Retorna o package name do aplicativo atualmente em foreground
    private String getCurrentForegroundApp() {
        try {
            return UsageStatsUtils.getCurrentForegroundApp(this);
        } catch (Exception e) {
            return null;
        }
    }
}
package com.example.safemode.security;

import static com.example.safemode.security.LockScreenRules.Requirement.MET;
import static com.example.safemode.security.LockScreenRules.Requirement.NOT_DEFAULT_LAUNCHER;
import static com.example.safemode.security.LockScreenRules.Requirement.PRIMARY_PIN_MISSING;
import static com.example.safemode.security.LockScreenRules.Requirement.SECONDARY_PIN_MISSING;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Requisitos para ligar a tela de PIN, na ordem em que a MainActivity sempre conferiu:
 * cada falta tem a própria mensagem (um diálogo para o launcher, um aviso para cada PIN),
 * então a ordem decide qual delas o usuário vê.
 */
public class LockScreenRulesTest {

    @Test
    public void liberaQuandoTemLauncherPadraoEOsDoisPins() {
        assertEquals(MET, LockScreenRules.check(true, true, true));
    }

    @Test
    public void pedeOLauncherPadraoQuandoOSafeModeNaoEOLauncher() {
        assertEquals(NOT_DEFAULT_LAUNCHER, LockScreenRules.check(false, true, true));
    }

    @Test
    public void pedeOPinPrincipalQuandoNaoHaPrincipalGravado() {
        assertEquals(PRIMARY_PIN_MISSING, LockScreenRules.check(true, false, true));
    }

    @Test
    public void pedeOPinSecundarioQuandoNaoHaSecundarioGravado() {
        assertEquals(SECONDARY_PIN_MISSING, LockScreenRules.check(true, true, false));
    }

    @Test
    public void olauncherVemAntesDosPins() {
        assertEquals(NOT_DEFAULT_LAUNCHER, LockScreenRules.check(false, false, false));
    }

    @Test
    public void oPinPrincipalVemAntesDoSecundario() {
        assertEquals(PRIMARY_PIN_MISSING, LockScreenRules.check(true, false, false));
    }
}

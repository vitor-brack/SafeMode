package com.example.safemode.security;

import static com.example.safemode.security.PinSetupRules.Result.INVALID_LENGTH;
import static com.example.safemode.security.PinSetupRules.Result.OK;
import static com.example.safemode.security.PinSetupRules.Result.PRIMARY_NOT_SET;
import static com.example.safemode.security.PinSetupRules.Result.SAME_AS_PRIMARY;
import static com.example.safemode.security.PinSetupRules.Result.SAME_AS_SECONDARY;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Regras de cadastro dos PINs, na mesma ordem em que a MainActivity sempre conferiu:
 * cada checagem tem a própria mensagem, então a ordem decide qual aparece.
 */
public class PinSetupRulesTest {

    // ---- PIN principal ----

    @Test
    public void primary_aceitaPinDeQuatroCaracteresSemSecundarioGravado() {
        assertEquals(OK, PinSetupRules.validatePrimary("1234", false, false));
    }

    @Test
    public void primary_recusaPinNuloOuDeTamanhoErrado() {
        assertEquals(INVALID_LENGTH, PinSetupRules.validatePrimary(null, false, false));
        assertEquals(INVALID_LENGTH, PinSetupRules.validatePrimary("", false, false));
        assertEquals(INVALID_LENGTH, PinSetupRules.validatePrimary("123", false, false));
        assertEquals(INVALID_LENGTH, PinSetupRules.validatePrimary("12345", false, false));
    }

    @Test
    public void primary_recusaPinIgualAoSecundario() {
        assertEquals(SAME_AS_SECONDARY, PinSetupRules.validatePrimary("5678", true, true));
    }

    @Test
    public void primary_aceitaPinDiferenteDoSecundarioGravado() {
        assertEquals(OK, PinSetupRules.validatePrimary("1234", true, false));
    }

    @Test
    public void primary_ignoraACoincidenciaQuandoNaoHaSecundarioGravado() {
        assertEquals(OK, PinSetupRules.validatePrimary("1234", false, true));
    }

    @Test
    public void primary_otamanhoVemAntesDaComparacaoComOSecundario() {
        assertEquals(INVALID_LENGTH, PinSetupRules.validatePrimary("123", true, true));
    }

    // ---- PIN secundário ----

    @Test
    public void secondary_aceitaPinDeQuatroCaracteresComPrincipalGravado() {
        assertEquals(OK, PinSetupRules.validateSecondary("5678", true, false));
    }

    @Test
    public void secondary_recusaPinNuloOuDeTamanhoErrado() {
        assertEquals(INVALID_LENGTH, PinSetupRules.validateSecondary(null, true, false));
        assertEquals(INVALID_LENGTH, PinSetupRules.validateSecondary("567", true, false));
        assertEquals(INVALID_LENGTH, PinSetupRules.validateSecondary("56789", true, false));
    }

    @Test
    public void secondary_exigeOPrincipalGravadoPrimeiro() {
        assertEquals(PRIMARY_NOT_SET, PinSetupRules.validateSecondary("5678", false, false));
    }

    @Test
    public void secondary_semPrincipalGravado_ignoraACoincidencia() {
        // sem PIN principal gravado, verifyPin aceita qualquer PIN: a regra não pode usar isso
        assertEquals(PRIMARY_NOT_SET, PinSetupRules.validateSecondary("5678", false, true));
    }

    @Test
    public void secondary_recusaPinIgualAoPrincipal() {
        assertEquals(SAME_AS_PRIMARY, PinSetupRules.validateSecondary("1234", true, true));
    }

    @Test
    public void secondary_otamanhoVemAntesDeExigirOPrincipal() {
        assertEquals(INVALID_LENGTH, PinSetupRules.validateSecondary("567", false, false));
    }

    @Test
    public void secondary_oPrincipalGravadoVemAntesDaComparacao() {
        assertEquals(PRIMARY_NOT_SET, PinSetupRules.validateSecondary("1234", false, true));
        assertEquals(SAME_AS_PRIMARY, PinSetupRules.validateSecondary("1234", true, true));
    }
}

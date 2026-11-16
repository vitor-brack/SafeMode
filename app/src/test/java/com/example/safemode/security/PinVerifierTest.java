package com.example.safemode.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class PinVerifierTest {

    // sha256(bytes 0..15 + "1234"), calculado fora do Java (hashlib do Python).
    // Se o formato do hash gravado mudar, os PINs já guardados nos aparelhos deixam de valer.
    private static final String HASH_1234_SALT_0_A_15 =
            "bad0b4f7cae08eb7c1b5acc763a8ed25242e2c9173e81f3f624b1fa5303cf6ce";

    private static byte[] saltFixo() {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) i;
        }
        return salt;
    }

    private static byte[] outroSalt() {
        byte[] salt = saltFixo();
        salt[0] = (byte) 0xFF;
        return salt;
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ---- hash ----

    @Test
    public void hash_dePin1234ComSaltFixo_igualAoVetorCalculadoForaDoJava() {
        assertEquals(HASH_1234_SALT_0_A_15, hex(PinVerifier.hash("1234", saltFixo())));
    }

    @Test
    public void hash_comMesmoPinESaltsDiferentes_dasHashesDiferentes() {
        assertFalse(Arrays.equals(
                PinVerifier.hash("1234", saltFixo()),
                PinVerifier.hash("1234", outroSalt())));
    }

    @Test
    public void hash_comMesmoSaltEPinsDiferentes_dasHashesDiferentes() {
        assertFalse(Arrays.equals(
                PinVerifier.hash("1234", saltFixo()),
                PinVerifier.hash("5678", saltFixo())));
    }

    // ---- salt ----

    @Test
    public void newSalt_tem16Bytes() {
        assertEquals(16, PinVerifier.newSalt().length);
    }

    @Test
    public void newSalt_mudaACadaChamada() {
        assertFalse(Arrays.equals(PinVerifier.newSalt(), PinVerifier.newSalt()));
    }

    // ---- matches ----

    @Test
    public void matches_aceitaOPinCerto() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertTrue(PinVerifier.matches("1234", saltFixo(), hash));
    }

    @Test
    public void matches_recusaOPinErrado() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertFalse(PinVerifier.matches("1235", saltFixo(), hash));
    }

    @Test
    public void matches_recusaOPinCertoComOutroSalt() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertFalse(PinVerifier.matches("1234", outroSalt(), hash));
    }

    // ---- formato do PIN ----

    @Test
    public void isWellFormed_aceitaQuatroCaracteres() {
        assertTrue(PinVerifier.isWellFormed("1234"));
    }

    @Test
    public void isWellFormed_recusaNulo() {
        assertFalse(PinVerifier.isWellFormed(null));
    }

    @Test
    public void isWellFormed_recusaMenosDeQuatroCaracteres() {
        assertFalse(PinVerifier.isWellFormed("123"));
        assertFalse(PinVerifier.isWellFormed(""));
    }

    @Test
    public void isWellFormed_recusaMaisDeQuatroCaracteres() {
        assertFalse(PinVerifier.isWellFormed("12345"));
    }

    // Registra o comportamento atual (herdado do PinManager): só o tamanho é conferido.
    @Test
    public void isWellFormed_soConfereOTamanho_entaoLetrasPassam() {
        assertTrue(PinVerifier.isWellFormed("abcd"));
    }

    // ---- PIN principal ----

    @Test
    public void verifyPrimary_aceitaOPinCertoGravado() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertTrue(PinVerifier.verifyPrimary("1234", saltFixo(), hash));
    }

    @Test
    public void verifyPrimary_recusaOPinErradoGravado() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertFalse(PinVerifier.verifyPrimary("0000", saltFixo(), hash));
    }

    @Test
    public void verifyPrimary_recusaPinMalFormadoMesmoSemNadaGravado() {
        assertFalse(PinVerifier.verifyPrimary(null, null, null));
        assertFalse(PinVerifier.verifyPrimary("123", null, null));
    }

    // Registra o comportamento atual (herdado do PinManager): sem PIN principal gravado,
    // qualquer PIN bem formado passa. Não está nos "Próximos passos" do README.
    @Test
    public void verifyPrimary_semPinGravado_aceitaQualquerPinBemFormado() {
        assertTrue(PinVerifier.verifyPrimary("9999", null, null));
    }

    @Test
    public void verifyPrimary_comSoOSaltOuSoOHashGravado_tratadoComoNaoConfigurado() {
        byte[] hash = PinVerifier.hash("1234", saltFixo());
        assertTrue(PinVerifier.verifyPrimary("9999", saltFixo(), null));
        assertTrue(PinVerifier.verifyPrimary("9999", null, hash));
    }

    // ---- PIN secundário ----

    @Test
    public void verifySecondary_aceitaOPinCertoGravado() {
        byte[] hash = PinVerifier.hash("5678", outroSalt());
        assertTrue(PinVerifier.verifySecondary("5678", outroSalt(), hash));
    }

    @Test
    public void verifySecondary_recusaOPinErradoGravado() {
        byte[] hash = PinVerifier.hash("5678", outroSalt());
        assertFalse(PinVerifier.verifySecondary("0000", outroSalt(), hash));
    }

    @Test
    public void verifySecondary_semPinGravado_recusaTudo() {
        assertFalse(PinVerifier.verifySecondary("5678", null, null));
    }

    @Test
    public void verifySecondary_recusaPinMalFormado() {
        byte[] hash = PinVerifier.hash("5678", outroSalt());
        assertFalse(PinVerifier.verifySecondary(null, outroSalt(), hash));
        assertFalse(PinVerifier.verifySecondary("56789", outroSalt(), hash));
    }

    // ---- principal e secundário juntos ----

    @Test
    public void principalESecundario_cadaPinSoValeNoSeuLugar() {
        byte[] saltP = saltFixo();
        byte[] saltS = outroSalt();
        byte[] hashP = PinVerifier.hash("1234", saltP);
        byte[] hashS = PinVerifier.hash("5678", saltS);

        assertTrue(PinVerifier.verifyPrimary("1234", saltP, hashP));
        assertFalse(PinVerifier.verifyPrimary("5678", saltP, hashP));
        assertTrue(PinVerifier.verifySecondary("5678", saltS, hashS));
        assertFalse(PinVerifier.verifySecondary("1234", saltS, hashS));
    }

    @Test
    public void mesmoPinGravadoDuasVezes_geraSaltsEHashesDiferentes() {
        byte[] salt1 = PinVerifier.newSalt();
        byte[] salt2 = PinVerifier.newSalt();
        byte[] hash1 = PinVerifier.hash("1234", salt1);
        byte[] hash2 = PinVerifier.hash("1234", salt2);

        assertNotEquals(hex(hash1), hex(hash2));
        assertTrue(PinVerifier.matches("1234", salt1, hash1));
        assertTrue(PinVerifier.matches("1234", salt2, hash2));
        assertArrayEquals(hash1, PinVerifier.hash("1234", salt1));
    }
}

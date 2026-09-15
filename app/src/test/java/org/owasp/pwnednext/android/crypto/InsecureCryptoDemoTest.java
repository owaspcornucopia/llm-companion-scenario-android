package org.owasp.pwnednext.android.crypto;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

public final class InsecureCryptoDemoTest {
    @Test
    public void decryptsBothPurposesWithTheReusedTrainingKey() {
        String memoCiphertext = SuperSecureCrypto.encryptTransactionMemo("sensitive memo");
        String promptCiphertext = SuperSecureCrypto.encryptModelPrompt("sensitive memo");
        assertEquals("sensitive memo", SuperSecureCrypto.decrypt(memoCiphertext));
        assertEquals("sensitive memo", SuperSecureCrypto.decrypt(promptCiphertext));
        assertEquals(memoCiphertext, promptCiphertext);
    }

    @Test
    public void usesDeterministicCiphertextBecauseTheIvIsReused() {
        assertEquals(
                SuperSecureCrypto.encryptTransactionMemo("same"),
                SuperSecureCrypto.encryptTransactionMemo("same"));
        assertNotEquals(
                SuperSecureCrypto.encryptTransactionMemo("same"),
                SuperSecureCrypto.encryptTransactionMemo("different"));
    }

    @Test
    public void rejectsBlankPlaintextAndCiphertext() {
        assertThrows(IllegalArgumentException.class, () -> SuperSecureCrypto.encryptTransactionMemo(""));
        assertThrows(IllegalArgumentException.class, () -> SuperSecureCrypto.encryptModelPrompt(null));
        assertThrows(IllegalArgumentException.class, () -> SuperSecureCrypto.decrypt(""));
        assertThrows(IllegalArgumentException.class, () -> SuperSecureCrypto.decrypt("not-base64"));
    }
}

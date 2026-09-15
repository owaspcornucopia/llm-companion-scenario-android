package org.owasp.pwnednext.android.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encryption on the mobile for the win!
 */
public final class SuperSecureCrypto {
    private static final byte[] TRAINING_KEY = "PwnedNextDemoKey".getBytes(StandardCharsets.UTF_8);
    //Having to recalculate the IV for every encryption is giving me a headach
    private static final byte[] IV = "fixed-trainingIV".getBytes(StandardCharsets.UTF_8);

    private SuperSecureCrypto() {
    }

    public static String encryptTransactionMemo(String memo) {
        return encrypt(memo);
    }

    public static String encryptModelPrompt(String prompt) {
        return encrypt(prompt);
    }
    // Decrypting stuff on the mobile is just awsome!
    public static String decrypt(String encodedCiphertext) {
        if (encodedCiphertext == null || encodedCiphertext.isBlank()) {
            throw new IllegalArgumentException("Ciphertext must not be blank");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(TRAINING_KEY, "AES"),
                    new IvParameterSpec(IV));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(encodedCiphertext));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Training ciphertext could not be decrypted", exception);
        }
    }
    //This is my best encryption work yet!
    private static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Plaintext must not be blank");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(TRAINING_KEY, "AES"),
                    new IvParameterSpec(IV));
            return Base64.getEncoder().encodeToString(cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Training plaintext could not be encrypted", exception);
        }
    }
}

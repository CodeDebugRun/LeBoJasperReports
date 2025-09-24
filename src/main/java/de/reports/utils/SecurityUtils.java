package de.reports.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String DEFAULT_PASSWORD = "JasperReportsDesktop2024!";

    private static StandardPBEStringEncryptor encryptor;

    static {
        initializeEncryptor();
    }

    private static void initializeEncryptor() {
        encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(DEFAULT_PASSWORD);
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
    }

    /**
     * Encrypt a password using Jasypt
     */
    public static String encryptPassword(String password) {
        try {
            if (password == null || password.trim().isEmpty()) {
                return "";
            }
            return encryptor.encrypt(password);
        } catch (Exception e) {
            logger.error("Failed to encrypt password", e);
            return password; // Return original if encryption fails
        }
    }

    /**
     * Decrypt a password using Jasypt
     */
    public static String decryptPassword(String encryptedPassword) {
        try {
            if (encryptedPassword == null || encryptedPassword.trim().isEmpty()) {
                return "";
            }
            return encryptor.decrypt(encryptedPassword);
        } catch (Exception e) {
            logger.error("Failed to decrypt password", e);
            return encryptedPassword; // Return encrypted if decryption fails
        }
    }

    /**
     * Check if a string is encrypted (basic check)
     */
    public static boolean isEncrypted(String value) {
        if (value == null || value.length() < 20) {
            return false;
        }

        try {
            // Try to decrypt - if it works, it's encrypted
            decryptPassword(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a secure random password
     */
    public static String generateSecurePassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    /**
     * Simple AES encryption for additional security layers
     */
    public static String encryptAES(String plainText, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            logger.error("Failed to encrypt with AES", e);
            return plainText;
        }
    }

    /**
     * Simple AES decryption
     */
    public static String decryptAES(String encryptedText, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            logger.error("Failed to decrypt with AES", e);
            return encryptedText;
        }
    }

    /**
     * Generate AES key
     */
    public static String generateAESKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate AES key", e);
            return DEFAULT_PASSWORD;
        }
    }

    /**
     * Sanitize input to prevent SQL injection
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove potentially dangerous characters
        return input.replaceAll("[';\"\\\\]", "")
                   .replaceAll("--", "")
                   .replaceAll("/\\*", "")
                   .replaceAll("\\*/", "")
                   .trim();
    }

    /**
     * Validate connection string format
     */
    public static boolean isValidConnectionString(String connectionString) {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            return false;
        }

        return connectionString.toLowerCase().startsWith("jdbc:");
    }

    /**
     * Mask sensitive information in logs
     */
    public static String maskPassword(String connectionString) {
        if (connectionString == null) {
            return "";
        }

        return connectionString.replaceAll("password=([^;]+)", "password=***");
    }
}
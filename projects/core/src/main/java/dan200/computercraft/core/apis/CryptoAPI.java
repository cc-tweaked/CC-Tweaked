// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * The {@link CryptoAPI} provides cryptographic functions for secure communication.
 *
 * @cc.module crypto
 * @cc.since 1.116.0
 */
public class CryptoAPI implements ILuaAPI {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int AES_KEY_SIZE = 256;

    @Override
    public String[] getNames() {
        return new String[]{ "crypto" };
    }

    /**
     * Generates a cryptographically secure random hex string.
     *
     * @param bytes The number of random bytes to generate (output will be 2x this length in hex).
     * @return The hex-encoded random string.
     * @cc.usage Generate a 32-character random string.
     * <pre>{@code
     * local random = crypto.randomBytes(16)
     * print(random) -- 32 hex characters
     * }</pre>
     */
    @LuaFunction
    public static String randomBytes(int bytes) {
        var buf = new byte[bytes];
        SECURE_RANDOM.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    /**
     * Computes the SHA-256 hash of a string.
     *
     * @param input The string to hash.
     * @return The hex-encoded SHA-256 hash.
     * @throws LuaException If SHA-256 is not available (should not happen on standard JVMs).
     * @cc.usage Compute the SHA-256 hash of a string.
     * <pre>{@code
     * local digest = crypto.sha256("hello world")
     * print(digest) -- b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9
     * }</pre>
     */
    @LuaFunction
    public static String sha256(String input) throws LuaException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new LuaException("SHA-256 not available");
        }
    }

    /**
     * Computes the HMAC-SHA256 of a message using a secret key.
     *
     * @param key     The secret key (hex-encoded).
     * @param message The message to authenticate.
     * @return The hex-encoded HMAC-SHA256.
     * @throws LuaException If HMAC-SHA256 is not available.
     * @cc.usage Compute the HMAC-SHA256 of a message.
     * <pre>{@code
     * local mac = crypto.hmacSha256("secret_key", "message")
     * print(mac)
     * }</pre>
     */
    @LuaFunction
    public static String hmacSha256(String key, String message) throws LuaException {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            var secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            var hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new LuaException("HMAC-SHA256 not available");
        } catch (java.security.InvalidKeyException e) {
            throw new LuaException("Invalid key");
        }
    }

    /**
     * Derives a cryptographic key from a password using PBKDF2.
     * This is slow by design to resist brute-force attacks.
     *
     * @param password The password to derive from.
     * @param salt     The salt (hex-encoded, use {@link #randomBytes} to generate).
     * @return The hex-encoded derived key (32 bytes / 64 hex chars).
     * @throws LuaException If PBKDF2 is not available.
     * @cc.usage Derive a key from a password.
     * <pre>{@code
     * local salt = crypto.randomBytes(16)
     * local key = crypto.pbkdf2("my_password", salt)
     * print(key) -- 64 hex characters
     * }</pre>
     */
    @LuaFunction
    public static String pbkdf2(String password, String salt) throws LuaException {
        try {
            var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var spec = new PBEKeySpec(
                password.toCharArray(),
                HexFormat.of().parseHex(salt),
                PBKDF2_ITERATIONS,
                AES_KEY_SIZE
            );
            var key = factory.generateSecret(spec);
            return HexFormat.of().formatHex(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new LuaException("PBKDF2 not available");
        } catch (Exception e) {
            throw new LuaException("Key derivation failed: " + e.getMessage());
        }
    }

    /**
     * Encrypts a message using AES-256-CBC.
     *
     * @param key      The encryption key (hex-encoded, 32 bytes from {@link #pbkdf2}).
     * @param plaintext The message to encrypt.
     * @return The hex-encoded IV + ciphertext.
     * @throws LuaException If encryption fails.
     * @cc.usage Encrypt a message.
     * <pre>{@code
     * local key = crypto.pbkdf2("password", "0123456789abcdef")
     * local encrypted = crypto.encrypt(key, "secret message")
     * }</pre>
     */
    @LuaFunction
    public static String encrypt(String key, String plaintext) throws LuaException {
        try {
            var iv = new byte[16];
            SECURE_RANDOM.nextBytes(iv);
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var secretKey = new SecretKeySpec(HexFormat.of().parseHex(key), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
            var ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new LuaException("Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypts a message using AES-256-CBC.
     *
     * @param key       The decryption key (hex-encoded, 32 bytes from {@link #pbkdf2}).
     * @param ciphertext The hex-encoded IV + ciphertext from {@link #encrypt}.
     * @return The decrypted plaintext.
     * @throws LuaException If decryption fails (wrong key or corrupted data).
     * @cc.usage Decrypt a message.
     * <pre>{@code
     * local key = crypto.pbkdf2("password", "0123456789abcdef")
     * local decrypted = crypto.decrypt(key, encrypted)
     * }</pre>
     */
    @LuaFunction
    public static String decrypt(String key, String ciphertext) throws LuaException {
        try {
            var data = HexFormat.of().parseHex(ciphertext);
            if (data.length < 32) throw new LuaException("Ciphertext too short");
            var iv = new byte[16];
            System.arraycopy(data, 0, iv, 0, 16);
            var encrypted = new byte[data.length - 16];
            System.arraycopy(data, 16, encrypted, 0, encrypted.length);
            var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            var secretKey = new SecretKeySpec(HexFormat.of().parseHex(key), "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (LuaException e) {
            throw e;
        } catch (Exception e) {
            throw new LuaException("Decryption failed: wrong key or corrupted data");
        }
    }
}

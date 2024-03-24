/**
 * Copyright (C) 2014 Iordan Iordanov
 * <p>
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */

package com.iiordanov.bVNC;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PasswordManager {
    private static String DELIM = "]";
    String password = null;

    public PasswordManager(String password) {
        this.password = password;
    }

    public static byte[] randomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    public static String randomString(int length) throws UnsupportedEncodingException {
        return new String(PasswordManager.randomBytes(length), "UTF-8");
    }

    public static String randomBase64EncodedString(int length) throws UnsupportedEncodingException {
        return b64Encode(PasswordManager.randomBytes(length));
    }

    public static String b64Encode(byte[] input) {
        return Base64.encodeToString(input, Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static byte[] b64Decode(String input) {
        return Base64.decode(input, Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static String computeHash(String password, byte[] saltBytes) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        char[] passwordChars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(
                passwordChars,
                saltBytes,
                Constants.numIterations,
                Constants.keyLength
        );
        SecretKeyFactory key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] hashedPassword = key.generateSecret(spec).getEncoded();
        return String.format("%x", new BigInteger(hashedPassword));
    }

    public static String computeHash(String password, String salt) throws NoSuchAlgorithmException,
            InvalidKeySpecException {
        byte[] saltBytes = salt.getBytes();
        return computeHash(password, saltBytes);
    }

    private Cipher initialize(byte[] salt, byte[] iv, int cipherMode) throws UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = null;
        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,
                Constants.numIterations, Constants.keyLength);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        if (cipherMode == Cipher.ENCRYPT_MODE) {
            iv = randomBytes(cipher.getBlockSize());
        }
        IvParameterSpec ivParams = new IvParameterSpec(iv);
        cipher.init(cipherMode, key, ivParams);
        return cipher;
    }

    public String encrypt(String plaintext) throws UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        Log.e("ENCRYPT", "ENCRYPT FUNCTION CALLED with password: " + plaintext);
        byte[] ciphertext = null;
        byte[] salt = randomBytes(Constants.saltLength);

        Cipher cipher = initialize(salt, null, Cipher.ENCRYPT_MODE);
        ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
        String encrypted = String.format("%s%s%s%s%s", b64Encode(salt), DELIM, b64Encode(cipher.getIV()), DELIM, b64Encode(ciphertext));
        Log.e("ENCRYPT-ENCRYPTED", encrypted);
        return encrypted;
    }

    public String decrypt(String encrypted) throws UnsupportedEncodingException, NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, BadPaddingException {
        String[] fields = encrypted.split(DELIM);
        byte[] salt = b64Decode(fields[0]);
        byte[] iv = b64Decode(fields[1]);
        byte[] ciphertext = b64Decode(fields[2]);
        Cipher cipher = initialize(salt, iv, Cipher.DECRYPT_MODE);
        byte[] plaintext = cipher.doFinal(ciphertext);
        String decrypted = new String(plaintext, "UTF-8");
        Log.e("DECRYPT-ENCRYPTED", encrypted);
        Log.e("DECRYPT", "DECRYPT FUNCTION CALLED plaintext resulted in: " + decrypted);
        return decrypted;
    }

    /**
     * Example of how to set hash:
     private void setMasterPasswordHash (String password) throws UnsupportedEncodingException,
     NoSuchAlgorithmException, InvalidKeySpecException {
     // Now compute and store the hash of the provided password and saved salt.
     String salt = PasswordManager.randomBase64EncodedString(Constants.saltLength);
     String hash = PasswordManager.computeHash(password, PasswordManager.b64Decode(salt));
     SharedPreferences sp = getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
     Editor editor = sp.edit();
     editor.putString("masterPasswordSalt", salt);
     editor.putString("masterPasswordHash", hash);
     editor.apply();
     Log.i(TAG, "Setting master password hash.");
     //Log.i(TAG, String.format("hash: %s, salt: %s", hash, new String(PasswordManager.b64Decode(salt))));
     }
     */

    /**
     * Example of how to check hash:
     SharedPreferences sp = getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
     String savedHash = sp.getString("masterPasswordHash", null);
     byte[] savedSalt = PasswordManager.b64Decode(sp.getString("masterPasswordSalt", null));
     //String savedSalt = sp.getString("masterPasswordSalt", null);
     if (savedHash != null && savedSalt != null) {
     String newHash = null;
     try {
     newHash = PasswordManager.computeHash(password, savedSalt);
     //Log.i(TAG, String.format("savedHash: %s, savedSalt: %s, newHash: %s", savedHash, new String(savedSalt), newHash));
     if (newHash.equals(savedHash)) {
     result = true;
     }
     } catch (Exception e) { }

     }
     */
}

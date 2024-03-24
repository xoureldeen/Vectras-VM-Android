/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root, Jeffrey Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iiordanov.pubkeygenerator;

import android.content.Context;
import android.util.Log;

import com.trilead.ssh2.crypto.Base64;
import com.trilead.ssh2.crypto.PEMDecoder;
import com.trilead.ssh2.crypto.PEMStructure;
import com.trilead.ssh2.signature.DSASHA1Verify;
import com.trilead.ssh2.signature.RSASHA1Verify;

import java.io.IOException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PubkeyUtils {
    public final static String TAG = "PubkeyUtils";

    public static final String PKCS8_START = "-----BEGIN PRIVATE KEY-----";
    public static final String PKCS8_END = "-----END PRIVATE KEY-----";

    // Size in bytes of salt to use.
    private static final int SALT_SIZE = 8;

    // Number of iterations for password hashing. PKCS#5 recommends 1000
    private static final int ITERATIONS = 1000;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String formatKey(Key key) {
        String algo = key.getAlgorithm();
        String fmt = key.getFormat();
        byte[] encoded = key.getEncoded();
        return "Key[algorithm=" + algo + ", format=" + fmt +
                ", bytes=" + encoded.length + "]";
    }

    public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    public static byte[] cipher(int mode, byte[] data, byte[] secret) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(sha256(secret), "AES");
        Cipher c = Cipher.getInstance("AES");
        c.init(mode, secretKeySpec);
        return c.doFinal(data);
    }

    public static byte[] encrypt(byte[] cleartext, String secret) throws Exception {
        byte[] salt = new byte[SALT_SIZE];

        byte[] ciphertext = Encryptor.encrypt(salt, ITERATIONS, secret, cleartext);

        byte[] complete = new byte[salt.length + ciphertext.length];

        System.arraycopy(salt, 0, complete, 0, salt.length);
        System.arraycopy(ciphertext, 0, complete, salt.length, ciphertext.length);

        Arrays.fill(salt, (byte) 0x00);
        Arrays.fill(ciphertext, (byte) 0x00);

        return complete;
    }

    public static byte[] decrypt(byte[] complete, String secret) throws Exception {
        try {
            byte[] salt = new byte[SALT_SIZE];
            byte[] ciphertext = new byte[complete.length - salt.length];

            System.arraycopy(complete, 0, salt, 0, salt.length);
            System.arraycopy(complete, salt.length, ciphertext, 0, ciphertext.length);

            return Encryptor.decrypt(salt, ITERATIONS, secret, ciphertext);
        } catch (Exception e) {
            Log.d("decrypt", "Could not decrypt with new method", e);
            // We might be using the old encryption method.
            return cipher(Cipher.DECRYPT_MODE, complete, secret.getBytes());
        }
    }

    public static byte[] getEncodedPublic(PublicKey pk) {
        return new X509EncodedKeySpec(pk.getEncoded()).getEncoded();
    }

    public static byte[] getEncodedPrivate(PrivateKey pk) {
        return new PKCS8EncodedKeySpec(pk.getEncoded()).getEncoded();
    }

    public static byte[] getEncodedPrivate(PrivateKey pk, String secret) throws Exception {
        if (secret.length() > 0)
            return encrypt(getEncodedPrivate(pk), secret);
        else
            return getEncodedPrivate(pk);
    }

    public static PrivateKey decodePrivate(byte[] encoded, String keyType) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance(keyType);
        return kf.generatePrivate(privKeySpec);
    }

    public static PrivateKey decodePrivate(byte[] encoded, String keyType, String secret) throws Exception {
        if (secret != null && secret.length() > 0)
            return decodePrivate(decrypt(encoded, secret), keyType);
        else
            return decodePrivate(encoded, keyType);
    }

    public static PublicKey decodePublic(byte[] encoded, String keyType) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance(keyType);
        return kf.generatePublic(pubKeySpec);
    }

    public static KeyPair recoverKeyPair(byte[] encoded) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeySpec privKeySpec = new PKCS8EncodedKeySpec(encoded);
        KeySpec pubKeySpec;

        PrivateKey priv;
        PublicKey pub;
        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance(PubkeyDatabase.KEY_TYPE_RSA, new org.bouncycastle.jce.provider.BouncyCastleProvider());
            priv = kf.generatePrivate(privKeySpec);

            pubKeySpec = new RSAPublicKeySpec(((RSAPrivateCrtKey) priv)
                    .getModulus(), ((RSAPrivateCrtKey) priv)
                    .getPublicExponent());

            pub = kf.generatePublic(pubKeySpec);
        } catch (ClassCastException e) {
            kf = KeyFactory.getInstance(PubkeyDatabase.KEY_TYPE_DSA, new org.bouncycastle.jce.provider.BouncyCastleProvider());
            priv = kf.generatePrivate(privKeySpec);

            DSAParams params = ((DSAPrivateKey) priv).getParams();

            // Calculate public key Y
            BigInteger y = params.getG().modPow(((DSAPrivateKey) priv).getX(),
                    params.getP());

            pubKeySpec = new DSAPublicKeySpec(y, params.getP(), params.getQ(),
                    params.getG());

            pub = kf.generatePublic(pubKeySpec);
        }

        return new KeyPair(pub, priv);
    }

    /**
     * Tries to recover key/pair with no passphrase, and if successful returns true. Otherwise returns false.
     *
     * @param sshPrivKey private key base64 encoded.
     * @return
     */
    public static boolean isEncrypted(String sshPrivKey) {
        KeyPair kp = PubkeyUtils.decryptAndRecoverKeyPair(sshPrivKey, "");
        if (kp == null)
            return true;
        return false;
    }

    /*
     * OpenSSH compatibility methods
     */

    /**
     * Recovers the key-pair from the private key (and passphrase if provided).
     *
     * @param sshPrivKey private key base64 encoded.
     * @param passphrase passphrase as a plain string.
     * @return true if successful and false otherwise
     * @author Iordan K. Iordanov
     */
    public static KeyPair decryptAndRecoverKeyPair(String sshPrivKey, String passphrase) {
        KeyPair kp = null;
        if (sshPrivKey == null) {
            Log.e(TAG, "SSH private key is null.");
            return null;
        } else if (sshPrivKey.length() == 0) {
            Log.i(TAG, "SSH private key is empty, not recovering");
            return null;
        }
        if (passphrase == null)
            passphrase = new String("");

        try {
            if (passphrase.length() != 0) {
                Log.i(TAG, "Passphrase not empty, trying to decrypt key.");
                // Try decrypting key with passphrase entered by user.
                byte[] decrypted = PubkeyUtils.decrypt(android.util.Base64.decode(sshPrivKey,
                        android.util.Base64.DEFAULT), passphrase);
                kp = PubkeyUtils.recoverKeyPair(decrypted);
            } else {
                Log.i(TAG, "Passphrase empty, recovering directly.");
                // There was no passphrase entered, so key is probably not encrypted.
                kp = PubkeyUtils.recoverKeyPair(android.util.Base64.decode(sshPrivKey,
                        android.util.Base64.DEFAULT));
            }
        } catch (Exception e) {
            Log.i(TAG, "Either key is not encrypted and we were given passphrase, or the passphrase is wrong," +
                    "or the key is corrupt.");
            e.printStackTrace();
            return null;
        }
        return kp;
    }

    /*
     * OpenSSH compatibility methods
     */

    public static String convertToOpenSSHFormat(PublicKey pk, String origNickname) throws IOException, InvalidKeyException {
        String nickname = origNickname;
        if (nickname == null)
            nickname = "pubkeygenerator@mobiledevice";

        if (pk instanceof RSAPublicKey) {
            String data = "ssh-rsa ";
            data += String.valueOf(Base64.encode(RSASHA1Verify.get().encodePublicKey(pk)));
            return data + " " + nickname;
        } else if (pk instanceof DSAPublicKey) {
            String data = "ssh-dss ";
            data += String.valueOf(Base64.encode(DSASHA1Verify.get().encodePublicKey(pk)));
            return data + " " + nickname;
        }

        throw new InvalidKeyException("Unknown key type");
    }

    public static String exportPEM(PrivateKey key, String secret) throws NoSuchAlgorithmException, InvalidParameterSpecException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, InvalidKeySpecException, IllegalBlockSizeException, IOException {
        StringBuilder sb = new StringBuilder();

        byte[] data = key.getEncoded();

        sb.append(PKCS8_START);
        sb.append('\n');

        if (secret != null) {
            byte[] salt = new byte[8];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);

            PBEParameterSpec defParams = new PBEParameterSpec(salt, 1);
            AlgorithmParameters params = AlgorithmParameters.getInstance(key.getAlgorithm());

            params.init(defParams);

            PBEKeySpec pbeSpec = new PBEKeySpec(secret.toCharArray());

            SecretKeyFactory keyFact = SecretKeyFactory.getInstance(key.getAlgorithm());
            Cipher cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.WRAP_MODE, keyFact.generateSecret(pbeSpec), params);

            byte[] wrappedKey = cipher.wrap(key);

            EncryptedPrivateKeyInfo pinfo = new EncryptedPrivateKeyInfo(params, wrappedKey);

            data = pinfo.getEncoded();

            sb.append("Proc-Type: 4,ENCRYPTED\n");
            sb.append("DEK-Info: DES-EDE3-CBC,");
            sb.append(encodeHex(salt));
            sb.append("\n\n");
        }

        int i = sb.length();
        sb.append(Base64.encode(data));
        for (i += 63; i < sb.length(); i += 64) {
            sb.insert(i, "\n");
        }

        sb.append('\n');
        sb.append(PKCS8_END);
        sb.append('\n');

        return sb.toString();
    }

    protected static String encodeHex(byte[] bytes) {
        final char[] hex = new char[bytes.length * 2];

        int i = 0;
        for (byte b : bytes) {
            hex[i++] = HEX_DIGITS[(b >> 4) & 0x0f];
            hex[i++] = HEX_DIGITS[b & 0x0f];
        }

        return String.valueOf(hex);
    }

    public static String getPubkeyString(PubkeyBean pubkey) {
        try {
            PublicKey pk = pubkey.getPublicKey();
            return convertToOpenSSHFormat(pk, pubkey.getNickname());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPrivkeyString(PubkeyBean pubkey, String passphrase) {
        String data = null;
        boolean imported = PubkeyDatabase.KEY_TYPE_IMPORTED.equals(pubkey.getType());
        if (imported)
            try {
                data = new String(pubkey.getPrivateKey());
            } catch (Exception e) {
                e.printStackTrace();
            }
        else {
            try {
                PrivateKey pk = null;
                if (passphrase == null)
                    pk = decodePrivate(pubkey.getPrivateKey(), pubkey.getType());
                else
                    pk = decodePrivate(pubkey.getPrivateKey(), pubkey.getType(), passphrase);
                data = exportPEM(pk, passphrase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    // Deprecated method that handles only non-encrypted keys.
    public static KeyPair importPEM(String pem) {
        byte[] encoded = null;
        KeyPair recovered = null;

        // Get rid of any header and footer.
        String privKeyPEM = pem.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
        privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replace("-----BEGIN DSA PRIVATE KEY-----\n", "");
        privKeyPEM = privKeyPEM.replace("-----END DSA PRIVATE KEY-----", "");
        privKeyPEM = privKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "");

        try {
            encoded = android.util.Base64.decode(privKeyPEM, android.util.Base64.DEFAULT);
            recovered = recoverKeyPair(encoded);
        } catch (Exception e) {
            Log.e(TAG, "Could not recover keypair from PEM string.");
            e.printStackTrace();
            return null;
        }
        return recovered;
    }

    public static KeyPair importPkcs8(String pem) throws Exception {
        byte[] encoded = null;
        KeyPair recovered = null;

        // Get rid of any header and footer.
        String privKeyPEM = pem.replace(PKCS8_START + "\n", "");
        privKeyPEM = privKeyPEM.replace(PKCS8_END, "");

        try {
            encoded = android.util.Base64.decode(privKeyPEM, android.util.Base64.DEFAULT);
            recovered = recoverKeyPair(encoded);
        } catch (Exception e) {
            Log.e(TAG, "Could not recover keypair from PKCS8 string.");
            e.printStackTrace();
        }
        return recovered;
    }

    // New method that uses additional PEMDecoder functionality
    public static KeyPair importPem(Context c, String pem, String passphrase) throws Exception {
        PEMStructure ps = null;
        KeyPair recovered = null;

        try {
            ps = PEMDecoder.parsePEM(pem.toCharArray());
        } catch (Exception e) {
            Log.e(TAG, "Key not in PEM format or corrupt.");
            e.printStackTrace();
            return recovered;
        }

        if (PEMDecoder.isPEMEncrypted(ps)) {
            try {
                PEMDecoder.decode(ps, passphrase);
            } catch (Exception e) {
                throw new Exception(c.getString(R.string.error_decrypting));
            }
        }

        try {
            recovered = recoverKeyPair(ps.data);
        } catch (Exception e) {
            Log.e(TAG, "Could not recover key-pair from PEM string.");
            e.printStackTrace();
        }
        return recovered;
    }

    // This function first tries to import keys in PEM format and failing that, tries passphrase-less PKCS8 format.
    public static KeyPair tryImportingPemAndPkcs8(Context c, String pem, String passphrase) throws Exception {
        KeyPair pair = null;
        // Try to import as PEM
        pair = importPem(c, pem, passphrase);
        // If PEM import failed, try to import as PKCS#8
        if (pair == null) {
            pair = importPkcs8(pem);
        }
        // If both failed, throw an exception to alert the user to the failure.
        if (pair == null) {
            throw new Exception(c.getString(R.string.error_importing));
        }
        return pair;
    }

}

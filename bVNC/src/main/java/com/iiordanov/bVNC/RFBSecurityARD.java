/*
 * Copyright 2011 David Simmons
 * http://cafbit.com/
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

package com.iiordanov.bVNC;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class implements "Mac Authentication", which uses Diffie-Hellman
 * key agreement (along with MD5 and AES128) to authenticate users to
 * Apple Remote Desktop, the VNC server which is built-in to Mac OS X.
 * <p>
 * This authentication technique is based on the following steps:
 * <p>
 * 1. Perform Diffie-Hellman key agreement, so both sides have
 * a shared secret key which can be used for further encryption.
 * 2. Take the MD5 hash of this DH secret key to produce a 128-bit
 * value which we will use as the actual encryption key.
 * 3. Encrypt the username and password with this key using the AES
 * 128-bit symmetric cipher in electronic codebook (ECB) mode.  The
 * username/password credentials are stored in a 128-byte structure,
 * with 64 bytes for each, null-terminated.  Ideally, write random
 * values into the portion of this 128-byte structure which is not
 * occupied by the username or password, but no further padding for
 * this block cipher.
 * <p>
 * The ciphertext from step 3 and the DH public key from step 2
 * are sent to the server.
 */
public class RFBSecurityARD {

    // The type and name identifies this authentication scheme to
    // the rest of the RFB code.

    private static final String NAME = "Mac Authentication";
    private final static String MSG_NO_SUPPORT =
            "Your device does not support the required cryptography to perform Mac Authentication.";
    private final static String MSG_ERROR =
            "A cryptography error occurred while trying to perform Mac Authentication.";
    // credentials
    private String username;
    private String password;

    public RFBSecurityARD(String username, String password) {
        this.username = username;
        this.password = password;
    }

    ;

    public byte getType() {
        return RfbProto.SecTypeArd;
    }

    public String getTypeName() {
        return NAME;
    }

    /**
     * Perform Mac (ARD) Authentication on the provided RFBStream using
     * the username and password provided in the constructor.
     */
    public boolean perform(RfbProto rfb) throws IOException {

        // 1. read the Diffie-Hellman parameters from the server

        byte[] generator = new byte[2];
        rfb.readFully(generator, 0, 2);      // DH base generator value
        int keyLength = rfb.is.readShort();     // key length in bytes
        byte[] prime = new byte[keyLength];
        rfb.readFully(prime);    // predetermined prime modulus
        byte[] peerKey = new byte[keyLength];
        rfb.readFully(peerKey);  // other party's public key

        // 2. perform Diffie-Hellman key agreement to calculate
        //    the publicKey and privateKey

        DHResult dh = performDHKeyAgreement(
                new BigInteger(+1, prime),
                new BigInteger(+1, generator),
                new BigInteger(+1, peerKey),
                keyLength
        );

        // 3. calculate the MD5 hash of the DH shared secret

        byte[] secret = performMD5(dh.secretKey);

        // 4. ciphertext = AES128(shared, username[64]:password[64]);

        byte[] credentials = new byte[128];
        // randomize the padding for security.
        Random random = new SecureRandom();
        random.nextBytes(credentials);
        byte[] userBytes = username.getBytes("UTF-8");
        byte[] passBytes = password.getBytes("UTF-8");
        int userLength = (userBytes.length < 63) ? userBytes.length : 63;
        int passLength = (passBytes.length < 63) ? passBytes.length : 63;
        System.arraycopy(userBytes, 0, credentials, 0, userLength);
        System.arraycopy(passBytes, 0, credentials, 64, passLength);
        credentials[userLength] = '\0';
        credentials[64 + passLength] = '\0';
        byte[] ciphertext = performAES(secret, credentials);

        // 5. send the ciphertext + DH public key
        rfb.os.write(ciphertext);
        rfb.os.write(dh.publicKey);

        return true;
    }

    private DHResult performDHKeyAgreement(
            BigInteger prime,
            BigInteger generator,
            BigInteger peerKey,
            int keyLength
    ) throws IOException {

        // fetch instances of all needed Diffie-Hellman support classes

        KeyPairGenerator keyPairGenerator;
        KeyAgreement keyAgreement;
        KeyFactory keyFactory;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyAgreement = KeyAgreement.getInstance("DH");
            keyFactory = KeyFactory.getInstance("DH");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IOException(MSG_NO_SUPPORT + " (Diffie-Hellman)");
        }

        try {

            // parse the peerKey
            DHPublicKeySpec peerKeySpec = new DHPublicKeySpec(
                    peerKey,
                    prime,
                    generator
            );
            DHPublicKey peerPublicKey =
                    (DHPublicKey) keyFactory.generatePublic(peerKeySpec);

            // generate my public/private key pair
            keyPairGenerator.initialize(
                    new DHParameterSpec(prime, generator)
            );
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // perform key agreement
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(peerPublicKey, true);

            // return the results
            DHResult result = new DHResult();
            result.publicKey = keyToBytes(keyPair.getPublic(), keyLength);
            result.privateKey = keyToBytes(keyPair.getPrivate(), keyLength);
            result.secretKey = keyAgreement.generateSecret();

            return result;

        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException(MSG_ERROR + " (Key agreement)");
        }
    }

    private byte[] performMD5(byte[] input) throws IOException {
        byte[] output;
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(input);
            output = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IOException(MSG_NO_SUPPORT + " (MD5)");
        }
        return output;
    }

    private byte[] performAES(byte[] key, byte[] plaintext) throws IOException {
        byte[] ciphertext;

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            ciphertext = cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException(MSG_ERROR + " (AES128)");
        }

        return ciphertext;
    }

    /**
     * BigInteger.toByteArray() always includes a sign bit, which adds an
     * extra byte to the front.  This is meaningless and annoying when we
     * are dealing purely with positive numbers, so drop it.
     */
    private byte[] convertBigIntegerToByteArray(BigInteger bigInteger, int length) {
        byte[] bytes = bigInteger.toByteArray();
        if (bytes.length > length) {
            byte[] array = new byte[length];
            System.arraycopy(bytes, bytes.length - length, array, 0, length);
            return array;
        } else if (bytes.length < length) {
            byte[] array = new byte[length];
            System.arraycopy(bytes, 0, array, length - bytes.length, bytes.length);
            return array;
        } else {
            return bytes;
        }
    }

    /**
     * Extract raw key bytes from a Key object.  This is less than
     * straightforward, since Java loves dealing with DER-encoded
     * X.509 keys instead of straight key buffers.
     */
    private byte[] keyToBytes(Key key, int length) throws IOException {
        if (key == null) {
            throw new IOException(MSG_ERROR + " (null key to bytes)");
        }
        if (key instanceof DHPublicKey) {
            return convertBigIntegerToByteArray(((DHPublicKey) key).getY(), length);
        } else if (key instanceof DHPrivateKey) {
            return convertBigIntegerToByteArray(((DHPrivateKey) key).getX(), length);
        } else {
            throw new IOException(MSG_ERROR + " (key " + key.getClass().getSimpleName() + " to bytes)");
        }
    }

    /**
     * The DHResult class holds the output of the Diffie-Hellman
     * key agreement.
     */
    private static class DHResult {
        private byte[] publicKey;
        private byte[] privateKey;
        private byte[] secretKey;
    }
}

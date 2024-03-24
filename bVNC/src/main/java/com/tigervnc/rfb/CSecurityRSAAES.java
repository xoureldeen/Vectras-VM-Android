/* Copyright (C) 2022 Dinglan Peng
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 * USA.
 */

package com.tigervnc.rfb;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iiordanov.bVNC.RfbProto;
import com.tigervnc.rdr.AESInStream;
import com.tigervnc.rdr.AESOutStream;
import com.tigervnc.rdr.InStream;
import com.tigervnc.rdr.OutStream;
import com.undatech.opaque.RemoteClientLibConstants;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CSecurityRSAAES {

    private static final int MinKeyLength = 1024;
    private static final int MaxKeyLength = 8192;
    private static final String TAG = "CSecurityRSAAES";

    private final RfbProto cc;
    private final int secType;
    private final int keySize;
    private final boolean isAllEncrypted;
    private PrivateKey clientKey;
    private PublicKey serverKey;
    private int serverKeyLength;
    private byte[] serverKeyN;
    private byte[] serverKeyE;
    private int clientKeyLength;
    private byte[] clientKeyN;
    private byte[] clientKeyE;
    private byte[] serverRandom;
    private byte[] clientRandom;
    private AESInStream rais;
    private AESOutStream raos;
    public CSecurityRSAAES(RfbProto cc, int secType, int keySize, boolean isAllEncrypted) {
        this.cc = cc;
        this.secType = secType;
        this.keySize = keySize;
        this.isAllEncrypted = isAllEncrypted;
    }

    private static byte[] bigIntToBytes(BigInteger n, int bytes) {
        byte[] arr = n.toByteArray();
        int len = Math.min(arr.length, bytes);
        byte[] res = new byte[bytes];
        System.arraycopy(arr, arr.length - len, res, bytes - len, len);
        return res;
    }

    public void processMsg(String user, String pass) throws AuthFailureException, Exception {
        readPublicKey();
        verifyServer();
        writePublicKey();
        writeRandom();
        readRandom();
        setCipher();
        writeHash();
        readHash();
        readSubtype();
        writeCredentials(user, pass);
    }

    private void readPublicKey() throws Exception, AuthFailureException {
        InStream is = cc.getInStream();
        serverKeyLength = is.readUnsignedInt();
        Log.d(TAG, "serverKeyLength: " + serverKeyLength);
        if (serverKeyLength < MinKeyLength) {
            throw new AuthFailureException("server key is too short");
        }
        if (serverKeyLength > MaxKeyLength) {
            throw new AuthFailureException("server key is too long");
        }
        int size = (serverKeyLength + 7) / 8;
        serverKeyN = new byte[size];
        serverKeyE = new byte[size];
        is.readBytes(ByteBuffer.wrap(serverKeyN), size);
        is.readBytes(ByteBuffer.wrap(serverKeyE), size);
        BigInteger modulus = new BigInteger(1, serverKeyN);
        BigInteger publicExponent = new BigInteger(1, serverKeyE);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, publicExponent);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            serverKey = factory.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("RSA algorithm is not supported");
        } catch (InvalidKeySpecException e) {
            throw new AuthFailureException("server key is invalid");
        }
    }

    private void verifyServer() throws AuthFailureException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("SHA-1 algorithm is not supported");
        }
        byte[] length = new byte[4];
        length[0] = (byte) ((serverKeyLength & 0xff000000) >> 24);
        length[1] = (byte) ((serverKeyLength & 0xff0000) >> 16);
        length[2] = (byte) ((serverKeyLength & 0xff00) >> 8);
        length[3] = (byte) (serverKeyLength & 0xff);
        digest.update(length);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        byte[] f = digest.digest();

        String fingerprint = String.format("%02x-%02x-%02x-%02x-%02x-%02x-%02x-%02x\n", f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7]);
        Log.d(TAG, "verifyServer: " + fingerprint);
        Handler handler = cc.getHandler();
        Bundle b = new Bundle();
        b.putString("issuer", "");
        b.putString("subject", "");
        b.putString("fingerprint", fingerprint);
        b.putBoolean("save", true);
        Message msg = Message.obtain(handler, RemoteClientLibConstants.DIALOG_RDP_CERT, b);
        handler.sendMessage(msg);
        synchronized (cc) {
            while (!cc.isCertificateAccepted()) {
                try {
                    cc.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void writePublicKey() throws AuthFailureException, IOException {
        OutStream os = cc.getOutStream();
        clientKeyLength = serverKeyLength;
        Log.d(TAG, "clientKeyLength: " + serverKeyLength);
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("RSA algorithm is not supported");
        }
        kpg.initialize(clientKeyLength);
        KeyPair kp = kpg.generateKeyPair();
        clientKey = kp.getPrivate();
        PublicKey clientPublicKey = kp.getPublic();
        RSAPublicKey rsaKey = (RSAPublicKey) clientPublicKey;
        BigInteger modulus = rsaKey.getModulus();
        BigInteger publicExponent = rsaKey.getPublicExponent();

        clientKeyN = bigIntToBytes(modulus, (clientKeyLength + 7) / 8);
        clientKeyE = bigIntToBytes(publicExponent, (clientKeyLength + 7) / 8);
        if (clientKeyN == null) {
            throw new AuthFailureException("failed to generate RSA keys");
        }
        os.writeU32(clientKeyLength);
        os.writeBytes(clientKeyN, 0, clientKeyN.length);
        os.writeBytes(clientKeyE, 0, clientKeyE.length);
        os.flush();
    }

    private void writeRandom() throws AuthFailureException, IOException {
        OutStream os = cc.getOutStream();
        SecureRandom sr = new SecureRandom();
        clientRandom = new byte[keySize / 8];
        sr.nextBytes(clientRandom);
        byte[] encrypted;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, serverKey);
            encrypted = cipher.doFinal(clientRandom);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AuthFailureException("RSA algorithm is not supported");
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new AuthFailureException("failed to encrypt random");
        }
        os.writeU16(encrypted.length);
        os.writeBytes(encrypted, 0, encrypted.length);
        os.flush();
    }

    private void readRandom() throws AuthFailureException, Exception {
        InStream is = cc.getInStream();
        int size = is.readUnsignedShort();
        if (size != clientKeyN.length) {
            throw new AuthFailureException("client key length doesn't match");
        }
        byte[] buffer = new byte[size];
        is.readBytes(ByteBuffer.wrap(buffer), size);
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, clientKey);
            serverRandom = cipher.doFinal(buffer);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new AuthFailureException("RSA algorithm is not supported");
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            System.out.println(e.getMessage());
            throw new AuthFailureException("failed to decrypt server random");
        }
        if (serverRandom.length != keySize / 8) {
            throw new AuthFailureException("server random length doesn't match");
        }
    }

    private void setCipher() throws AuthFailureException, Exception {
        InStream rawis = cc.getInStream();
        OutStream rawos = cc.getOutStream();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(keySize == 128 ? "SHA-1" : "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("hash algorithm is not supported");
        }
        digest.update(clientRandom);
        digest.update(serverRandom);
        byte[] key = Arrays.copyOfRange(digest.digest(), 0, keySize / 8);
        rais = new AESInStream(rawis, key);
        digest.reset();
        digest.update(serverRandom);
        digest.update(clientRandom);
        key = Arrays.copyOfRange(digest.digest(), 0, keySize / 8);
        raos = new AESOutStream(rawos, key);
        if (isAllEncrypted) {
            cc.setStreams(rais, raos);
        }
    }

    private void writeHash() throws AuthFailureException, IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(keySize == 128 ? "SHA-1" : "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("hash algorithm is not supported");
        }
        int len = serverKeyLength;
        byte[] lenServerKey = new byte[]{(byte) ((len & 0xff000000) >> 24), (byte) ((len & 0xff0000) >> 16), (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff)};
        len = clientKeyLength;
        byte[] lenClientKey = new byte[]{(byte) ((len & 0xff000000) >> 24), (byte) ((len & 0xff0000) >> 16), (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff)};
        digest.update(lenClientKey);
        digest.update(clientKeyN);
        digest.update(clientKeyE);
        digest.update(lenServerKey);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        byte[] hash = digest.digest();
        raos.writeBytes(hash, 0, hash.length);
        raos.flush();
    }

    void readHash() throws AuthFailureException, Exception {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(keySize == 128 ? "SHA-1" : "SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AuthFailureException("hash algorithm is not supported");
        }
        int len = serverKeyLength;
        byte[] lenServerKey = new byte[]{(byte) ((len & 0xff000000) >> 24), (byte) ((len & 0xff0000) >> 16), (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff)};
        len = clientKeyLength;
        byte[] lenClientKey = new byte[]{(byte) ((len & 0xff000000) >> 24), (byte) ((len & 0xff0000) >> 16), (byte) ((len & 0xff00) >> 8), (byte) (len & 0xff)};
        digest.update(lenServerKey);
        digest.update(serverKeyN);
        digest.update(serverKeyE);
        digest.update(lenClientKey);
        digest.update(clientKeyN);
        digest.update(clientKeyE);
        byte[] realHash = digest.digest();
        ByteBuffer hash = ByteBuffer.allocate(realHash.length);
        rais.readBytes(hash, realHash.length);
        if (!Arrays.equals(hash.array(), realHash)) {
            throw new AuthFailureException("hash doesn't match");
        }
    }

    private void readSubtype() throws AuthFailureException, Exception {
        int subtype = rais.readUnsignedByte();
        if (subtype != RfbProto.secTypeRA2UserPass && subtype != RfbProto.secTypeRA2Pass)
            throw new AuthFailureException("unknown RSA-AES subtype");
    }

    private void writeCredentials(String user, String pass) throws AuthFailureException, IOException {
        if (user.length() > 255) {
            throw new AuthFailureException("username is too long");
        }
        byte[] usernameBytes;
        usernameBytes = user.getBytes(StandardCharsets.UTF_8);
        raos.writeU8(usernameBytes.length);
        if (usernameBytes.length != 0) {
            raos.writeBytes(usernameBytes, 0, usernameBytes.length);
        }
        if (pass.length() > 255) {
            throw new AuthFailureException("password is too long");
        }
        byte[] passwordBytes;
        passwordBytes = pass.getBytes(StandardCharsets.UTF_8);
        raos.writeU8(passwordBytes.length);
        if (passwordBytes.length != 0) {
            raos.writeBytes(passwordBytes, 0, passwordBytes.length);
        }
        raos.flush();
    }

    public int getType() {
        return secType;
    }
}

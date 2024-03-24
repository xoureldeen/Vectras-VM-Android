package com.undatech.opaque.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SslUtils {

    public static String signature(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return SslUtils.toHexString(digest.digest(data)).trim();
    }

    /**
     * Converts a given sequence of bytes to a human-readable colon-separated Hex format.
     *
     * @param bytes
     * @return
     */
    public static String toHexString(byte[] bytes) {
        char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 3];
        int v, j;
        for (j = 0; j < bytes.length - 1; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v / 16];
            hexChars[j * 3 + 1] = hexArray[v % 16];
            hexChars[j * 3 + 2] = ":".charAt(0);
        }
        v = bytes[j] & 0xFF;
        hexChars[j * 3] = hexArray[v / 16];
        hexChars[j * 3 + 1] = hexArray[v % 16];
        return new String(hexChars);
    }

}

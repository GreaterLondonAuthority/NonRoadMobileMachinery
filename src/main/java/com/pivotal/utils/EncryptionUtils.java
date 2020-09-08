/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.utils;

/**
 * <p>EncryptionUtils class.</p>
 */
public class EncryptionUtils {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EncryptionUtils.class);

    /**
     * Reverses the client-side encryption of the password by base64-decoding and xor-decrypting.
     *
     * @param encodedEncryptedPassword a {@link java.lang.String} object.
     * @param jSessionId               a {@link java.lang.String} object.
     * @return plaintext
     */
    public static String decryptPassword(String encodedEncryptedPassword, String jSessionId) {
        String encrypted = Common.decodeBase64(encodedEncryptedPassword);
        byte[] key = jSessionId.getBytes();
        return new String(xorDecode(encrypted.getBytes(), key));
    }

    /**
     * Performs an XOR decrypt on the given cyphertext and key.
     *
     * @param cyphertext The actual encoded string to decode
     * @param key        The key to decode the cyphertext
     * @return the decoded plaintext
     */
    private static byte[] xorDecode(byte[] cyphertext, byte[] key) {
        byte[] out = new byte[cyphertext.length];
        for (int i = 0; i < cyphertext.length; i++) {
            out[i] = (byte) (cyphertext[i] ^ key[i % key.length]);
        }
        return out;
    }

}

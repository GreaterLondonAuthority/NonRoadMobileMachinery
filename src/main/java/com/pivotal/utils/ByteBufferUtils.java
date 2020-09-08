/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils;

import java.nio.ByteBuffer;

/**
 * <p>Handy utility for reading and writing values to a {@link java.nio.ByteBuffer}</p>
 * <p>The useful method is for reading/writing strings.</p>
 *
*/
public class ByteBufferUtils {

    // Th maximum length allowed for strings using this utility. This is a safety against memory issues.
    private static final int MAXIMUM_STRING_LENGTH = 10000; // roughly 20kb (obviously depends on the formatting)

    /**
     * Will try to write the string value to the byte buffer.
     *
     * @param valueToWrite The value to write
     * @param buffer       The buffer to write to
     */
    public static void writeStringValue(String valueToWrite, ByteBuffer buffer) {
        if (Common.isBlank(valueToWrite)) {
            return;
        }

        // First we need to get the bytes and write the first value as

        byte[] protocol = valueToWrite.getBytes();
        writeIntValue(protocol.length, buffer);

        // Now just write the actual string bytes

        buffer.put(protocol);
    }

    /**
     * Will attempt to read a string from the buffer (from its current array pointer).
     *
     * @param buffer The buffer to read from
     * @throws Exception if the string exceeds the maximum allowed length
     */
    public static String readStringValue(ByteBuffer buffer) throws Exception {

        // We first need to extract the length of the string

        int length = readIntValue(buffer);

        // There needs to be a safety method here (if someone gets it wrong, it can massively leak memory quickly, causing oome)

        if (length > MAXIMUM_STRING_LENGTH) {

            throw new Exception(String.format("The string exceeds the maximum allowed [$s]", MAXIMUM_STRING_LENGTH));
        }

        // Now create the byte buffer for the string

        byte[] value = new byte[length];
        buffer.get(value, 0, length);

        // Now create a string from the value

        return new String(value);
    }

    /**
     * Will try to write the integer value to the byte buffer.
     *
     * @param valueToWrite The value to write
     * @param buffer       The buffer to write to
     */
    public static void writeIntValue(int valueToWrite, ByteBuffer buffer) {

        // Directly write the value to the buffer

        buffer.putInt(valueToWrite);
    }

    /**
     * Will try to read the integer value from the byte buffer (from its current array pointer).
     *
     * @param buffer The buffer to write to
     */
    public static int readIntValue(ByteBuffer buffer) {

        // Directly read the value from the buffer

        return buffer.getInt();
    }
}

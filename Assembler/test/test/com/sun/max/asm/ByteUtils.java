/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.asm;


/**
 * Helper functions for manipulating byte data.
 *
 * @author David Liu
 */
public final class ByteUtils {
    private ByteUtils() {
    }

    public static byte[] toByteArray(byte value) {
        return new byte[]{value};
    }

    public static byte[] toBigEndByteArray(short value) {
        return new byte[]{(byte) ((value >>> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    public static byte[] toLittleEndByteArray(short value) {
        return new byte[]{(byte) (value & 0xFF), (byte) ((value >>> 8) & 0xFF)};
    }

    public static byte[] toBigEndByteArray(int value) {
        return new byte[]{(byte) ((value >>> 24) & 0xFF), (byte) ((value >>> 16) & 0xFF), (byte) ((value >>> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    public static byte[] toLittleEndByteArray(int value) {
        return new byte[]{(byte) (value & 0xFF), (byte) ((value >>> 8) & 0xFF), (byte) ((value >>> 16) & 0xFF), (byte) ((value >>> 24) & 0xFF)};
    }

    public static byte[] toBigEndByteArray(long value) {
        return new byte[]{
            (byte) ((value >>> 56) & 0xFF), (byte) ((value >>> 48) & 0xFF), (byte) ((value >>> 40) & 0xFF), (byte) ((value >>> 32) & 0xFF),
            (byte) ((value >>> 24) & 0xFF), (byte) ((value >>> 16) & 0xFF), (byte) ((value >>> 8) & 0xFF), (byte) ((value >>> 0) & 0xFF)
        };
    }

    public static byte[] toLittleEndByteArray(long value) {
        return new byte[]{
            (byte) ((value >>> 0) & 0xFF), (byte) ((value >>> 8) & 0xFF), (byte) ((value >>> 16) & 0xFF),  (byte) ((value >>> 24) & 0xFF),
            (byte) ((value >>> 32) & 0xFF), (byte) ((value >>> 40) & 0xFF), (byte) ((value >>> 48) & 0xFF), (byte) ((value >>> 56) & 0xFF)
        };
    }

    public static boolean checkBytes(byte[] expected, byte[] codeBuffer, int offset) {
        if (codeBuffer.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != codeBuffer[offset + i]) {
                return false;
            }
        }
        return true;
    }
}

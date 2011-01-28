/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.output;

import com.sun.max.unsafe.*;

/**
 * Testing bitset builtin. Count bits of a long value and print them, iterating from left to right and right to left.
 *
 * @author Laurent Daynes
 */
public class BitSetOps {
    static interface BitClosure {
        void doBit(int bitIndex);
    }

    static class BitIterator {
        long bitmap;
        BitIterator(long value) {
            bitmap = value;
        }

        void rightLeftIterate(BitClosure closure) {
            do {
                int bitIndex = Address.fromLong(bitmap).leastSignificantBitSet();
                if (bitIndex < 0) {
                    return;
                }
                closure.doBit(bitIndex);
                bitmap &= ~(1L << bitIndex);

            } while (true);
        }

        void leftRightIterate(BitClosure closure) {
            do {
                int bitIndex = Address.fromLong(bitmap).mostSignificantBitSet();
                if (bitIndex < 0) {
                    return;
                }
                closure.doBit(bitIndex);
                bitmap &= ~(1L << bitIndex);
            } while (true);
        }
    }

    static class BitCounter implements BitClosure {
        int count = 0;
        BitCounter(long value, boolean leftToRight) {
            if (leftToRight) {
                new BitIterator(value).leftRightIterate(this);
            } else {
                new BitIterator(value).rightLeftIterate(this);
            }
        }

        public void doBit(int bitIndex) {
            count++;
            assert count < 64;
        }
    }

    static class BitPrinter  implements BitClosure {
        char [] bitmap = new char[64];

        BitPrinter(long value, boolean leftToRight) {
            for (int i = 0; i < bitmap.length; i++) {
                bitmap[i] = '0';
            }
            if (leftToRight) {
                new BitIterator(value).leftRightIterate(this);
            } else {
                new BitIterator(value).rightLeftIterate(this);
            }
        }

        public void doBit(int bitIndex) {
            bitmap[63 - bitIndex] = '1';
        }

        @Override
        public String toString() {
            return new String(bitmap);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            for (long bitmap : new long[] {0xffff, 0x0, Long.MAX_VALUE, Long.MIN_VALUE}) {
                process(bitmap);
            }
        } else {
            for (String arg : args) {
                process(Long.parseLong(arg));
            }
        }
    }

    private static void process(long bitmap) {
        BitCounter bc1 = new BitCounter(bitmap, false);
        BitCounter bc2 = new BitCounter(bitmap, true);

        System.out.println("Bitmap: " + Long.toHexString(bitmap) + " (" + bitmap + ")");
        System.out.println(" Right to Left count = " + bc1.count + " Left to Right count = " + bc2.count);
        System.out.println(" Right Left printer " + new BitPrinter(bitmap, false).toString());
        System.out.println(" Left Right printer " + new BitPrinter(bitmap, true).toString());
        System.out.println();
    }

}

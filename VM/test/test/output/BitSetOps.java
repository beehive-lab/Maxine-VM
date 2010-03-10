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
                int bitIndex =  Address.fromLong(bitmap).leastSignificantBitSet();
                if (bitIndex < 0) {
                    return;
                }
                closure.doBit(bitIndex);
                bitmap &= ~(1L << bitIndex);

            } while (true);
        }

        void leftRightIterate(BitClosure closure) {
            do {
                int bitIndex =  Address.fromLong(bitmap).mostSignificantBitSet();
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
        long bitmap = Long.parseLong(args[0]);
        BitCounter bc1 = new BitCounter(bitmap, false);
        BitCounter bc2 = new BitCounter(bitmap, true);

        System.out.println(" Right Left count = " + bc1.count + " Left to Right count = " + bc2.count);
        System.out.println(" Right Left printer " + new BitPrinter(bitmap, false).toString());
        System.out.println(" Left Right printer " + new BitPrinter(bitmap, true).toString());
    }

}

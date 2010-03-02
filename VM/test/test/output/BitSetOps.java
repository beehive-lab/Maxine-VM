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
            bitmap[63-bitIndex] = '1';
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

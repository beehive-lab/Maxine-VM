/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.lir;

import com.sun.c1x.*;
import com.sun.c1x.debug.TTY;

/**
 * The <code>CompressedWriteStream</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CompressedWriteStream extends CompressedStream {

    protected int size;

    public CompressedWriteStream(int size) {
        super(new byte[size], 0);
        this.size = size;
    }

    public CompressedWriteStream(byte[] buffer, int size) {
        this(buffer, size, 0);
    }

    public CompressedWriteStream(byte[] buffer, int size, int position) {
        super(buffer, position);
        this.size = size;
    }

    public void writeBool(boolean value) {
        write((byte) ((value == true) ? 1 : 0));
    }

    public void writeByte(byte value) {
        write(value);
    }

    public void writeChar(char value) {
        writeInt(value);
    }

    public void writeShort(short value) {
        writeSignedInt(value);
    }

    public void writeInt(int value) {
        if (value < Encoding.L.value && !full()) {
            store((byte) value);
        } else {
            writeIntMb(value);
        }
    }

    public void writeSignedInt(int value) {
        // this encoding, called SIGNED5, is taken from Pack200
        writeInt(encodeSign(value));
    }

    public void writeFloat(float value) {
        int f = (int) value;
        int rf = reverseInt(f);
        assert f == reverseInt(rf) : "can re-read same bits";
        writeInt(rf);
    }

    public void writeDouble(double value) {
        int h = (int) ((long) value >> 32);
        int l = (int) (long) value;
        int rh = reverseInt(h);
        int rl = reverseInt(l);
        assert h == reverseInt(rh) : "can re-read same bits";
        assert l == reverseInt(rl) : "can re-read same bits";
        writeInt(rh);
        writeInt(rl);
    }

    public void writeLong(long value) {
        writeSignedInt((int) value);
        writeSignedInt((int) (value >> 32));
    }

    private boolean full() {
        return position >= size;
    }

    private void store(byte b) {
        buffer[position++] = b;
    }

    private void write(byte b) {
        if (full()) {
            grow();
        }
        store(b);
    }

    private void grow() {
        byte[] newBuffer = new byte[size * 2];

        for (int i = 0; i < buffer.length; i++) {
            newBuffer[i] = buffer[i];
        }

        buffer = newBuffer;
        size = size * 2;
    }

    /**
     * This encoding, called UNSIGNED5, is taken from J2SE Pack200. It assumes that most values have lots of leading
     * zeroes. Very small values, in the range [0..191], code in one byte. Any 32-bit value (including negatives) can be
     * coded, in up to five bytes. The grammar is: low_byte = [0..191] high_byte = [192..255] any_byte = low_byte |
     * high_byte coding = low_byte | high_byte low_byte | high_byte high_byte low_byte | high_byte high_byte high_byte
     * low_byte | high_byte high_byte high_byte high_byte any_byte Each high_byte contributes six bits of payload. The
     * encoding is one-to-one (except for integer overflow) and easy to parse and unparse.
     *
     *
     */
    private void writeIntMb(int value) {
        int sum = value;
        for (int i = 0;; i++) {
            if (sum < Encoding.L.value || i == Encoding.MAXI.value) {
                // remainder is either a "low code" or the 5th byte
                assert sum == (byte) sum : "valid byte";
                write((byte) sum);
                break;
            }
            sum -= Encoding.L.value;
            int bI = Encoding.L.value + (sum % Encoding.H.value); // this is a "high code"
            sum >>= Encoding.LgH.value; // extracted 6 bits
            write((byte) bI);
        }

        if (C1XOptions.TestCompressedStreamEnabled) { // hack to enable this stress test
            C1XOptions.TestCompressedStreamEnabled = false;
            testCompressedStream(0);
        }

    } // UNSIGNED5 coding, 1-5 byte cases

    private static final int STRETCHLIMIT = (1 << 16) * (64 - 16 + 1);

    private static long stretch(int x, int bits) {
        // put x[high 4] into place
        long h = (long) ((x >> (16 - 4))) << (bits - 4);
        // put x[low 12] into place, sign extended
        long l = ((long) x << (64 - 12)) >> (64 - 12);
        // move l upwards, maybe
        l <<= (x >> 16);
        return h ^ l;
    }

    private int checkXY(byte x, byte y, int xlen, int step, int n, int trace, String fmt) {
        if (trace > 0 && (step % trace) == 0) {
            TTY.println(String.format("step %d, n=%08x: value=" + fmt + " (len=%d)", step, n, x, xlen));
        }
        if (x != y) {
            TTY.println(String.format("step %d, n=%d: " + fmt + " != " + fmt, step, n, x, y));
            return 1;
        }
        return 0;
    }

    public void testCompressedStream(int trace) {
        CompressedWriteStream bytes = new CompressedWriteStream(STRETCHLIMIT * 100);
        int n;
        int step = 0;
        int fails = 0;

        for (n = 0; n < (1 << 8); n++) {
            byte x = (byte) n;
            bytes.writeByte(x);
            ++step;
        }
        for (n = 0; n < STRETCHLIMIT; n++) {
            int x = (int) stretch(n, 32);
            bytes.writeInt(x);
            ++step;
            bytes.writeSignedInt(x);
            ++step;
            bytes.writeFloat(x);
            ++step;
        }
        for (n = 0; n < STRETCHLIMIT; n++) {
            long x = stretch(n, 64);
            bytes.writeLong(x);
            ++step;
            bytes.writeDouble(x);
            ++step;
        }
        int length = bytes.position();
        if (trace != 0) {
            TTY.println(String.format("set up test of %d stream values, size %d", step, length));
        }
        step = 0;
        // now decode it all
        CompressedReadStream decode = new CompressedReadStream(bytes.buffer());
        int pos;
        int lastpos = decode.position();
        for (n = 0; n < (1 << 8); n++) {
            byte x = (byte) n;
            byte y = decode.readByte();
            step++;
            pos = decode.position;
            fails += checkXY(x, y, pos - lastpos, step, n, trace, "%db");
            lastpos = pos;
        }
        for (n = 0; n < STRETCHLIMIT; n++) {
            int x = (int) stretch(n, 32);
            int y1 = decode.readInt();
            step++;
            pos = decode.position;
            fails += checkXY((byte) x, (byte) y1, pos - lastpos, step, n, trace, "%du");
            lastpos = pos;
            int y2 = decode.readSignedInt();
            step++;
            pos = decode.position;
            fails += checkXY((byte) x, (byte) y2, pos - lastpos, step, n, trace, "%di");
            lastpos = pos;
            int y3 = (int) decode.readFloat();
            step++;
            pos = decode.position;
            fails += checkXY((byte) x, (byte) y3, pos - lastpos, step, n, trace, "%df");
            lastpos = pos;
        }
        for (n = 0; n < STRETCHLIMIT; n++) {
            long x = stretch(n, 64);
            long y1 = decode.readLong();
            step++;
            pos = decode.position;
            fails += checkXY((byte) x, (byte) y1, pos - lastpos, step, n, trace, "%l");
            lastpos = pos;
            long y2 = (long) decode.readDouble();
            step++;
            pos = decode.position;
            fails += checkXY((byte) x, (byte) y2, pos - lastpos, step, n, trace, "%d");
            lastpos = pos;
        }
        int length2 = decode.position();
        if (trace != 0) {
            TTY.println(String.format("finished test of %d stream values, size %d", step, length2));
        }
        if (length != length2) {
            throw new Error("bad length");
        }

        if (fails != 0) {
            throw new Error("test failures");
        }
    }
}

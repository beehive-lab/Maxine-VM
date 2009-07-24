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

/**
 * The <code>CompressedReadStream</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class CompressedReadStream extends CompressedStream {

    private byte read() {
        return buffer[position++];
    }

    private int readIntMb(int value) {
        int pos = position() - 1;
        // char buf = buffer() + pos;
        assert (buffer[pos] == value) && (value >= Encoding.L.value) : "correctly called";
        int sum = value;
        // must collect more bytes: b[1]...b[4]
        int lgHI = Encoding.LgH.value;

        int i = 0;
        while (true) {
            int bI = buffer[pos + ++i]; // bI = read(); ++i;
            sum += bI << lgHI; // sum += b[i](64i)
            if (bI < Encoding.L.value || i == Encoding.MAXI.value) {
                setPosition(pos + i + 1);
                return sum;
            }
            lgHI += Encoding.LgH.value;
        }
    } // UNSIGNED5 coding, 2-5 byte cases

    public CompressedReadStream(byte[] buffer) {
        this(buffer, 0);
    }

    public CompressedReadStream(byte[] buffer, int position) {
        super(buffer, position);
    }

    public boolean readBool() {
        return (read() != 0) ? true : false;
    }

    public byte readByte() {
        return read();
    }

    public char readChar() {
        return (char) readInt();
    }

    public short readShort() {
        return (short) readSignedInt();
    }

    public int readInt() {
        int b0 = read();
        if (b0 < Encoding.L.value) {
            return b0;
        } else {
            return readIntMb(b0);
        }
    }

    public int readSignedInt() {
        return decodeSign(readInt());
    }

    public float readFloat() {
        int rf = readInt();
        int f = reverseInt(rf);
        return f;
    }

    public double readDouble() {
        int rh = readInt();
        int rl = readInt();
        int h = reverseInt(rh);
        int l = reverseInt(rl);
        return h << 32 | (l & (0xffffffff << 32));
    }

    public long readLong() {
        int low = readSignedInt();
        int high = readSignedInt();
        return high << 32 | (low & (0xffffffff << 32));
    }
}

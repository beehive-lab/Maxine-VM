/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights
 * reserved.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.
 *
 * Parts of the product may be derived from Berkeley BSD systems, licensed from the University of California. UNIX is a
 * registered trademark in the U.S. and in other countries, exclusively licensed through X/Open Company, Ltd.
 *
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the
 * U.S. and other countries.
 *
 * This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in
 * other countries. Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether
 * direct or indirect, are strictly prohibited. Export or reexport to countries subject to U.S. embargo or to entities
 * identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 */
/**
 * Copyright (c) 2005, Regents of the University of California All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created Sep 5, 2005
 */
package com.sun.max.elf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * @author Ben L. Titzer
 */
public class ELFDataInputStream {

    private boolean _bigEndian;
    private ELFHeader _header;
    private RandomAccessFile _file;
    private ByteBuffer _buffer;

    public ELFDataInputStream(ELFHeader elfHeader, RandomAccessFile f) {
        this._header = elfHeader;
        _bigEndian = elfHeader.isBigEndian();
        _file = f;
    }

    public ELFDataInputStream(ELFHeader elfHeader, ByteBuffer buffer) {
        this._header = elfHeader;
        _buffer = buffer;
    }

    public byte read_Elf32_byte() throws IOException {
        return (byte) read_1();
    }

    public int read_Elf32_uchar() throws IOException {
        return read_1();
    }

    public int read_Elf32_Addr() throws IOException {
        return read_4();
    }

    public short read_Elf32_Half() throws IOException {
        return (short) read_2();
    }

    public int read_Elf32_Off() throws IOException {
        return read_4();
    }

    public int read_Elf32_SWord() throws IOException {
        return read_4();
    }

    public int read_Elf32_Word() throws IOException {
        return read_4();
    }

    public byte read_Elf64_byte() throws IOException {
        return (byte) read_1();
    }

    public int read_Elf64_uchar() throws IOException {
        return read_1();
    }

    public long read_Elf64_Addr() throws IOException {
        return read_8();
    }

    public short read_Elf64_Half() throws IOException {
        return (short) read_2();
    }

    public long read_Elf64_Off() throws IOException {
        return read_8();
    }

    public int read_Elf64_SWord() throws IOException {
        return read_4();
    }

    public int read_Elf64_Word() throws IOException {
        return read_4();
    }

    public long read_Elf64_XWord() throws IOException {
        return read_8();
    }

    private int read_1() throws IOException {
        if (_file == null) {
            return _buffer.get() & 0xff;
        } else {
            return _file.read() & 0xff;
        }
    }

    private int read_2() throws IOException {
        final int b1 = read_1();
        final int b2 = read_1();
        if (_bigEndian) {
            return asShort(b2, b1);
        }
        return asShort(b1, b2);
    }

    private int read_4() throws IOException {
        final int b1 = read_1();
        final int b2 = read_1();
        final int b3 = read_1();
        final int b4 = read_1();
        if (_bigEndian) {
            return asInt(b4, b3, b2, b1);
        }
        return asInt(b1, b2, b3, b4);
    }

    private long read_8() throws IOException {
        final int b1 = read_1();
        final int b2 = read_1();
        final int b3 = read_1();
        final int b4 = read_1();
        final int b5 = read_1();
        final int b6 = read_1();
        final int b7 = read_1();
        final int b8 = read_1();
        if (_bigEndian) {
            return asLong(b8, b7, b6, b5, b4, b3, b2, b1);
        }
        return asLong(b1, b2, b3, b4, b5, b6, b7, b8);
    }

    private short asShort(int bl, int bh) {
        return (short) ((bh << 8) | bl);
    }

    private int asInt(int b1, int b2, int b3, int b4) {
        return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
    }

    private long asLong(int b1, int b2, int b3, int b4, int b5, int b6, int b7, int b8) {
        final long lw = (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        final long hw = (b8 << 24) | (b7 << 16) | (b6 << 8) | b5;
        return hw << 32 | (lw & 0xffffffffL);
    }

}

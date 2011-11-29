/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.elf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Supports reading of ELF values from an ELF file or a {@link ByteBuffer} (presumably read from an ELF file).
 */
public class ELFDataInputStream {

    private boolean bigEndian;
    private RandomAccessFile file;
    private ByteBuffer buffer;

    /**
     * Setup to read from an ELF file, that is assumed to be positioned at the point reading should start.
     * @param elfHeader
     * @param f
     */
    public ELFDataInputStream(ELFHeader elfHeader, RandomAccessFile f) {
        bigEndian = elfHeader.isBigEndian();
        file = f;
    }

    /**
     * Setup to read from (and ELF file in) a {@link ByteBuffer}, positioned so that {@link ByteBuffer#get} gets the first byte to be accessed.
     * @param elfHeader
     * @param buffer
     */
    public ELFDataInputStream(ELFHeader elfHeader, ByteBuffer buffer) {
        bigEndian = elfHeader.isBigEndian();
        this.buffer = buffer;
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
        if (file == null) {
            return buffer.get() & 0xff;
        } else {
            return file.read() & 0xff;
        }
    }

    private int read_2() throws IOException {
        final int b1 = read_1();
        final int b2 = read_1();
        if (bigEndian) {
            return asShort(b2, b1);
        }
        return asShort(b1, b2);
    }

    private int read_4() throws IOException {
        final int b1 = read_1();
        final int b2 = read_1();
        final int b3 = read_1();
        final int b4 = read_1();
        if (bigEndian) {
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
        if (bigEndian) {
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

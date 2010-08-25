/*
 * Copyright (c) 2009 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Parts of the product may be derived from Berkeley BSD systems,
 * licensed from the University of California. UNIX is a registered
 * trademark in the U.S.  and in other countries, exclusively licensed
 * through X/Open Company, Ltd.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws and
 * may be subject to the export or import laws in other
 * countries. Nuclear, missile, chemical biological weapons or nuclear
 * maritime end uses or end users, whether direct or indirect, are
 * strictly prohibited. Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion lists,
 * including, but not limited to, the denied persons and specially
 * designated nationals lists is strictly prohibited.
 * 
 */
/**
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created Jun 11, 2008
 */
package com.sun.max.elf;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author Ben L. Titzer
 */
public class ELFDataOutputStream {

    final boolean _bigEndian;
    final ELFHeader _header;
    final RandomAccessFile _file;

    public ELFDataOutputStream(ELFHeader elfHeader, RandomAccessFile f) {
        this._header = elfHeader;
        _bigEndian = elfHeader.isBigEndian();
        _file = f;
    }

    public byte[] write_section(int off, int length) throws IOException {
        final byte[] buffer = new byte[length];
        _file.seek(off);
        int cntr = 0;
        while (cntr < length) {
 //           cntr += _file.write(buffer, cntr, length - cntr);
        }
        return buffer;
    }
/*
    public void write_Elf32_byte(byte b) throws IOException {
        write_1(b);
    }

    public void write_Elf32_uchar(int i) throws IOException {
        write_1(i);
    }

    public void write_Elf32_Addr(int i) throws IOException {
        write_4(i);
    }


    public void write_Elf32_Half(short s) throws IOException {
        write_2(s);
    }

    public void write_Elf32_Off(int i) throws IOException {
        write_4(i);
    }

    public void write_Elf32_SWord(int i) throws IOException {
        write_4(i);
    }

    public void write_Elf32_Word(int i) throws IOException {
        write_4(i);
    }
*/
    public void write_Elf64_byte(byte b) throws IOException {
        write_1(b);
    }

    public void write_Elf64_uchar(int i) throws IOException {
        write_1(i);
    }

    public void write_Elf64_Addr(long l) throws IOException {
        write_8(l);
    }

    public void write_Elf64_Half(short s) throws IOException {
        write_2(s);
    }

    public void write_Elf64_Off(long l) throws IOException {
        write_8(l);
    }

    public void write_Elf64_SWord(int i) throws IOException {
        write_4(i);
    }



    public void write_Elf64_Word(int i) throws IOException {
        write_4(i);

    }

    public void write_Elf64_XWord(long l) throws IOException {
        write_8(l);
    }


    public void write_1(int i) throws IOException {
        _file.write(i);
    }

    private short [] shortToByte(short s) {
        short srtArray[] = new short[2];
        int  s2 = s & 0xff;
        int s1 = (s >> 8) & 0xff;
        if (_bigEndian) {
            srtArray[0] = (short) s1;
            srtArray[1] = (short)s2;
        }
        else {
            srtArray[0] = (short) s2;
            srtArray[1] = (short)s1;
        }
        return srtArray;
    }

    private short[] longToByte(long l, short srtArray[]) {
        short srtArray1[] = new short[2];

        int longToInt[] = new int[4];
        int i = 0;
        longToInt[0] = (int)l & 0x0000ffff;
        longToInt[1] = (int) (l >> 16) & 0x0000ffff;
        longToInt[2] =(int) (l >> 32) & 0x0000ffff;
        longToInt[3]= (int) (l >> 48) & 0x0000ffff;
        for(i = 0; i<4; i++) {
            srtArray1 = shortToByte((short) longToInt[i]);
            srtArray[(i*2)] = srtArray1[0];
            srtArray[(i*2)+ 1] = srtArray1[1];
        }
        return srtArray;
    }

    private short[] intToByte(int i, short srtArray[]) {
        short srtArray1[] = new short[2];
        int splitInt[]=new int[2];
        int j = 0;
        splitInt[0] =(short) i & 0xff;
        splitInt[1] = (short) (i >> 16) & 0xff;
        for(j = 0; j<2; j++) {
            srtArray1 = shortToByte((short) splitInt[j]);
            srtArray[(j*2)] = srtArray1[0];
            srtArray[(j*2)+ 1] = srtArray1[1];
        }
        return srtArray;
    }

    private void write_2(short s) throws IOException {
        short srtArray[] = new short[2];
        srtArray =shortToByte(s);
        write_1(srtArray[0]);
        write_1(srtArray[1]);
    }


    private void write_4(int i) throws IOException {
        short srtArray[] = new short[4];
        srtArray = intToByte(i, srtArray);
        if (_bigEndian) {
            for( i = 3; i >= 0; i--) {
                write_1(srtArray[i]);
            }
        }
        else {
            for(i = 0; i<4; i++) {
                write_1(srtArray[i]);
            }
        }

    }



    private void write_8(long l) throws IOException {

        int i = 0;
        short srtArray[] = new short[8];
        srtArray = longToByte(l,srtArray);
        if (_bigEndian) {
            for( i = 7; i >= 0; i--) {
                write_1(srtArray[i]);
            }
        }
        else {
            for(i = 0; i<8; i++) {
                write_1(srtArray[i]);
            }
        }

    }

    /*
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

    */

}

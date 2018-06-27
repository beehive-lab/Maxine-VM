/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.elf;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * The <code>ELFNoteGNUbuildID</code> class represents the .note.gnu.build-id section of an ELF file.
 */
public class ELFNoteGNUbuildID {

    private final long offset;
    private final long size;

    public ELFNoteGNUbuildID(ELFSectionHeaderTable.Entry noteGNUbuildIDentry) {
        offset = noteGNUbuildIDentry.getOffset();
        size = noteGNUbuildIDentry.getSize();
    }

    public String getBuildID(RandomAccessFile raf, ELFHeader header) throws IOException {
        raf.seek(offset);
        final ELFDataInputStream dis = new ELFDataInputStream(header, raf);
        long readLength = 0;
        int namesz = dis.read_Elf64_Word();
        final int descsz = dis.read_Elf64_Word();
        final int type = dis.read_Elf64_Word();
        final String name = readNoteString(dis, namesz);
        readLength += 12 + namesz;
        final byte[] desc = readNoteDesc(dis, descsz);
        readLength += descsz;
        assert readLength == size : "readLength = " + readLength + " size = " + size;

        return DatatypeConverter.printHexBinary(desc).toLowerCase();
    }

    /**
     * Read a string from a NOTE entry with length length. The returned string is of size length - 1 as java strings are
     * not null terminated
     *
     * @param length
     */
    public String readNoteString(ELFDataInputStream dis, int length) throws IOException {
        byte[] arr = new byte[length - 1];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dis.read_Elf64_byte();
        }
        dis.read_Elf64_byte();
        return new String(arr);
    }

    public byte[] readNoteDesc(ELFDataInputStream dis, int length) throws IOException {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = dis.read_Elf64_byte();
        }
        return arr;
    }

}

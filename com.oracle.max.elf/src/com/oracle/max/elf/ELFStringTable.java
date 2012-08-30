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
 * Created Sep 5, 2005
 */
package com.oracle.max.elf;

import java.io.*;
import java.util.*;

/**
 * The <code>ELFStringTable</code> class represents a string table that is
 * present in the ELF file. A string table section contains a sequence of
 * null-terminated strings and can be indexed by integers to obtain the
 * string corresponding to the sequence of characters starting at the
 * specified index up to (and not including) the next null character.
 */
public class ELFStringTable {

    protected final Map<Integer, String> stringMap;
    protected byte[] data;
    protected char [] stringTable;
    protected int position;

    protected ELFSectionHeaderTable.Entry sectionEntry;

    /**
     * The constructor for the <code>ELFStringTable</code> class creates a new
     * string table with the specified number of bytes reserved for its internal
     * string storage.
     * @param header the ELF header of the file
     * @param sectionEntry the section header table entry corresponding to this string
     * table
     */
    public ELFStringTable(ELFHeader header, ELFSectionHeaderTable.Entry sectionEntry) {
        data = new byte[(int) sectionEntry.getSize()];
        stringMap = new HashMap<Integer, String>();
        this.sectionEntry = sectionEntry;
        stringTable = new char[50];
        stringTable[0] = '\0';
        position = 1;
    }

    public ELFStringTable(ELFHeader header) {
        stringMap = new HashMap<Integer, String>();
        stringTable = new char[50];
        stringTable[0] = '\0';
        position = 1;
    }

    public void setSection(ELFSectionHeaderTable.Entry sectionEntry) {
        data = new byte[(int) sectionEntry.getSize()];
        this.sectionEntry = sectionEntry;
    }

    /**
     * The <code>read()</code> method reads this string table from the specified
     * file, beginning at the specified offset and continuing for the length
     * of the section.
     * @param f the random access file to read the data from
     * @throws IOException if there is a problem reading the file
     */
    public void read(RandomAccessFile f) throws IOException {
        if (data.length == 0) {
            return;
        }
        f.seek(sectionEntry.getOffset());
        int read = 0;
        while (read < data.length) {
            read += f.read(data, read, data.length - read);
        }
    }

    /**
     * The <code>getString()</code> method gets a string in this section corresponding
     * to the specified index. Since Java strings are not null-terminated as the
     * ones in this section are, this method employs a hash table to remember frequently
     * accessed strings.
     * @param ind the index into the section for which to retrieve the string
     * @return a string corresponding to reading bytes at the specified index up to
     * and not including the next null character
     */
    public String getString(int ind) {
        if (ind >= data.length) {
            return "";
        }
        String str = stringMap.get(ind);
        if (str == null) {
            final StringBuffer buf = new StringBuffer();
            for (int pos = ind; pos < data.length; pos++) {
                final byte b = data[pos];
                if (b == 0) {
                    break;
                }
                buf.append((char) b);
            }
            str = buf.toString();
            stringMap.put(ind, str);
        }
        return str;
    }
    public void addStringInTable(String str) {
        int addSize = str.length();
        int index = 0;
        char [] addString = str.toCharArray();
        while (index < addSize) {
            stringTable[position++] = addString[index++];
        }
        stringTable[position++] = '\0';

    }

    public int getIndex(String str) {
        int index = -1;
        int strSize = str.length();
        int strCount = 0;
        int count = 0;
        if (str.equalsIgnoreCase("\0")) {
            return 0;
        }
        while (count < position && index == -1) {
            if (str.charAt(strCount) == stringTable[count]) {
                while (count <= position && strCount < strSize &&
                                str.charAt(strCount) == stringTable[count]) {

                    if (index == -1) {
                        if (stringTable[count - 1] != '\0') {
                            count++;
                            break;
                        } else {
                            index = count;
                        }
                    }
                    count++;
                    strCount++;

                }
                if (strCount < strSize - 1 || stringTable[count] != '\0') {
                    index = -1;
                    strCount = 0;
                }
            } else {
                count++;
            }
        }
        return index;
    }

    public int getStringLength() {
        return position;
    }

    public void write64ToFile(ELFDataOutputStream os, RandomAccessFile fis) throws IOException {
        for (int count = 0; count < position; count++) {
            fis.write(stringTable[count]);
        }

    }

}

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
package com.sun.max.vm.classfile;

import static com.sun.max.vm.classfile.ErrorContext.*;

import java.io.*;
import java.util.*;

import com.sun.max.vm.bytecode.graft.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * Encapsulates the information specified in one or more
 * <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#22856">LineNumberTable</a>
 * class file attributes.
 */
public final class LineNumberTable {

    public static final class Entry {

        private final char bci;
        private final char lineNumber;

        public int bci() {
            return bci;
        }

        public int lineNumber() {
            return lineNumber;
        }

        public Entry(char bci, char lineNumber) {
            this.bci = bci;
            this.lineNumber = lineNumber;
        }
    }

    public static final LineNumberTable EMPTY = new LineNumberTable(new Entry[0]);

    private final char[] encodedEntries;

    public Entry[] entries() {
        final Entry[] entries = new Entry[encodedEntries.length >> 1];
        int encodedIndex = 0;
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry(encodedEntries[encodedIndex++], encodedEntries[encodedIndex++]);
        }
        return entries;
    }

    public LineNumberTable relocate(OpcodeBCIRelocator relocator) {
        if (encodedEntries.length == 0) {
            return this;
        }
        final char[] relocEncodedEntries = new char[encodedEntries.length];
        for (int i = 0; i < relocEncodedEntries.length; i += 2) {
            relocEncodedEntries[i] = (char) relocator.relocate(encodedEntries[i]);
            relocEncodedEntries[i + 1] = encodedEntries[i + 1];
        }
        return new LineNumberTable(relocEncodedEntries);
    }

    public boolean isEmpty() {
        return encodedEntries.length == 0;
    }

    LineNumberTable(char[] encodedEntries) {
        this.encodedEntries = encodedEntries;
    }

    public LineNumberTable(LineNumberTable prefix, ClassfileStream classfileStream, int codeLength) {
        final int length = classfileStream.readUnsigned2();
        int encodedIndex;
        if (prefix.encodedEntries.length == 0) {
            encodedEntries = new char[length * 2];
            encodedIndex = 0;
        } else {
            encodedIndex = prefix.encodedEntries.length;
            encodedEntries = Arrays.copyOf(prefix.encodedEntries, encodedIndex + (length * 2));
        }
        for (int i = 0; i != length; ++i) {
            final char bci = (char) classfileStream.readUnsigned2();
            final char lineNumber = (char) classfileStream.readUnsigned2();
            if (bci >= codeLength) {
                throw classFormatError("Invalid address in LineNumberTable entry " + i);
            }
            encodedEntries[encodedIndex++] = bci;
            encodedEntries[encodedIndex++] = lineNumber;
        }
    }

    public LineNumberTable(Entry[] entries) {
        encodedEntries = new char[entries.length * 2];
        int encodedIndex = 0;
        for (Entry entry : entries) {
            encodedEntries[encodedIndex++] = entry.bci;
            encodedEntries[encodedIndex++] = entry.lineNumber;
        }
    }

    /**
     * Gets the source line number corresponding to a given BCI.
     *
     * @param bci
     * @return -1 if this line number table does not containing a source line mapping for {@code bci}
     */
    public int findLineNumber(int bci) {
        int lineNumber = -1;
        if (encodedEntries.length != 0) {
            int index = 0;
            while (index != encodedEntries.length) {
                final int e = encodedEntries[index++];
                if (e > bci) {
                    break;
                }
                lineNumber = encodedEntries[index++];
            }
        }
        return lineNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(encodedEntries.length * 10);
        sb.append('[');
        for (int i = 0; i != encodedEntries.length; i += 2) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append((int) encodedEntries[i]).append(':').append((int) encodedEntries[i + 1]);
        }
        return sb.append(']').toString();
    }

    public void encode(DataOutputStream dataOutputStream) throws IOException {
        CodeAttribute.writeCharArray(dataOutputStream, encodedEntries);
    }

    public static LineNumberTable decode(DataInputStream dataInputStream) throws IOException {
        return new LineNumberTable(CodeAttribute.readCharArray(dataInputStream));
    }

    /**
     * Writes this line number table to a given stream as a LineNumberTable class file attribute.
     *
     * @param stream
     *                a data output stream that has just written the 'attribute_name_index' and 'attribute_length'
     *                fields of a class file attribute
     * @param constantPoolEditor
     */
    public void writeAttributeInfo(DataOutputStream stream, ConstantPoolEditor constantPoolEditor) throws IOException {
        stream.writeShort(encodedEntries.length >> 1);
        for (char c : encodedEntries) {
            stream.writeShort(c);
        }
    }
}

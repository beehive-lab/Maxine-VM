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
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class LineNumberTable {

    public static final class Entry {

        private final char position;
        private final char lineNumber;

        public int position() {
            return position;
        }

        public int lineNumber() {
            return lineNumber;
        }

        public Entry(char position, char lineNumber) {
            this.position = position;
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

    public LineNumberTable relocate(OpcodePositionRelocator relocator) {
        if (encodedEntries.length == 0) {
            return this;
        }
        final char[] relocEncodedEntries = new char[this.encodedEntries.length];
        for (int i = 0; i < relocEncodedEntries.length; i += 2) {
            relocEncodedEntries[i] = (char) relocator.relocate(relocEncodedEntries[i]);
            relocEncodedEntries[i + 1] = relocEncodedEntries[i + 1];
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
            final char position = (char) classfileStream.readUnsigned2();
            final char lineNumber = (char) classfileStream.readUnsigned2();
            if (position >= codeLength) {
                throw classFormatError("Invalid address in LineNumberTable entry " + i);
            }
            encodedEntries[encodedIndex++] = position;
            encodedEntries[encodedIndex++] = lineNumber;
        }
    }

    public LineNumberTable(Entry[] entries) {
        encodedEntries = new char[entries.length * 2];
        int encodedIndex = 0;
        for (Entry entry : entries) {
            encodedEntries[encodedIndex++] = entry.position;
            encodedEntries[encodedIndex++] = entry.lineNumber;
        }
    }

    /**
     * Gets the source line number corresponding to a given bytecode position.
     *
     * @param bytecodePosition
     * @return -1 if this line number table does not containing a source line mapping for {@code bytecodePosition}
     */
    public int findLineNumber(int bytecodePosition) {
        int lineNumber = -1;
        if (encodedEntries.length != 0) {
            int index = 0;
            while (index != encodedEntries.length) {
                final int position = encodedEntries[index++];
                if (position > bytecodePosition) {
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

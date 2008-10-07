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
/*VCSID=5c4bc80a-1e34-48b0-9f80-67b1713c8c50*/
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

        private final char _position;
        private final char _lineNumber;

        public int position() {
            return _position;
        }

        public int lineNumber() {
            return _lineNumber;
        }

        public Entry(char position, char lineNumber) {
            _position = position;
            _lineNumber = lineNumber;
        }
    }

    public static final LineNumberTable EMPTY = new LineNumberTable(new Entry[0]);

    private final char[] _encodedEntries;

    public Entry[] entries() {
        final Entry[] entries = new Entry[_encodedEntries.length / 2];
        int encodedIndex = 0;
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry(_encodedEntries[encodedIndex++], _encodedEntries[encodedIndex++]);
        }
        return entries;
    }

    public LineNumberTable relocate(OpcodePositionRelocator relocator) {
        if (_encodedEntries.length == 0) {
            return this;
        }
        final char[] encodedEntries = new char[_encodedEntries.length];
        for (int i = 0; i < _encodedEntries.length; i += 2) {
            encodedEntries[i] = (char) relocator.relocate(_encodedEntries[i]);
            encodedEntries[i + 1] = _encodedEntries[i + 1];
        }
        return new LineNumberTable(encodedEntries);
    }

    public boolean isEmpty() {
        return _encodedEntries.length == 0;
    }

    LineNumberTable(char[] encodedEntries) {
        _encodedEntries = encodedEntries;
    }

    public LineNumberTable(LineNumberTable prefix, ClassfileStream classfileStream, int codeLength) {
        final int length = classfileStream.readUnsigned2();
        int encodedIndex;
        if (prefix._encodedEntries.length == 0) {
            _encodedEntries = new char[length * 2];
            encodedIndex = 0;
        } else {
            encodedIndex = prefix._encodedEntries.length;
            _encodedEntries = Arrays.copyOf(prefix._encodedEntries, encodedIndex + (length * 2));
        }
        for (int i = 0; i != length; ++i) {
            final char position = (char) classfileStream.readUnsigned2();
            final char lineNumber = (char) classfileStream.readUnsigned2();
            if (position >= codeLength) {
                throw classFormatError("Invalid address in LineNumberTable entry " + i);
            }
            _encodedEntries[encodedIndex++] = position;
            _encodedEntries[encodedIndex++] = lineNumber;
        }
    }

    public LineNumberTable(Entry[] entries) {
        _encodedEntries = new char[entries.length * 2];
        int encodedIndex = 0;
        for (Entry entry : entries) {
            _encodedEntries[encodedIndex++] = entry._position;
            _encodedEntries[encodedIndex++] = entry._lineNumber;
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
        if (_encodedEntries.length != 0) {
            int index = 0;
            while (index != _encodedEntries.length) {
                final int position = _encodedEntries[index++];
                if (position > bytecodePosition) {
                    break;
                }
                lineNumber = _encodedEntries[index++];
            }
        }
        return lineNumber;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(_encodedEntries.length * 10);
        sb.append('[');
        for (int i = 0; i != _encodedEntries.length; i += 2) {
            if (sb.length() != 1) {
                sb.append(", ");
            }
            sb.append((int) _encodedEntries[i]).append(':').append((int) _encodedEntries[i + 1]);
        }
        return sb.append(']').toString();
    }

    public void encode(DataOutputStream dataOutputStream) throws IOException {
        CodeAttribute.writeCharArray(dataOutputStream, _encodedEntries);
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
        stream.writeShort(_encodedEntries.length / 2);
        for (char c : _encodedEntries) {
            stream.writeShort(c);
        }
    }
}

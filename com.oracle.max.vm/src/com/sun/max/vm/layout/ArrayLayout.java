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
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 */
public interface ArrayLayout extends SpecificLayout {

    int readLength(Accessor accessor);

    void writeLength(Accessor accessor, int length);

    int arrayLengthOffset();

    /**
     * Gets the size of the cell required to hold an array of a given kind and length.
     * The return value accounts for the object header size as well as any padding at the end of the cell
     * to ensure that the cell size is word aligned.
     *
     * @param kind the kind of the elements in the array
     * @param length the length of an array
     */
    Size getArraySize(Kind kind, int length);

    int headerSize();

    /**
     * Gets the header fields of this array header layout.
     *
     * @return an array of header field descriptors sorted by ascending order of the field addresses in memory
     */
    HeaderField[] headerFields();

    Kind elementKind();

    /**
     * Gets the size of the cell required to hold an array of a given length described by this layout object.
     * The return value accounts for the object header size as well as any padding at the end of the cell
     * to ensure that the cell size is word aligned.
     *
     * @param length the length of an array
     */
    Size getArraySize(int length);

    Offset getElementOffsetFromOrigin(int index);

    Offset getElementOffsetInCell(int index);

    /**
     * Copies elements from an array described by this layout.
     *
     * @param src an accessor to an array described by this layout
     * @param srcIndex starting index in {@code src}
     * @param dst the array into which the values are copied
     * @param dstIndex the starting index in {@code dst}
     * @param length the number of elements to copy
     */
    void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length);

    boolean getBoolean(Accessor accessor, int index);
    void setBoolean(Accessor accessor, int index, boolean value);

    byte getByte(Accessor accessor, int index);
    void setByte(Accessor accessor, int index, byte value);

    char getChar(Accessor accessor, int index);
    void setChar(Accessor accessor, int index, char value);

    short getShort(Accessor accessor, int index);
    void setShort(Accessor accessor, int index, short value);

    int getInt(Accessor accessor, int index);
    void setInt(Accessor accessor, int index, int value);

    float getFloat(Accessor accessor, int index);
    void setFloat(Accessor accessor, int index, float value);

    long getLong(Accessor accessor, int index);
    void setLong(Accessor accessor, int index, long value);

    double getDouble(Accessor accessor, int index);
    void setDouble(Accessor accessor, int index, double value);

    Reference getReference(Accessor accessor, int index);
    void setReference(Accessor accessor, int index, Reference value);

    Word getWord(Accessor accessor, int index);
    void setWord(Accessor accessor, int index, Word value);
}

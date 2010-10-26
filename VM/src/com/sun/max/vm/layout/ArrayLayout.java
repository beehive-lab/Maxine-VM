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
package com.sun.max.vm.layout;

import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
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

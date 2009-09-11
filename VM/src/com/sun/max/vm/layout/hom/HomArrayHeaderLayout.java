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
package com.sun.max.vm.layout.hom;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class HomArrayHeaderLayout extends HomGeneralLayout implements ArrayHeaderLayout {

    /**
     * The offset of the first array element.
     */
    final int elementsOffset;

    protected final int headerSize;

    HomArrayHeaderLayout(GripScheme gripScheme) {
        super(gripScheme);
        elementsOffset = 0;
        headerSize = -arrayLengthOffset;
    }

    @INLINE
    public final int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return new HeaderField[] {HeaderField.LENGTH, HeaderField.HUB, HeaderField.MISC};
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        return Size.fromInt(kind.width.numberOfBytes).times(length).plus(headerSize).wordAligned();
    }

    @Override
    @INLINE
    public final Pointer cellToOrigin(Pointer cell) {
        return cell.plus(headerSize);
    }

    @Override
    @INLINE
    public final Pointer originToCell(Pointer origin) {
        return origin.minus(headerSize);
    }

    @Override
    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Kind getElementKind(Accessor accessor) {
        final ArrayClassActor arrayClassActor = UnsafeLoophole.cast(readHubReference(accessor).toJava());
        return arrayClassActor.componentClassActor().kind;
    }

    @Override
    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return Offset.fromInt(arrayLengthOffset);
        }
        return super.getOffsetFromOrigin(headerField);
    }

    @INLINE
    static final Word lengthToWord(int length) {
        return Address.fromInt(length).shiftedLeft(1).or(1);
    }

    @INLINE
    static int wordToLength(Word word) {
        return word.asAddress().unsignedShiftedRight(1).toInt();
    }

    public int arrayLengthOffset() {
        return arrayLengthOffset;
    }

    @INLINE
    public final int readLength(Accessor accessor) {
        return wordToLength(accessor.readWord(arrayLengthOffset));
    }

    @INLINE
    public final void writeLength(Accessor accessor, int length) {
        accessor.writeWord(arrayLengthOffset, lengthToWord(length));
    }

}

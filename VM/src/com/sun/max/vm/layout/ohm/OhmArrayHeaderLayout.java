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
package com.sun.max.vm.layout.ohm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class OhmArrayHeaderLayout extends OhmGeneralLayout implements ArrayHeaderLayout {

    /**
     * The cell offset of the word in the header containing the array length.
     */
    protected final int lengthOffset;

    private final int headerSize;

    @INLINE
    public final int headerSize() {
        return headerSize;
    }

    public HeaderField[] headerFields() {
        return new HeaderField[] {HeaderField.HUB, HeaderField.MISC, HeaderField.LENGTH};
    }

    OhmArrayHeaderLayout() {
        lengthOffset = miscOffset + Word.size();
        headerSize = lengthOffset + Word.size();
    }

    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        return Size.fromInt(kind.width.numberOfBytes).times(length).plus(headerSize).wordAligned();
    }

    @INLINE
    public final Kind getElementKind(Accessor accessor) {
        final ArrayClassActor arrayClassActor = UnsafeCast.asArrayClassActor(readHubReference(accessor).toJava());
        return arrayClassActor.componentClassActor().kind;
    }

    @Override
    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return Offset.fromInt(lengthOffset);
        }
        return super.getOffsetFromOrigin(headerField);
    }

    public int arrayLengthOffset() {
        return lengthOffset;
    }

    @INLINE
    public final int readLength(Accessor accessor) {
        return accessor.readInt(lengthOffset);
    }

    @INLINE
    public final void writeLength(Accessor accessor, int length) {
        accessor.writeInt(lengthOffset, length);
    }

}

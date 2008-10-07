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
/*VCSID=1ca47819-9356-4523-8a9f-12e5f5b6211d*/
package com.sun.max.vm.layout.ohm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class OhmArrayHeaderLayout extends OhmGeneralLayout implements ArrayHeaderLayout {

    /**
     * The cell offset of the word in the header containing the array length.
     */
    protected final int _lengthOffset;

    /**
     * The cell offset of the first element in the array.
     */
    protected final int _elementsOffset;

    private final int _headerSize;

    @INLINE
    public final int headerSize() {
        return _headerSize;
    }

    OhmArrayHeaderLayout(GripScheme gripScheme) {
        super(gripScheme);
        _lengthOffset = _miscOffset + Word.size();
        _elementsOffset = _lengthOffset + Word.size();
        _headerSize = _elementsOffset;
    }

    @Override
    public boolean isArrayLayout() {
        return true;
    }

    @INLINE
    public final Size getArraySize(Kind kind, int length) {
        return Size.fromInt(kind.size()).times(length).plus(_headerSize).aligned();
    }

    @INLINE
    public final Kind getElementKind(Accessor accessor) {
        final ArrayClassActor arrayClassActor = UnsafeLoophole.cast(readHubReference(accessor).toJava());
        return arrayClassActor.componentClassActor().kind();
    }

    @Override
    public Offset getOffsetFromOrigin(HeaderField headerField) {
        if (headerField == HeaderField.LENGTH) {
            return Offset.fromInt(_lengthOffset);
        }
        return super.getOffsetFromOrigin(headerField);
    }

    @INLINE
    public final int readLength(Accessor accessor) {
        return accessor.readInt(_lengthOffset);
    }

    @INLINE
    public final void writeLength(Accessor accessor, int length) {
        accessor.writeInt(_lengthOffset, length);
    }

}

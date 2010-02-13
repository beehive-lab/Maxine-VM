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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class HomArrayLayout<Value_Type extends Value<Value_Type>> extends HomArrayHeaderLayout implements ArrayLayout<Value_Type> {

    protected final Kind<Value_Type> elementKind;

    public HomArrayLayout(GripScheme gripScheme, Kind<Value_Type> elementKind) {
        super(gripScheme);
        this.elementKind = elementKind;
    }

    @INLINE
    public final Kind<Value_Type> elementKind() {
        return elementKind;
    }

    public Layout.Category category() {
        return Layout.Category.ARRAY;
    }

    @Override
    public final boolean isReferenceArrayLayout() {
        final Kind rawKind = elementKind;
        return rawKind == Kind.REFERENCE;
    }

    @INLINE
    public final int elementSize() {
        return elementKind().width.numberOfBytes;
    }

    @INLINE
    protected final int originDisplacement() {
        return 0;
    }

    @INLINE
    public final Offset getElementOffsetFromOrigin(int index) {
        // Converting to 'Offset' before multiplication to avoid overflow:
        return Offset.fromInt(index).times(elementSize());
    }

    @INLINE
    public final Offset getElementOffsetInCell(int index) {
        return getElementOffsetFromOrigin(index).plus(headerSize);
    }

    @INLINE
    public final Size getArraySize(int length) {
        return getElementOffsetInCell(length).aligned().asSize();
    }

    @INLINE
    public final Size specificSize(Accessor accessor) {
        return getArraySize(readLength(accessor));
    }

    @HOSTED_ONLY
    @Override
    public void visitHeader(ObjectCellVisitor visitor, Object array) {
        super.visitHeader(visitor, array);
        final int origin = headerSize;
        visitor.visitHeaderField(origin + arrayLengthOffset, "length", JavaTypeDescriptor.WORD, new WordValue(lengthToWord(HostObjectAccess.getArrayLength(array))));
    }

    @HOSTED_ONLY
    private void visitElements(ObjectCellVisitor visitor, Object array) {
        final int length = Array.getLength(array);
        final Hub hub = HostObjectAccess.readHub(array);
        final Kind elementKind = hub.classActor.componentClassActor().kind;
        if (elementKind == Kind.REFERENCE) {
            for (int i = 0; i < length; i++) {
                final Object object = Array.get(array, i);
                visitor.visitElement(getElementOffsetInCell(i).toInt(), i, ReferenceValue.from(object));
            }
        } else {
            for (int i = 0; i < length; i++) {
                final Object boxedJavaValue = Array.get(array, i);
                final Value value = elementKind.asValue(boxedJavaValue);
                visitor.visitElement(getElementOffsetInCell(i).toInt(), i, value);
            }
        }
    }

    @HOSTED_ONLY
    public void visitObjectCell(Object array, ObjectCellVisitor visitor) {
        visitHeader(visitor, array);
        visitElements(visitor, array);
    }

    public int getHubReferenceOffsetInCell() {
        final int origin = headerSize;
        return origin + hubOffset;
    }

    @HOSTED_ONLY
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        assert kind.isPrimitiveOfSameSizeAs(elementKind) : "kind: " + kind + ", elementKind: " + elementKind;
        assert offset % kind.width.numberOfBytes == 0;
        final int index = offset / kind.width.numberOfBytes;
        return mirror.readElement(kind, index);
    }

    @HOSTED_ONLY
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        assert kind.isPrimitiveOfSameSizeAs(value.kind());
        assert offset % elementSize() == 0;
        assert kind.isPrimitiveOfSameSizeAs(elementKind);
        final int index = offset / elementSize();
        mirror.writeElement(elementKind, index, value);
    }

    public void copyElements(Accessor src, int srcIndex, Object dst, int dstIndex, int length) {
        src.copyElements(originDisplacement(), srcIndex, dst, dstIndex, length);
    }
}

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
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class OhmHybridLayout extends OhmWordArrayLayout implements HybridLayout {

    @Override
    public Layout.Category category() {
        return Layout.Category.HYBRID;
    }

    @Override
    public boolean isHybridLayout() {
        return true;
    }

    @Override
    public boolean isArrayLayout() {
        return false;
    }

    private final OhmTupleLayout _tupleLayout;
    private final IntArrayLayout _intArrayLayout;


    OhmHybridLayout(GripScheme gripScheme) {
        super(gripScheme);
        _tupleLayout = new OhmTupleLayout(gripScheme);
        _intArrayLayout = new OhmIntArrayLayout(gripScheme);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        final Size tupleSize = _tupleLayout.layoutFields(superClassActor, fieldActors, headerSize());
        return getArraySize(firstAvailableWordArrayIndex(tupleSize));
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return _tupleLayout.getFieldOffsetInCell(fieldActor);
    }

    public int firstAvailableWordArrayIndex(Size tupleSize) {
        return tupleSize.minus(headerSize()).roundedUpBy(Word.size()).dividedBy(Word.size()).toInt();
    }

    @Override
    public void visitObjectCell(Object object, ObjectCellVisitor visitor) {
        final Hybrid hybrid = (Hybrid) object;
        visitHeader(visitor, hybrid);
        _tupleLayout.visitFields(visitor, hybrid);

        for (int wordIndex = hybrid.firstWordIndex(); wordIndex <= hybrid.lastWordIndex(); wordIndex++) {
            visitor.visitElement(getElementOffsetInCell(wordIndex).toInt(), wordIndex, new WordValue(hybrid.getWord(wordIndex)));
        }

        for (int intIndex = hybrid.firstIntIndex(); intIndex <= hybrid.lastIntIndex(); intIndex++) {
            visitor.visitElement(_intArrayLayout.getElementOffsetInCell(intIndex).toInt(), intIndex, IntValue.from(hybrid.getInt(intIndex)));
        }
    }

    @Override
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        final int index = (offset - headerSize()) / kind.size();
        if ((kind == Kind.INT && index >= mirror.firstIntIndex()) ||
            (kind == Kind.WORD && index >= mirror.firstWordIndex())) {
            return mirror.readElement(kind, index);
        }
        return _tupleLayout.readValue(kind, mirror, offset);
    }

    @Override
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        final int index = (offset - headerSize()) / Word.size();
        if ((kind == Kind.INT && index >= mirror.firstIntIndex()) ||
            (kind == Kind.WORD && index >= mirror.firstWordIndex())) {
            super.writeValue(kind, mirror, offset, value);
        } else {
            _tupleLayout.writeValue(kind, mirror, offset, value);
        }
    }
}

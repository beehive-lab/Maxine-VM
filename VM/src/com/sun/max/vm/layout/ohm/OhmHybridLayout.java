/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.layout.ohm;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class OhmHybridLayout extends OhmArrayLayout implements HybridLayout {

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

    private final OhmTupleLayout tupleLayout;
    private final OhmArrayLayout intArrayLayout;

    OhmHybridLayout() {
        super(Kind.WORD);
        tupleLayout = new OhmTupleLayout();
        intArrayLayout = new OhmArrayLayout(Kind.INT);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        final Size tupleSize = tupleLayout.layoutFields(superClassActor, fieldActors, headerSize());
        return getArraySize(firstAvailableWordArrayIndex(tupleSize));
    }

    @INLINE
    public int getFieldOffsetInCell(FieldActor fieldActor) {
        return tupleLayout.getFieldOffsetInCell(fieldActor);
    }

    public int firstAvailableWordArrayIndex(Size tupleSize) {
        return tupleSize.minus(headerSize()).roundedUpBy(Word.size()).dividedBy(Word.size()).toInt();
    }

    @HOSTED_ONLY
    @Override
    public void visitObjectCell(Object object, ObjectCellVisitor visitor) {
        final Hybrid hybrid = (Hybrid) object;
        visitHeader(visitor, hybrid);
        tupleLayout.visitFields(visitor, hybrid);

        for (int wordIndex = hybrid.firstWordIndex(); wordIndex <= hybrid.lastWordIndex(); wordIndex++) {
            visitor.visitElement(getElementOffsetInCell(wordIndex).toInt(), wordIndex, new WordValue(hybrid.getWord(wordIndex)));
        }

        for (int intIndex = hybrid.firstIntIndex(); intIndex <= hybrid.lastIntIndex(); intIndex++) {
            visitor.visitElement(intArrayLayout.getElementOffsetInCell(intIndex).toInt(), intIndex, IntValue.from(hybrid.getInt(intIndex)));
        }
    }

    @HOSTED_ONLY
    @Override
    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        final Value value = readHeaderValue(mirror, offset);
        if (value != null) {
            return value;
        }
        final int index = (offset - headerSize()) / kind.width.numberOfBytes;
        if ((kind == Kind.INT && index >= mirror.firstIntIndex()) ||
            (kind.isWord && index >= mirror.firstWordIndex())) {
            return mirror.readElement(kind, index);
        }
        return tupleLayout.readValue(kind, mirror, offset);
    }

    @HOSTED_ONLY
    @Override
    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        if (writeHeaderValue(mirror, offset, value)) {
            return;
        }
        final int index = (offset - headerSize()) / Word.size();
        if ((kind == Kind.INT && index >= mirror.firstIntIndex()) ||
            (kind.isWord && index >= mirror.firstWordIndex())) {
            super.writeValue(kind, mirror, offset, value);
        } else {
            tupleLayout.writeValue(kind, mirror, offset, value);
        }
    }
}

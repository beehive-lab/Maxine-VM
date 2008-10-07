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
package com.sun.max.vm.layout.prototype;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class PrototypeTupleLayout extends PrototypeGeneralLayout implements TupleLayout {

    public Layout.Category category() {
        return Layout.Category.TUPLE;
    }

    @Override
    public boolean isTupleLayout() {
        return true;
    }

    public Size specificSize(Accessor accessor) {
        throw ProgramError.unexpected();
    }

    public PrototypeTupleLayout(GripScheme gripScheme) {
        super(gripScheme);
    }

    public int headerSize() {
        throw ProgramError.unexpected();
    }

    public int getFieldOffsetInCell(FieldActor fieldActor) {
        throw ProgramError.unexpected();
    }

    @Override
    public TupleClassActor readReferenceClassActor(Accessor accessor) {
        assert isTuple(accessor);
        return (TupleClassActor) super.readReferenceClassActor(accessor);
    }

    public Size layoutFields(ClassActor superClassActor, FieldActor[] fieldActors) {
        int offset = (superClassActor == null) ? 0 : superClassActor.dynamicTupleSize().toInt();
        for (FieldActor fieldActor : fieldActors) {
            fieldActor.setOffset(offset);
            offset += fieldActor.kind().size();
        }
        return Size.fromInt(offset);
    }

    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        throw ProgramError.unexpected();
    }

    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        assert kind == value.kind();
        ProgramError.unexpected();
    }
}

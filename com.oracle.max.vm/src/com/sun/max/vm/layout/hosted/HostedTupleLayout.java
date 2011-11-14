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
package com.sun.max.vm.layout.hosted;

import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.layout.Layout.HeaderField;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 */
public class HostedTupleLayout extends HostedGeneralLayout implements TupleLayout {

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

    public int headerSize() {
        throw ProgramError.unexpected();
    }

    public HeaderField[] headerFields() {
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
            offset += fieldActor.kind.width.numberOfBytes;
        }
        return Size.fromInt(offset);
    }

    public Value readValue(Kind kind, ObjectMirror mirror, int offset) {
        throw ProgramError.unexpected();
    }

    public void writeValue(Kind kind, ObjectMirror mirror, int offset, Value value) {
        assert kind == value.kind();
        throw ProgramError.unexpected();
    }
}

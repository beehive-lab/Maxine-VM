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
package com.sun.max.vm.reference.hosted;

import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.SpecificLayout.ObjectMirror;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public class HostedObjectMirror implements ObjectMirror {
    private final Object object;
    private final ClassActor classActor;

    public HostedObjectMirror(final Object object) {
        this.object = object;
        classActor = ClassActor.fromJava(object.getClass());
    }

    public boolean isArray() {
        return classActor.isArrayClass();
    }

    public ClassActor classActor() {
        return classActor;
    }

    public Value readHub() {
        return ReferenceValue.from(classActor.dynamicHub());
    }

    public Value readElement(Kind kind, int index) {
        if (object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) object;
            switch (kind.asEnum) {
                case INT:
                    return IntValue.from(hybrid.getInt(index));
                case WORD:
                    return new WordValue(hybrid.getWord(index));
                default:
                    ProgramError.unknownCase();
                    return null;
            }
        }
        final Object javaValue = Array.get(object, index);
        final ArrayClassActor arrayClassActor = (ArrayClassActor) classActor;
        ProgramError.check(arrayClassActor.componentClassActor().kind.stackKind == kind.stackKind);
        return arrayClassActor.componentClassActor().kind.asValue(javaValue);
    }

    public Value readField(int offset) {
        final FieldActor fieldActor = classActor.findInstanceFieldActor(offset);
        final Field field = fieldActor.toJava();
        field.setAccessible(true);
        try {
            return fieldActor.kind.asValue(field.get(object));
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not read field: " + field, throwable);
        }
    }

    public Value readMisc() {
        return new WordValue(vmConfig().monitorScheme().createMisc(object));
    }

    public int readArrayLength() {
        return ArrayAccess.readArrayLength(object);
    }

    public void writeArrayLength(Value value) {
        assert value.asInt() == ArrayAccess.readArrayLength(object);
    }

    public void writeHub(Value value) {
        assert value == ReferenceValue.from(classActor);
    }

    public void writeElement(Kind kind, int index, Value value) {
        if (object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) object;
            switch (kind.asEnum) {
                case INT:
                    hybrid.setInt(index, value.asInt());
                    break;
                case WORD:
                    hybrid.setWord(index, value.asWord());
                    break;
                default:
                    ProgramError.unknownCase();
                    break;
            }
        } else {
            final ArrayClassActor arrayClassActor = (ArrayClassActor) classActor;
            final ClassActor componentClassActor = arrayClassActor.componentClassActor();
            if (componentClassActor.kind.stackKind != kind.stackKind) {
                throw new ArrayStoreException("cannot store a '" + kind + "' into an array of '" + componentClassActor.kind + "'");
            }
            if (object instanceof Object[]) {
                final Object[] objectArray = (Object[]) object;
                objectArray[index] = value.asObject();
            } else {
                final Object javaBoxedValue = componentClassActor.kind.convert(value).asBoxedJavaValue();
                Array.set(object, index, javaBoxedValue);
            }
        }
    }

    public void writeField(int offset, Value value) {
        final TupleClassActor tupleClassActor = (TupleClassActor) classActor;
        final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
        final Field field = fieldActor.toJava();
        field.setAccessible(true);
        final TypeDescriptor fieldDescriptor = fieldActor.descriptor();
        try {
            if (fieldDescriptor.toKind().isWord) {
                final Class<Class<? extends Word>> type = null;
                final Word word = value.toWord().as(Utils.cast(type, field.getType()));
                field.set(object, word);
            } else {
                field.set(object, fieldActor.kind.convert(value).asBoxedJavaValue());
            }
        } catch (IllegalArgumentException e) {
            throw ProgramError.unexpected(e);
        } catch (IllegalAccessException e) {
            throw ProgramError.unexpected(e);
        }
    }

    public void writeMisc(Value value) {
        assert value.asWord().equals(vmConfig().monitorScheme().createMisc(object));
    }

    public int firstWordIndex() {
        if (classActor.isHybridClass()) {
            final Hybrid hybrid = (Hybrid) object;
            return hybrid.firstWordIndex();
        }
        return Integer.MAX_VALUE;
    }

    public int firstIntIndex() {
        if (classActor.isHybridClass()) {
            final Hybrid hybrid = (Hybrid) object;
            return hybrid.firstIntIndex();
        }
        return Integer.MAX_VALUE;
    }
}

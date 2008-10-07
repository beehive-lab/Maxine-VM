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
package com.sun.max.vm.interpreter;

import java.lang.reflect.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.layout.SpecificLayout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Doug Simon
 * @author Bernd Mathiske
 */
class InterpreterObjectMirror implements ObjectMirror {
    private final Object _object;
    private final ClassActor _classActor;

    public InterpreterObjectMirror(final Object object) {
        _object = object;
        _classActor = MaxineVM.usingTarget(new Function<ClassActor>() {
            public ClassActor call() {
                return ClassActor.fromJava(object.getClass());
            }
        });
    }

    public boolean isArray() {
        return _classActor.isArrayClassActor();
    }

    public ClassActor classActor() {
        return _classActor;
    }

    public Value readHub() {
        return ReferenceValue.from(_classActor.dynamicHub());
    }

    public Value readElement(Kind kind, int index) {
        if (_object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) _object;
            switch (kind.asEnum()) {
                case INT:
                    return IntValue.from(hybrid.getInt(index));
                case WORD:
                    return new WordValue(hybrid.getWord(index));
                default:
                    ProgramError.unknownCase();
                    return null;
            }
        }
        final Object javaValue = Array.get(_object, index);
        final ArrayClassActor arrayClassActor = (ArrayClassActor) _classActor;
        ProgramError.check(arrayClassActor.componentClassActor().kind().toStackKind() == kind.toStackKind());
        return arrayClassActor.componentClassActor().kind().asValue(javaValue);
    }


    public Value readField(int offset) {
        final FieldActor fieldActor = _classActor.findInstanceFieldActor(offset);
        final Field field = fieldActor.toJava();
        field.setAccessible(true);
        try {
            return fieldActor.kind().asValue(field.get(_object));
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not read field: " + field, throwable);
        }
    }


    public Value readMisc() {
        return new WordValue(VMConfiguration.target().monitorScheme().createMisc(_object));
    }


    public Value readArrayLength() {
        return IntValue.from(HostObjectAccess.getArrayLength(_object));
    }


    public void writeArrayLength(Value value) {
        assert value.asInt() == HostObjectAccess.getArrayLength(_object);
    }


    public void writeHub(Value value) {
        assert value == ReferenceValue.from(_classActor);
    }


    public void writeElement(Kind kind, int index, Value value) {
        if (_object instanceof Hybrid) {
            final Hybrid hybrid = (Hybrid) _object;
            switch (kind.asEnum()) {
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
            final ArrayClassActor arrayClassActor = (ArrayClassActor) _classActor;
            final ClassActor componentClassActor = arrayClassActor.componentClassActor();
            if (componentClassActor.kind().toStackKind() != kind.toStackKind()) {
                throw new ArrayStoreException("cannot store a " + kind + " to an array of " + componentClassActor.kind());
            }
            final Object javaBoxedValue = componentClassActor.kind().convert(value).asBoxedJavaValue();
            Array.set(_object, index, javaBoxedValue);
        }
    }


    public void writeField(int offset, Value value) {
        final TupleClassActor tupleClassActor = (TupleClassActor) _classActor;
        final FieldActor fieldActor = tupleClassActor.findInstanceFieldActor(offset);
        final Field field = fieldActor.toJava();
        field.setAccessible(true);
        try {
            final TypeDescriptor fieldDescriptor = fieldActor.descriptor();
            if (KindTypeDescriptor.isWord(fieldDescriptor)) {
                final Class<Class<? extends Word>> type = null;
                final Word word = value.toWord().as(StaticLoophole.cast(type, field.getType()));
                field.set(_object, word);
            } else {
                field.set(_object, fieldActor.kind().convert(value).asBoxedJavaValue());
            }
        } catch (Throwable throwable) {
            ProgramError.unexpected("could not set field: " + field, throwable);
        }
    }

    public void writeMisc(Value value) {
        assert value.asWord().equals(VMConfiguration.target().monitorScheme().createMisc(_object));
    }

    public int firstWordIndex() {
        if (_classActor.isHybridClassActor()) {
            final Hybrid hybrid = (Hybrid) _object;
            return hybrid.firstWordIndex();
        }
        return Integer.MAX_VALUE;
    }

    public int firstIntIndex() {
        if (_classActor.isHybridClassActor()) {
            final Hybrid hybrid = (Hybrid) _object;
            return hybrid.firstIntIndex();
        }
        return Integer.MAX_VALUE;
    }
}

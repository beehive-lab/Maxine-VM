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
package com.sun.max.vm.compiler.ir.interpreter;

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
public class InterpreterObjectMirror implements ObjectMirror {
    private final Object object;
    private final ClassActor classActor;

    public InterpreterObjectMirror(final Object object) {
        this.object = object;
        classActor = MaxineVM.usingTarget(new Function<ClassActor>() {
            public ClassActor call() {
                return ClassActor.fromJava(object.getClass());
            }
        });
    }

    public boolean isArray() {
        return classActor.isArrayClassActor();
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
        ProgramError.check(arrayClassActor.componentClassActor().kind.toStackKind() == kind.toStackKind());
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
        return new WordValue(VMConfiguration.target().monitorScheme().createMisc(object));
    }


    public int readArrayLength() {
        return HostObjectAccess.getArrayLength(object);
    }


    public void writeArrayLength(Value value) {
        assert value.asInt() == HostObjectAccess.getArrayLength(object);
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
            if (componentClassActor.kind.toStackKind() != kind.toStackKind()) {
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
            if (KindTypeDescriptor.isWord(fieldDescriptor)) {
                final Class<Class<? extends Word>> type = null;
                final Word word = value.toWord().as(StaticLoophole.cast(type, field.getType()));
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
        assert value.asWord().equals(VMConfiguration.target().monitorScheme().createMisc(object));
    }

    public int firstWordIndex() {
        if (classActor.isHybridClassActor()) {
            final Hybrid hybrid = (Hybrid) object;
            return hybrid.firstWordIndex();
        }
        return Integer.MAX_VALUE;
    }

    public int firstIntIndex() {
        if (classActor.isHybridClassActor()) {
            final Hybrid hybrid = (Hybrid) object;
            return hybrid.firstIntIndex();
        }
        return Integer.MAX_VALUE;
    }
}

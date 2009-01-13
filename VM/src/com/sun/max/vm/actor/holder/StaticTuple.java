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
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.value.*;

/**
 * Instances of this class represent static tuples during prototyping.
 * The boot image generator substitutes the former by the latter.
 * Thus the boot image does not contain any of the former.
 *
 * @author Bernd Mathiske
 */
public final class StaticTuple {

    private final ClassActor _classActor;

    public ClassActor classActor() {
        return _classActor;
    }

    private StaticTuple(ClassActor classActor) {
        _classActor = classActor;
    }

    public static Object create(ClassActor classActor) {
        if (MaxineVM.isPrototyping()) {
            return new StaticTuple(classActor);
        }
        final Object staticTuple = Heap.createTuple(classActor.staticHub());
        for (FieldActor fieldActor : classActor.localStaticFieldActors()) {
            final Value constantValue = fieldActor.constantValue();
            if (constantValue != null) {
                fieldActor.writeValue(staticTuple, constantValue);
            }
        }
        return staticTuple;
    }

    public static Object fromJava(Class javaClass) {
        return ClassActor.fromJava(javaClass).staticTuple();
    }

    public FieldActor findStaticFieldActor(int offset) {
        return _classActor.findStaticFieldActor(offset);
    }

    @Override
    public String toString() {
        return "staticTuple-" + _classActor.simpleName();
    }

    /**
     * A map for recording <i>overriding</i> values for static fields. If a static field has an overriding value, then this
     * overriding value is placed in the boot image instead of the value of the field obtained via reflection.
     */
    private Map<FieldActor, Value> _fieldActorToValue;

    @PROTOTYPE_ONLY
    public Value getValue(FieldActor fieldActor) {
        if (_fieldActorToValue != null) {
            return _fieldActorToValue.get(fieldActor);
        }
        return null;
    }

    @PROTOTYPE_ONLY
    public void setField(String fieldName, Object boxedJavaValue) {
        final FieldActor fieldActor = _classActor.findLocalStaticFieldActor(fieldName);
        ProgramError.check(fieldActor != null);
        if (_fieldActorToValue == null) {
            _fieldActorToValue = new LinkedIdentityHashMap<FieldActor, Value>();
        }
        final Value value = Value.fromBoxedJavaValue(boxedJavaValue);
        ProgramError.check(fieldActor.kind() == value.kind());
        _fieldActorToValue.put(fieldActor, value);
    }

    /**
     * Reset the static field to its zero value if it exists.
     * @param fieldName the name of the field
     * @param mustExist whether it is a program error if the field does not exist
     */
    @PROTOTYPE_ONLY
    public void resetField(String fieldName, boolean mustExist) {
        final FieldActor fieldActor = _classActor.findLocalStaticFieldActor(fieldName);
        if (fieldActor == null) {
            ProgramError.check(!mustExist, "static field not found: " + fieldName);
        } else {
            if (_fieldActorToValue == null) {
                _fieldActorToValue = new LinkedIdentityHashMap<FieldActor, Value>();
            }
            _fieldActorToValue.put(fieldActor, fieldActor.kind().zeroValue());
        }
    }

    public static boolean is(Object object) {
        if (MaxineVM.isPrototyping()) {
            return object instanceof StaticTuple;
        }
        return ObjectAccess.readHub(object) instanceof StaticHub;
    }

    public static String toString(Object staticTuple) {
        if (MaxineVM.isPrototyping()) {
            final StaticTuple s = (StaticTuple) staticTuple;
            return s.toString();
        }
        return "staticTuple-" + ObjectAccess.readHub(staticTuple).classActor().simpleName();
    }
}

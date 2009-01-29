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

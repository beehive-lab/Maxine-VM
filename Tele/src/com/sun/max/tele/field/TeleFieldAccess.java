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
package com.sun.max.tele.field;

import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public abstract class TeleFieldAccess {

    private static FieldActor findFieldActor(Class holder, String name) {
        final ClassActor classActor = HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER.mustMakeClassActor(JavaTypeDescriptor.forJavaClass(holder));
        final FieldActor fieldActor = classActor.findFieldActor(SymbolTable.makeSymbol(name));
        TeleError.check(fieldActor != null, "could not find field: " + name + " in class: " + holder);
        return fieldActor;
    }

    private final FieldActor fieldActor;

    public FieldActor fieldActor() {
        return fieldActor;
    }

    protected TeleFieldAccess(Class holder, String name, Kind kind) {
        fieldActor = findFieldActor(holder, name);
        TeleError.check(fieldActor.kind == kind, "field has wrong kind: " + name + " in class: " + holder);
    }

    @Override
    public String toString() {
        return fieldActor.toString();
    }
}

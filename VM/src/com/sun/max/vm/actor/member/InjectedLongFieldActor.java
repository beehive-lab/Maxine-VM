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
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Definition of {@code long} fields injected into JDK classes.
 *
 * @author Doug Simon
 */
public class InjectedLongFieldActor extends LongFieldActor implements InjectedFieldActor<LongValue> {

    public TypeDescriptor holderTypeDescriptor() {
        return _holder;
    }

    public LongValue readInjectedValue(Reference reference) {
        return LongValue.ZERO;
    }

    private final TypeDescriptor _holder;

    /**
     * Creates an actor for an injected long field.
     *
     * @param holder
     *                the class into which the field is injected
     * @param name
     *                the name used to derive a synthetic name for the field
     */
    @PROTOTYPE_ONLY
    public InjectedLongFieldActor(Class holder, String name) {
        super(SymbolTable.makeSymbol("_$injected$" + name),
              ACC_SYNTHETIC + ACC_PRIVATE + INJECTED);
        _holder = JavaTypeDescriptor.forJavaClass(holder);
        Static.registerInjectedFieldActor(this);
    }

}

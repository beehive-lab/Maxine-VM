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
package com.sun.max.vm.classfile.constant;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class ResolvedMethodRefConstant<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> extends AbstractPoolConstant<PoolConstant_Type> implements MethodRefConstant<PoolConstant_Type> {

    @INSPECTED
    private final MethodActor _methodActor;

    public final MethodActor methodActor() {
        return _methodActor;
    }

    public ResolvedMethodRefConstant(MethodActor methodActor) {
        _methodActor = methodActor;
    }

    public final Utf8Constant name(ConstantPool pool) {
        return name();
    }

    public final TypeDescriptor holder(ConstantPool pool) {
        return holder();
    }

    public final Descriptor descriptor(ConstantPool pool) {
        return signature();
    }

    public boolean isResolved() {
        return true;
    }

    public final boolean isResolvableWithoutClassLoading(ConstantPool pool) {
        return true;
    }

    public final MethodActor resolve(ConstantPool pool, int index) {
        return _methodActor;
    }

    static StaticMethodActor verifyIsStatic(MethodActor methodActor, ConstantPool pool) throws IncompatibleClassChangeError {
        try {
            return (StaticMethodActor) methodActor;
        } catch (ClassCastException e) {
            throw new IncompatibleClassChangeError(methodActor + " is not a static method");
        }
    }

    static VirtualMethodActor verifyIsVirtual(MethodActor methodActor, ConstantPool pool) throws IncompatibleClassChangeError {
        try {
            return (VirtualMethodActor) methodActor;
        } catch (ClassCastException e) {
            throw new IncompatibleClassChangeError(methodActor + " is a static method");
        }
    }

    public final SignatureDescriptor signature(ConstantPool pool) {
        return signature();
    }

    public final TypeDescriptor holder() {
        return _methodActor.holder().typeDescriptor();
    }

    public final Utf8Constant name() {
        return _methodActor.name();
    }

    public final SignatureDescriptor signature() {
        return _methodActor.descriptor();
    }

    public final String valueString(ConstantPool pool) {
        return _methodActor.format("%H.%n(%p)");
    }
}

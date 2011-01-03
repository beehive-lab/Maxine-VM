/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
    private final MethodActor methodActor;

    public final MethodActor methodActor() {
        return methodActor;
    }

    public ResolvedMethodRefConstant(MethodActor methodActor) {
        this.methodActor = methodActor;
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
        return methodActor;
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
        return methodActor.holder().typeDescriptor;
    }

    public final Utf8Constant name() {
        return methodActor.name;
    }

    public final SignatureDescriptor signature() {
        return methodActor.descriptor();
    }

    public final String valueString(ConstantPool pool) {
        return methodActor.format("%H.%n(%p):%r");
    }
}

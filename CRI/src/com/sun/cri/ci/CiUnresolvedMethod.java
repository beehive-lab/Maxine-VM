/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ci;

import com.sun.cri.ri.*;

/**
 * A implementation of {@link RiMethod} for an unresolved method.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class CiUnresolvedMethod implements RiMethod {

    public final String name;
    public final RiType holder;
    public final RiSignature signature;

    public CiUnresolvedMethod(RiType holder, String name, RiSignature signature) {
        this.name = name;
        this.holder = holder;
        this.signature = signature;
    }

    public String name() {
        return name;
    }

    public String jniSymbol() {
        throw unresolved("jniSymbol()");
    }

    public RiType holder() {
        return holder;
    }

    public RiSignature signature() {
        return signature;
    }

    public byte[] code() {
        throw unresolved("code()");
    }

    public int maxLocals() {
        throw unresolved("maxLocals()");
    }

    public int maxStackSize() {
        throw unresolved("maxStackSize()");
    }

    public boolean hasBalancedMonitors() {
        throw unresolved("hasBalancedMonitors()");
    }

    public boolean isResolved() {
        return false;
    }

    public int accessFlags() {
        throw unresolved("maxStackSize()");
    }

    public boolean isLeafMethod() {
        throw unresolved("maxStackSize()");
    }

    public boolean isClassInitializer() {
        throw unresolved("maxStackSize()");
    }

    public boolean isConstructor() {
        throw unresolved("maxStackSize()");
    }

    public boolean isOverridden() {
        throw unresolved("maxStackSize()");
    }

    public RiMethodProfile methodData() {
        throw unresolved("maxStackSize()");
    }

    public Object liveness(int bci) {
        throw unresolved("maxStackSize()");
    }

    public boolean canBeStaticallyBound() {
        throw unresolved("maxStackSize()");
    }

    public RiExceptionHandler[] exceptionHandlers() {
        throw unresolved("maxStackSize()");
    }

    private CiUnresolvedException unresolved(String operation) {
        throw new CiUnresolvedException(operation + " not defined for unresolved method " + CiUtil.format("%H.%n(%p)", this, false));
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public String toString() {
        return CiUtil.format("%H.%n(%p) [unresolved]", this, false);
    }
}

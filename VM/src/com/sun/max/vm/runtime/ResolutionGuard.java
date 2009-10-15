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
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.classfile.constant.*;

/**
 * A token that "guards" the {@linkplain PoolConstant#callWithResolutionAndClassInitialization(ConstantPool, int)
 * resolution} of a constant pool entry to an {@link Actor}.
 *
 * This pattern of use is intended:
 *
 * if (guard.value == null) { guard.value = ... } return guard.value;
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class ResolutionGuard {

    public final ConstantPool constantPool;

    public final int constantPoolIndex;

    @CONSTANT_WHEN_NOT_ZERO
    public Actor value;

    public ResolutionGuard(ConstantPool constantPool, int constantPoolIndex) {
        this.constantPool = constantPool;
        this.constantPoolIndex = constantPoolIndex;
        assert constantPoolIndex >= 0 : "must be a valid constant pool index!";
    }

    /**
     * Gets the pool constant whose resolution is guarded by this object.
     */
    public PoolConstant poolConstant() {
        return constantPool.at(constantPoolIndex);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + poolConstant().valueString(constantPool) + "]";
    }
}

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
package com.sun.max.vm.cps.eir.allocate.some.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.allocate.some.*;
import com.sun.max.vm.cps.eir.amd64.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirSomeAllocator extends EirSomeAllocator<AMD64EirRegister> {

    private final PoolSet<AMD64EirRegister> noRegisters = PoolSet.noneOf(AMD64EirRegister.pool());

    @Override
    protected PoolSet<AMD64EirRegister> noRegisters() {
        return noRegisters;
    }

    private final PoolSet<AMD64EirRegister> allocatableIntegerRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableIntegerRegisters() {
        return allocatableIntegerRegisters;
    }

    private final PoolSet<AMD64EirRegister> allocatableFloatingPointRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableFloatingPointRegisters() {
        return allocatableFloatingPointRegisters;
    }

    public AMD64EirSomeAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final AMD64EirABI abi = (AMD64EirABI) methodGeneration.abi;

        allocatableIntegerRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        allocatableIntegerRegisters.and(AMD64EirRegister.General.poolSet());
        allocatableIntegerRegisters.and(abi.allocatableRegisters());

        allocatableFloatingPointRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        allocatableFloatingPointRegisters.and(AMD64EirRegister.XMM.poolSet());
        allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }
}

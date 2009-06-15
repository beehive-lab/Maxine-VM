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
package com.sun.max.vm.compiler.eir.allocate.linearscan.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.eir.amd64.*;

/**
 * @author Thomas Wuerthinger
 */
public final class AMD64EirLinearScanAllocator extends LinearScanRegisterAllocator<AMD64EirRegister> {

    private final PoolSet<AMD64EirRegister> _noRegisters = PoolSet.noneOf(AMD64EirRegister.pool());

    @Override
    protected PoolSet<AMD64EirRegister> noRegisters() {
        return _noRegisters;
    }

    private final PoolSet<AMD64EirRegister> _allocatableIntegerRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableIntegerRegisters() {
        return _allocatableIntegerRegisters;
    }

    private final PoolSet<AMD64EirRegister> _allocatableFloatingPointRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableFloatingPointRegisters() {
        return _allocatableFloatingPointRegisters;
    }

    public AMD64EirLinearScanAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final AMD64EirABI abi = (AMD64EirABI) methodGeneration.abi();

        _allocatableIntegerRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        _allocatableIntegerRegisters.and(AMD64EirRegister.General.poolSet());
        _allocatableIntegerRegisters.and(abi.allocatableRegisters());

        _allocatableFloatingPointRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        _allocatableFloatingPointRegisters.and(AMD64EirRegister.XMM.poolSet());
        _allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }

}

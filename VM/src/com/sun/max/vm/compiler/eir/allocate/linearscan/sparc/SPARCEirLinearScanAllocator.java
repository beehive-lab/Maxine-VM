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
package com.sun.max.vm.compiler.eir.allocate.linearscan.sparc;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.dir.eir.sparc.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.eir.sparc.SPARCEirRegister.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Thomas Wuerthinger
 */
public final class SPARCEirLinearScanAllocator extends LinearScanRegisterAllocator<SPARCEirRegister> {

    private final PoolSet<SPARCEirRegister> _noRegisters = PoolSet.noneOf(SPARCEirRegister.pool());

    @Override
    protected PoolSet<SPARCEirRegister> noRegisters() {
        return _noRegisters;
    }

    private final PoolSet<SPARCEirRegister> _allocatableIntegerRegisters;

    @Override
    protected PoolSet<SPARCEirRegister> allocatableIntegerRegisters() {
        return _allocatableIntegerRegisters;
    }

    private final PoolSet<SPARCEirRegister> _allocatableFloatingPointRegisters;

    @Override
    protected PoolSet<SPARCEirRegister> allocatableFloatingPointRegisters() {
        return _allocatableFloatingPointRegisters;
    }

    public SPARCEirLinearScanAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final SPARCEirABI abi = (SPARCEirABI) methodGeneration.abi();

        _allocatableIntegerRegisters = PoolSet.allOf(SPARCEirRegister.pool());
        _allocatableIntegerRegisters.and(SPARCEirRegister.GeneralPurpose.poolSet());
        _allocatableIntegerRegisters.and(abi.allocatableRegisters());
        if (((DirToSPARCEirMethodTranslation) methodGeneration).saveSafetpoinLatchInLocal()) {
            // Reserve L5
            _allocatableIntegerRegisters.remove(DirToSPARCEirMethodTranslation.SAVED_SAFEPOINT_LATCH_LOCAL);
        }

        _allocatableFloatingPointRegisters = PoolSet.allOf(SPARCEirRegister.pool());
        _allocatableFloatingPointRegisters.and(SPARCEirRegister.FloatingPoint.singlePrecisionPoolSet());
        _allocatableFloatingPointRegisters.and(SPARCEirRegister.FloatingPoint.doublePrecisionPoolSet());
        _allocatableFloatingPointRegisters.and(SPARCEirRegister.FloatingPoint.poolSet());
        _allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }
}

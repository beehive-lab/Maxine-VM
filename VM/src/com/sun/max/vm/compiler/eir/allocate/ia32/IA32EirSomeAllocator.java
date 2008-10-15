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
package com.sun.max.vm.compiler.eir.allocate.ia32;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.eir.ia32.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirSomeAllocator extends EirSomeAllocator<IA32EirRegister> {

    private final PoolSet<IA32EirRegister> _noRegisters = PoolSet.noneOf(IA32EirRegister.pool());

    @Override
    protected PoolSet<IA32EirRegister> noRegisters() {
        return _noRegisters;
    }

    private final PoolSet<IA32EirRegister> _allocatableIntegerRegisters;

    @Override
    protected PoolSet<IA32EirRegister> allocatableIntegerRegisters() {
        return _allocatableIntegerRegisters;
    }

    private final PoolSet<IA32EirRegister> _allocatableFloatingPointRegisters;

    @Override
    protected PoolSet<IA32EirRegister> allocatableFloatingPointRegisters() {
        return _allocatableFloatingPointRegisters;
    }

    public IA32EirSomeAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final IA32EirABI abi = (IA32EirABI) methodGeneration.abi();

        _allocatableIntegerRegisters = PoolSet.noneOf(IA32EirRegister.pool());
        _allocatableIntegerRegisters.addAll();
        _allocatableIntegerRegisters.and(IA32EirRegister.General.poolSet());
        _allocatableIntegerRegisters.and(abi.allocatableRegisters());

        _allocatableFloatingPointRegisters = PoolSet.noneOf(IA32EirRegister.pool());
        _allocatableFloatingPointRegisters.addAll();
        _allocatableFloatingPointRegisters.and(IA32EirRegister.XMM.poolSet());
        _allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }

    @Override
    protected EirLocationCategory decideConstantLocationCategory(Value value, EirOperand operand) {
        if (value.kind() != Kind.REFERENCE || value.isZero()) {
            final WordWidth width = value.signedEffectiveWidth();

            EirLocationCategory category = EirLocationCategory.immediateFromWordWidth(width);
            do {
                if (operand.locationCategories().contains(category)) {
                    return category;
                }
                category = category.next();
            } while (EirLocationCategory.I.contains(category));

        }
        if (operand.locationCategories().contains(EirLocationCategory.LITERAL)) {
            return EirLocationCategory.LITERAL;
        }
        return null;
    }

}

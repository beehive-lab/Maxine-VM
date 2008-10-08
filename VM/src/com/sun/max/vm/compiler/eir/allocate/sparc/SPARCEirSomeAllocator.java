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
package com.sun.max.vm.compiler.eir.allocate.sparc;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCEirSomeAllocator extends EirSomeAllocator<SPARCEirRegister> {

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

    public SPARCEirSomeAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final SPARCEirABI abi = (SPARCEirABI) methodGeneration.abi();

        _allocatableIntegerRegisters = PoolSet.allOf(SPARCEirRegister.pool());
        _allocatableIntegerRegisters.and(SPARCEirRegister.GeneralPurpose.poolSet());
        _allocatableIntegerRegisters.and(abi.allocatableRegisters());

        _allocatableFloatingPointRegisters = PoolSet.allOf(SPARCEirRegister.pool());
        _allocatableFloatingPointRegisters.and(SPARCEirRegister.FloatingPoint.poolSet());
        _allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }

    @Override
    protected SPARCEirRegister allocateRegisterFor(EirVariable variable, PoolSet<SPARCEirRegister> registers) {
        if (registers.isEmpty()) {
            return null;
        }
        if (variable.kind() != Kind.DOUBLE) {
            if (registers.isEmpty()) {
                return null;
            }
            return registers.removeOne();
        }
        // Iterate over the pool to search the first free double precision register
        final Iterator<SPARCEirRegister> i = registers.iterator();
        while (i.hasNext()) {
            final SPARCEirRegister.FloatingPoint register = (SPARCEirRegister.FloatingPoint) i.next();
            final SPARCEirRegister.FloatingPoint overlappingRegister = register.overlappingSinglePrecision();
            if (overlappingRegister == null) {
                assert register.isDoublePrecision();
                registers.remove(register);
                return register;
            }
            // Check that the overlapping register is not already allocated. If it is, we can't allocate for a double.
            if (registers.contains(overlappingRegister)) {
                // both single-precision register forming the double one are available. Remove then.
                registers.remove(overlappingRegister);
                registers.remove(register);
                return register.isDoublePrecision() ? register : overlappingRegister;
            }
        }
        return null;
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

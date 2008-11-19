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
package com.sun.max.vm.compiler.eir.allocate.amd64;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.*;
import com.sun.max.vm.compiler.eir.amd64.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirUnfinishedAllocator extends EirUnfinishedAllocator<AMD64EirRegister> {

    private final BitSet _unallocatableLocationFlags = new BitSet();
    private final BitSet _integerRegisterFlags = new BitSet();
    private final BitSet _floatingPointRegisterFlags = new BitSet();

    @Override
    protected  BitSet unallocatableLocationFlags() {
        return _unallocatableLocationFlags;
    }

    @Override
    protected  BitSet integerRegisterFlags() {
        return _integerRegisterFlags;
    }

    @Override
    protected  BitSet floatingPointRegisterFlags() {
        return _floatingPointRegisterFlags;
    }

    public AMD64EirUnfinishedAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final AMD64EirABI abi = (AMD64EirABI) methodGeneration.abi();
        for (AMD64EirRegister register : abi.unallocatableRegisters()) {
            _unallocatableLocationFlags.set(locationToIndex(register));
        }
        for (AMD64EirRegister.General register : AMD64EirRegister.General.VALUES) {
            _integerRegisterFlags.set(register.serial());
        }
        for (AMD64EirRegister.XMM register : AMD64EirRegister.XMM.VALUES) {
            _floatingPointRegisterFlags.set(register.serial());
        }
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

    private static final int _stackSlotBaseIndex = AMD64EirRegister.pool().length();

    @Override
    public int locationToIndex(EirLocation location) {
        switch (location.category()) {
            case INTEGER_REGISTER:
            case FLOATING_POINT_REGISTER: {
                final AMD64EirRegister register = (AMD64EirRegister) location;
                return register.serial();
            }
            case STACK_SLOT: {
                final EirStackSlot stackSlot = (EirStackSlot) location;
                return _stackSlotBaseIndex + stackSlot.offset() / methodGeneration().abi().stackSlotSize();
            }
            default: {
                throw ProgramError.unexpected();
            }
        }
    }

    @Override
    public EirLocation locationFromIndex(int index) {
        if (index >= _stackSlotBaseIndex) {
            return methodGeneration().localStackSlotFromIndex(index - _stackSlotBaseIndex);
        }
        if (index >= AMD64EirRegister.General.VALUES.length()) {
            return AMD64EirRegister.XMM.VALUES.get(index - AMD64EirRegister.General.VALUES.length());
        }
        return AMD64EirRegister.General.VALUES.get(index);
    }
}

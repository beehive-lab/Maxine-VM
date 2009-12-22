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
package com.sun.max.vm.compiler.cps.eir.sparc;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Paul Caprioli
 */
public abstract class SPARCEirGenerator extends EirGenerator<SPARCEirGeneratorScheme> {

    public static void addFrameReferenceMap(PoolSet<EirVariable> liveVariables, WordWidth stackSlotWidth, ByteArrayBitMap map) {
        for (EirVariable variable : liveVariables) {
            if (variable.kind() == Kind.REFERENCE) {
                EirLocation location = variable.location();
                if (location.category() == EirLocationCategory.STACK_SLOT) {
                    final EirStackSlot stackSlot = (EirStackSlot) location;
                    if (stackSlot.purpose != EirStackSlot.Purpose.PARAMETER) {
                        final int stackSlotBitIndex = (stackSlot.offset + SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE) / stackSlotWidth.numberOfBytes;
                        map.set(stackSlotBitIndex);
                    }
                } else if (location instanceof SPARCEirRegister.GeneralPurpose) {
                    final SPARCEirRegister.GeneralPurpose gpr = (SPARCEirRegister.GeneralPurpose) location;
                    final int spillIndex = gpr.registerSpillIndex();
                    if (spillIndex >= 0) {
                        map.set(spillIndex);
                    }
                }
            }
        }
    }

    public static void addFrameReferenceMapAtCall(PoolSet<EirVariable> liveVariables, EirOperand[] arguments, WordWidth stackSlotWidth, ByteArrayBitMap map) {
        addFrameReferenceMap(liveVariables, stackSlotWidth, map);
        if (arguments != null) {
            for (EirOperand argument : arguments) {
                if (argument.kind() == Kind.REFERENCE) {
                    final EirLocation location = argument.location();
                    if (location instanceof EirStackSlot) {
                        final EirStackSlot stackSlot = (EirStackSlot) location;
                        final int stackSlotBitIndex = (stackSlot.offset + SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE) / stackSlotWidth.numberOfBytes;
                        map.set(stackSlotBitIndex);
                    }
                }
            }
        }
    }

    private final EirLocation eirCatchParameterLocation = eirABIsScheme().javaABI.getResultLocation(Kind.REFERENCE);

    public SPARCEirGenerator(SPARCEirGeneratorScheme eirGeneratorScheme) {
        super(eirGeneratorScheme);
    }

    @Override
    public EirLocation catchParameterLocation() {
        return eirCatchParameterLocation;
    }

}

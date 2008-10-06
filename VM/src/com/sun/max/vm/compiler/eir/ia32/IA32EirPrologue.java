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
/*VCSID=7191f7cc-59f5-4e54-9f99-5cd2af315089*/
package com.sun.max.vm.compiler.eir.ia32;

import java.util.*;

import com.sun.max.vm.compiler.eir.*;

/**
 * @author Bernd Mathiske
 */
public final class IA32EirPrologue extends EirPrologue<IA32EirInstructionVisitor, IA32EirTargetEmitter> implements IA32EirInstruction {

    public IA32EirPrologue(EirBlock block, EirMethod eirMethod,
                            EirValue[] calleeSavedValues, EirLocation[] calleeSavedRegisters,
                            BitSet isCalleeSavedParameter,
                            EirValue[] parameters, EirLocation[] parameterLocations) {
        super(block, eirMethod, calleeSavedValues, calleeSavedRegisters, isCalleeSavedParameter, parameters, parameterLocations);
    }

    @Override
    public void emit(IA32EirTargetEmitter emitter) {
        if (!eirMethod().isTemplate()) {
            final int frameSize = eirMethod().frameSize();
            if (frameSize != 0) {
                emitter.assembler().subl(emitter.framePointer(), frameSize);
            }
        }
    }

    @Override
    public void acceptVisitor(IA32EirInstructionVisitor visitor) {
        visitor.visit(this);
    }

}

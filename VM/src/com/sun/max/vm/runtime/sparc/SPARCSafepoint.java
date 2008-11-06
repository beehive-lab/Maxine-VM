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
package com.sun.max.vm.runtime.sparc;

import static com.sun.max.asm.sparc.GPR.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.util.Predicate;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class SPARCSafepoint extends Safepoint {

    private final boolean _is32Bit;

    public SPARCSafepoint(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _is32Bit = vmConfiguration.platform().processorKind().dataModel().wordWidth() == WordWidth.BITS_32;
    }

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    private static final GPR _LATCH_REGISTER = G2;

    @INLINE(override = true)
    @Override
    public GPR latchRegister() {
        return _LATCH_REGISTER;
    }

    private SPARCAssembler createAssembler() {
        if (_is32Bit) {
            return new SPARC32Assembler(0);
        }
        return new SPARC64Assembler(0);
    }

    @Override
    protected byte[] createCode() {
        final SPARCAssembler asm = createAssembler();
        try {
            if (_is32Bit) {
                asm.lduw(_LATCH_REGISTER, G0, _LATCH_REGISTER);
            } else {
                asm.ldx(_LATCH_REGISTER, G0, _LATCH_REGISTER);
            }
            return asm.toByteArray();
        } catch (AssemblyException assemblyException) {
            throw ProgramError.unexpected("could not assemble safepoint code");
        }
    }

    private static final Symbolizer<GPR> SIGNAL_INTEGER_REGISTER = Symbolizer.Static.fromSymbolizer(GPR.SYMBOLIZER, new Predicate<GPR>() {
        public boolean evaluate(GPR register) {
            return register.isOut() || (register.isGlobal() && register != G0);
        }
    });

    @Override
    public Pointer getInstructionPointer(Pointer trapState) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getStackPointer(Pointer trapState, TargetMethod targetMethod) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getFramePointer(Pointer trapState, TargetMethod targetMethod) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getSafepointLatch(Pointer trapState) {
        throw Problem.unimplemented();
    }
    @Override
    public void setSafepointLatch(Pointer trapState, Pointer value) {
        throw Problem.unimplemented();
    }
    @Override
    public Pointer getRegisterState(Pointer trapState) {
        throw Problem.unimplemented();
    }
    @Override
    public int getTrapNumber(Pointer trapState) {
        throw Problem.unimplemented();
    }
}

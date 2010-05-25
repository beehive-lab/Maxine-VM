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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.amd64.complete.*;
import com.sun.max.asm.ia32.complete.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public final class TargetBreakpoint {

    @HOSTED_ONLY
    public static byte[] createBreakpointCode(InstructionSet instructionSet) {
        try {
            switch (instructionSet) {
                case AMD64: {
                    final AMD64Assembler assembler = new AMD64Assembler();
                    assembler.int_3();
                    return assembler.toByteArray();
                }
                case IA32: {
                    final IA32Assembler assembler = new IA32Assembler();
                    assembler.int_3();
                    return assembler.toByteArray();
                }
                case SPARC: {
                    final WordWidth wordWidth = VMConfiguration.target().platform().processorKind.dataModel.wordWidth;
                    final SPARCAssembler assembler = wordWidth == WordWidth.BITS_64 ? new SPARC64Assembler() : new SPARC32Assembler();
                    assembler.ta(ICCOperand.XCC, GPR.G0, SoftwareTrap.ST_BREAKPOINT.trapNumber());
                    return assembler.toByteArray();
                }
                default: {
                    FatalError.unimplemented();
                    break;
                }
            }
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected();
        }
        return null;
    }

    public static final byte[] breakpointCode = createBreakpointCode(Platform.target().processorKind.instructionSet);

    private final Pointer instructionPointer;
    private byte[] originalCode;

    private TargetBreakpoint(Address instructionPointer) {
        this.instructionPointer = instructionPointer.asPointer();
    }

    public boolean isEnabled() {
        return originalCode != null;
    }

    private void enable() {
        if (originalCode == null) {
            originalCode = new byte[breakpointCode.length];
            // TODO (mlvdv) Record original code into the history, key instruction pointer.
            // Pick a data structure that can be read easily by the interpreter,
            // a hash table with overwriting because the interpreter is slow
            Memory.readBytes(instructionPointer, originalCode);
            Memory.writeBytes(breakpointCode, instructionPointer);
        }
    }

    private void disable() {
        if (originalCode != null) {
            Memory.writeBytes(originalCode, instructionPointer);
            originalCode = null;
        }
    }

    /**
     * This data structure is easy to interpret remotely.
     */
    private static final SortedLongArrayMapping<TargetBreakpoint> targetBreakpoints = new SortedLongArrayMapping<TargetBreakpoint>();

    // make another array like this for deleted TargetBreakpoints
    // add another method to look them up by address

    @INSPECTED
    public static byte[] findOriginalCode(long instructionPointer) {
        final TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer);
        if (targetBreakpoint != null) {
            return targetBreakpoint.originalCode;
        }
        return null;
    }

    public static synchronized void make(Address instructionPointer) {
        TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer.toLong());
        if (targetBreakpoint == null) {
            targetBreakpoint = new TargetBreakpoint(instructionPointer);
            targetBreakpoints.put(instructionPointer.toLong(), targetBreakpoint);
        }
        targetBreakpoint.enable();
    }

    public static synchronized void delete(Address instructionPointer) {
        final TargetBreakpoint targetBreakpoint = targetBreakpoints.get(instructionPointer.toLong());
        if (targetBreakpoint != null) {
            targetBreakpoint.disable();
            targetBreakpoints.remove(instructionPointer.toLong());
        }
    }
}

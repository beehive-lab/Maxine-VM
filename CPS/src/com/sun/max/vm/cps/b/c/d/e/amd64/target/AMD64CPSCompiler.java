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
package com.sun.max.vm.cps.b.c.d.e.amd64.target;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.b.c.d.e.amd64.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64CPSCompiler extends BcdeAMD64Compiler implements TargetGeneratorScheme {

    /**
     * Utility class used to find and patch a call to a {@linkplain StaticTrampoline static trampoline}.
     */
    static class StaticTrampolineContext implements RawStackFrameVisitor {

        @Override
        public boolean visitFrame(TargetMethod targetMethod, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, boolean isTopFrame) {
            if (isTopFrame) {
                return true;
            }
            Pointer callSite = instructionPointer.minus(AMD64OptStackWalking.RIP_CALL_INSTRUCTION_SIZE);
            if (callSite.readByte(0) == CALL) {
                Pointer target = instructionPointer.plus(callSite.readInt(1));
                if (StaticTrampoline.isEntryPoint(target)) {
                    final TargetMethod caller = Code.codePointerToTargetMethod(callSite);

                    final ClassMethodActor callee = caller.callSiteToCallee(callSite);

                    // Use the caller's abi to get the correct entry point.
                    final Address calleeEntryPoint = CompilationScheme.Static.compile(callee, caller.abi().callEntryPoint);
                    patchRipCallSite(callSite, calleeEntryPoint);

                    // Make the trampoline's caller re-executes the now modified CALL instruction after we return from the trampoline:
                    Pointer trampolineCallerRipPointer = stackPointer.minus(Word.size());
                    trampolineCallerRipPointer.setWord(callSite); // patch return address
                    return false;
                }
            }
            return true;
        }
    }

    private final StaticTrampolineContext staticTrampolineContext = new StaticTrampolineContext();

    private final AMD64EirToTargetTranslator eirToTargetTranslator;

    public AMD64CPSCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        eirToTargetTranslator = new AMD64EirToTargetTranslator(this);
    }

    public TargetGenerator targetGenerator() {
        return eirToTargetTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return eirToTargetTranslator;
    }

    @Override
    public List<IrGenerator> irGenerators() {
        final List<IrGenerator> result = new LinkedList<IrGenerator>(super.irGenerators());
        result.add((IrGenerator) targetGenerator());
        return result;
    }

    @INLINE
    static void patchRipCallSite(Pointer callSite, Address calleeEntryPoint) {
        final int calleeOffset = calleeEntryPoint.minus(callSite.plus(AMD64OptStackWalking.RIP_CALL_INSTRUCTION_SIZE)).toInt();
        callSite.writeInt(1, calleeOffset);
    }

    public static final byte CALL = (byte) 0xE8;


    /**
     * @see StaticTrampoline
     */
    @Override
    @NEVER_INLINE
    public void staticTrampoline() {
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VMRegister.getInstructionPointer(),
                VMRegister.getCpuStackPointer(),
                VMRegister.getCpuFramePointer(),
                staticTrampolineContext);
    }

    public void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset).asPointer();
        final AMD64Assembler assembler = new AMD64Assembler(callSite.toLong());
        final Label label = new Label();
        assembler.fixLabel(label, callEntryPoint.asAddress().toLong());
        assembler.call(label);
        try {
            final byte[] code = assembler.toByteArray();
            Bytes.copy(code, 0, targetMethod.code(), callOffset, code.length);
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected("patching call site failed", assemblyException);
        }
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            AMD64OptStackWalking.initialize();
        }
    }
}

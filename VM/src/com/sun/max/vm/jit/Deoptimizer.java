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
package com.sun.max.vm.jit;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Deoptimization of stack frames into JIT format.
 *
 * To trigger the deoptimization of an optimized target method,
 * we patch an illegal instruction after every call site and at every safepoint in the method code.
 *
 * The Inspector can also trigger deoptimization
 * by remotely patching in the illegal instructions.
 *
 * The actual deoptimization then happens in the trap stub
 * once the method has been executing on the top frame
 * and hit one of these illegal instructions.
 *
 * @author Bernd Mathiske
 */
public abstract class Deoptimizer {

    @CONSTANT_WHEN_NOT_ZERO
    private static Deoptimizer _deoptimizer;

    protected static Deoptimizer deoptimizer() {
        return _deoptimizer;
    }

    protected Deoptimizer() {
        _deoptimizer = this;
    }

    private static JitTargetMethod jitCompile(ClassMethodActor classMethodActor) {
        return (JitTargetMethod) VMConfiguration.target().jitScheme().compile(classMethodActor, CompilationDirective.DEFAULT);
    }

    public static int referenceReturnRegisterIndex() {
        return _deoptimizer.referenceReturnRegister().index();
    }

    protected abstract Deoptimization createDeoptimization();

    public static void deoptimizeTopFrame() {
        final Pointer instructionPointer = VmThreadLocal.TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer();
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);

        while (true) {
            Safepoint.enable();
            final Word safepointEpoch = VmThreadLocal.SAFEPOINT_EPOCH.getVariableWord();

            final Deoptimization deoptimization = _deoptimizer.createDeoptimization();
            new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VmThreadLocal.TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer(),
                                                                            VmThreadLocal.TRAP_STACK_POINTER.getVariableWord().asPointer(),
                                                                            VmThreadLocal.TRAP_FRAME_POINTER.getVariableWord().asPointer(),
                                                                            deoptimization);

            final int stopIndex = targetMethod.findClosestStopIndex(instructionPointer);
            final TargetJavaFrameDescriptor targetJavaFrameDescriptor = targetMethod.getJavaFrameDescriptor(stopIndex);

            deoptimization.createJitFrames(targetJavaFrameDescriptor);

            Safepoint.disable();
            if (VmThreadLocal.SAFEPOINT_EPOCH.getVariableWord().equals(safepointEpoch)) {
                VmThreadLocal.DEOPTIMIZER_REFERENCE_OCCURRENCES.setVariableReference(Reference.fromJava(ReferenceOccurrences.NONE));
                VmThreadLocal.DEOPTIMIZER_INSTRUCTION_POINTER.setVariableWord(Word.zero());
                deoptimization.patchExecutionContext();
                ProgramError.unexpected(); // Never reached. The previous statement should land us in the deoptimized method.
            } else {
                // A GC might have happened, rendering the created JIT frame contents invalid.
                // Retry!
            }
        }
    }

    public abstract int directCallSize();

    public abstract int indirectCallSize(byte firstInstructionByte);

    private int indirectCallSize(TargetMethod targetMethod, int stopIndex) {
        return indirectCallSize(targetMethod.codeStart().plus(targetMethod.stopPosition(stopIndex)).getByte());
    }

    public abstract byte[] illegalInstruction();

    /**
     * Which safepoint-saved integer registers need to be preserved by the GC throughout deoptimization.
     */
    public enum ReferenceOccurrences {
        /**
         * Indicates an illegal instruction in Java code at an unexpected position, i.e. a bug.
         */
        ERROR,

        /**
         * Indicates that the illegal instruction trap happened
         * just after a return from a call that did NOT return a reference.
         * Since all registers in optimized code are caller-saved,
         * no registers at all need to be tracked by the GC.
         */
        NONE,

        /**
         * Indicates that the illegal instruction trap happened
         * just after a return from a call that DID return a reference.
         * So the GC needs to track the register that carries a reference return result.
         */
        RETURN,

        /**
         * Indicates that the illegal instruction trap happened at a safepoint.
         * The GC needs to track registers according to the register reference map at the safepoint.
         */
        SAFEPOINT
    }

    public static ReferenceOccurrences determineReferenceOccurrences(TargetMethod targetMethod, Pointer instructionPointer) {
        if (!Memory.equals(instructionPointer, _deoptimizer.illegalInstruction()) || targetMethod instanceof JitTargetMethod) {
            return ReferenceOccurrences.ERROR;
        }
        final int position = instructionPointer.minus(targetMethod.codeStart()).toInt();
        int i;
        for (i = 0; i < targetMethod.numberOfDirectCalls(); i++) {
            if (position == targetMethod.stopPosition(i) + _deoptimizer.directCallSize()) {
                return targetMethod.isReferenceCall(i) ? ReferenceOccurrences.RETURN : ReferenceOccurrences.NONE;
            }
        }
        for (; i < targetMethod.numberOfIndirectCalls(); i++) {
            if (position == targetMethod.stopPosition(i) + _deoptimizer.indirectCallSize(targetMethod, i)) {
                return targetMethod.isReferenceCall(i) ? ReferenceOccurrences.RETURN : ReferenceOccurrences.NONE;
            }
        }
        for (; i < targetMethod.numberOfSafepoints(); i++) {
            if (position == targetMethod.stopPosition(i)) {
                return ReferenceOccurrences.SAFEPOINT;
            }
        }
        return ReferenceOccurrences.ERROR;
    }

    public abstract TargetLocation.IntegerRegister referenceReturnRegister();

    private static void patchIllegalInstruction(TargetMethod targetMethod, int stopIndex, int callSize) {
        Memory.writeBytes(_deoptimizer.illegalInstruction(), targetMethod.codeStart().plus(targetMethod.stopPosition(stopIndex) + callSize));
    }

    public static void triggerDeoptimization(TargetMethod targetMethod) {
        if (targetMethod instanceof JitTargetMethod) {
            return;
        }
        int i;
        for (i = 0; i < targetMethod.numberOfDirectCalls(); i++) {
            patchIllegalInstruction(targetMethod, i, _deoptimizer.directCallSize());
        }
        for (; i < targetMethod.numberOfIndirectCalls(); i++) {
            patchIllegalInstruction(targetMethod, i, _deoptimizer.indirectCallSize(targetMethod, i));
        }
        for (; i < targetMethod.numberOfSafepoints(); i++) {
            patchIllegalInstruction(targetMethod, i, 0);
        }
    }
}

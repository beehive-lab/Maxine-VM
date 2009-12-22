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
package com.sun.max.vm.compiler.cps.b.c.d.e.sparc.target;

import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.stack.JavaStackFrameLayout.*;

import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.eir.*;
import com.sun.max.vm.compiler.cps.eir.EirStackSlot.*;
import com.sun.max.vm.compiler.cps.eir.sparc.*;
import com.sun.max.vm.compiler.cps.eir.sparc.SPARCEirRegister.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.sparc.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.type.*;

/**
 * Specific adapter for calls from / to template-based JITed code (with explicit stack management) to code produced by the
 * optimizing CPS-based compiler.
 * When calling from code produced by one compiler to code produced by another, an adapter frame is created in between
 * to adjust for different calling conventions. The JIT for SPARC does not use register windows.
 * It maintains an expression stack adjusted dynamically, and use both
 * a frame pointer and a stack pointer. The former is used to access arguments and local variables, the latter is used
 * to maintain the Java expression stack. The stack pointer is the native stack pointer and is therefore biased.
 * The Java frame pointer is a local register and is not biased.
 *
 * All arguments to Java calls are passed via the Java expression stack. Result
 * are returned in a result register.
 *
 * Both compilers embed extra code in the target method to create an "adapter frame".
 *
 * @author Laurent Daynes
 * @author Paul Caprioli
 */
public abstract class SPARCAdapterFrameGenerator extends AdapterFrameGenerator<SPARCAssembler> {

    public static final GPR SAVED_CALLER_ADDRESS = GPR.L5;

    /**
     *  Mask for SPARC instruction encoding (bits 13, 19 to 24, 30 and 31).
     */
    private static final int OP3_IMM_MASK = 0xc1f82000;

    private static final int SUB_IMM_OP   = 0x80202000;
    private static final int SAVE_IMM_OP  = 0x81e02000;

    private static final int OP3_NONLOADMEMOP_MASK = 0xc0200000;

    private static final int DISP19_MASK = 0x7ffff;
    private static final int SIMM13_MASK = 0x1fff;

    protected final GPR intScratchRegister;
    protected final GPR longScratchRegister;
    protected final GPR optimizedCodeStackPointer;
    protected final GPR optimizedCodeFramePointer;
    protected final GPR jitedCodeFramePointer;
    protected final GPR literalBaseRegister;
    protected int jitedCodeFrameSize = 0;

    public static SPARCAdapterFrameGenerator jitToOptimizedCompilerAdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
        return new JitToOptimizedFrameAdapterGenerator(classMethodActor, optimizingCompilerAbi);
    }

    public static boolean jitToOptimizedCallNeedsAdapterFrame(MethodActor classMethodActor) {
        // Only static parameter-less methods and dynamic trampolines can avoid an adapter frame.
        // Others need the adapter frame to (i) load parameters onto the appropriate register on method entry, and
        //                                 (ii) retract the stack of the JIT caller.
        return !((classMethodActor.isStatic() && (classMethodActor.descriptor().numberOfParameters() == 0)) || isDynamicTrampoline(classMethodActor));
    }

    /**
     * Indicates whether the specified instruction is a subtract immediate instruction.
     * @param instruction
     * @return
     */
    private static boolean isSubImmInstruction(int instruction) {
        return (instruction & OP3_IMM_MASK) == SUB_IMM_OP;
    }

    /**
     * Indicates whether the specified instruction is a save with immediate instruction.
     * @param instruction
     * @return
     */
    private static boolean isSaveImmInstruction(int instruction) {
        return (instruction & OP3_IMM_MASK) == SAVE_IMM_OP;
    }

    private static boolean isNonLoadMemOp(int instruction) {
        return (instruction & OP3_NONLOADMEMOP_MASK) == OP3_NONLOADMEMOP_MASK;
    }

    private static int extractSimm13(int instruction) {
        return ((instruction & SIMM13_MASK) << 19) >> 19;
    }

    public static SPARCAdapterFrameGenerator optimizedToJitCompilerAdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
        return new OptimizedToJitFrameAdapterGenerator(classMethodActor, optimizingCompilerAbi);
    }

    public static Pointer jitEntryPointBranchTarget(StackFrameWalker stackFrameWalker, TargetMethod targetMethod) {
        assert jitToOptimizedCallNeedsAdapterFrame(targetMethod.classMethodActor());
        final Pointer jitEntryPoint = JIT_ENTRY_POINT.in(targetMethod);
        final int branchInstruction = stackFrameWalker.readInt(jitEntryPoint, 0);
        // Can't use the following. It's part of the assembler generator and is not put in the vm image.
        // final int disp19 = SPARCFields._disp19.extract(branchInstruction);
        // The branch to the entry point is always forward. So the disp19 value is always > 0 and we don't have to fiddle to get the signed value, just mask.
        final int disp19 = branchInstruction & DISP19_MASK;
        return jitEntryPoint.plus(disp19 << 2);
    }

    public static int jitToOptimizedAdapterFrameSize(StackFrameWalker stackFrameWalker, Pointer adapterFirstInstruction) {
        final int instruction = stackFrameWalker.readInt(adapterFirstInstruction, 0);
        if (isSubImmInstruction(instruction)) {
            // Note that SPARCFields.simm13.extract(instruction) is part of the assembler generator and is not put in the vm image.
            return extractSimm13(instruction);
        }
        return 0;
    }

    public static int optToJitAdapterFrameSize(StackFrameWalker stackFrameWalker, Pointer optimizedEntryPoint) {
        for (int offset = SPARCEirPrologue.sizeOfFrameBuilderInstructions(0, true) - 4; /*empty*/; offset += 4) {
            final int instruction = stackFrameWalker.readInt(optimizedEntryPoint, offset);
            if (isSaveImmInstruction(instruction)) {
                return -extractSimm13(instruction);
            }
            if (isNonLoadMemOp(instruction)) {
                FatalError.unexpected("Expected SAVE instruction.");
            }
        }
    }

    protected SPARCAdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizedAbi) {
        super(classMethodActor, optimizedAbi);
        intScratchRegister = ((SPARCEirRegister.GeneralPurpose) optimizedAbi.getScratchRegister(Kind.INT)).as();
        longScratchRegister = ((SPARCEirRegister.GeneralPurpose) optimizedAbi.getScratchRegister(Kind.LONG)).as();
        optimizedCodeStackPointer = ((SPARCEirRegister.GeneralPurpose) optimizedAbi.stackPointer()).as();
        optimizedCodeFramePointer = ((SPARCEirRegister.GeneralPurpose) optimizedAbi.framePointer()).as();
        final TargetABI jitABI = VMConfiguration.target().targetABIsScheme().jitABI();
        jitedCodeFramePointer = (GPR) jitABI.framePointer();
        literalBaseRegister = (GPR) jitABI.literalBaseRegister();
    }

    public void setJitedCodeFrameSize(int size) {
        jitedCodeFrameSize = size;
    }

    /**
     * Frame Adapter Generator for calls from JIT-ed code to optimized code.
     * No extra spaces needs to be allocated on the stack when entering optimized code from JIT code, except if some arguments cannot be passed by registers.
     * In this case, additional space needs to be reserved. The overflowing argument will be copied in that extra-space to satisfy the optimizing compiler calling convention.
     * The other parameters only need to be loaded into appropriate registers.
     * Further, no caller context need to be saved on the stack: the register window pushed by the optimizing code prologue already take care of this.
     * Parameter-less calls don't need any adapter code. Their prologue can just nops.
     */
    static final class JitToOptimizedFrameAdapterGenerator extends SPARCAdapterFrameGenerator {
        JitToOptimizedFrameAdapterGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
            super(classMethodActor, optimizingCompilerAbi);
        }

        @Override
        public void emitPrologue(SPARCAssembler assembler) {
            // Some method don't need any adapter frame:
            // - static parameter-less method
            // - virtual method with no parameter (except the receiver)
            //
            // The former, because it doesn't have any parameter.
            // The latter, because its single parameter is already loaded in the appropriate register (%o0),
            // thanks to the way the invokevirtual templates are implemented.
            //
            // For these methods, we only need to save the literal base register in the caller's saving area.
            // This is necessary for stack unwinding, which expects to retrieve the literal base register from there.
            // This can be done in place, in the two-instruction prologue in the jit-entry point.

            if (jitToOptimizedCallNeedsAdapterFrame(classMethodActor())) {
                // Branch to frame adapter code, and save the caller's address in local register in the delay slot.
                assembler.ba(AnnulBit.NO_A, BranchPredictionBit.PT, ICCOperand.ICC, adapterStart);
                assembler.mov(GPR.O7, SAVED_CALLER_ADDRESS);
            } else {
                assembler.stx(literalBaseRegister, jitedCodeFramePointer, -STACK_SLOT_SIZE);
                assembler.nop();
            }
            assembler.bindLabel(methodEntryPoint);
        }

        @Override
        public void emitEpilogue(SPARCAssembler assembler) {
            if (jitToOptimizedCallNeedsAdapterFrame(classMethodActor())) {
                emitFrameAdapter(assembler);
            }
        }

        @Override
        protected Purpose adapterArgumentPurpose() {
            // We need to override with LOCAL here to get the outgoing registers for the parameters.
            return Purpose.LOCAL;
        }

        @Override
        protected void emit(Kind[] parametersKinds, EirLocation[] parameterLocations, Label adapterReturnPoint, Label methodEntryPoint) {
            if (isDynamicTrampoline(classMethodActor())) {
                // Nop for dynamic trampoline.
                return;
            }
            final int adapterFrameSize = optimizedABI().overflowArgumentsSize(parameterLocations);

            if (adapterFrameSize > 0) {
                assert SPARCAssembler.isSimm13(adapterFrameSize);
                assembler().sub(optimizedCodeStackPointer, adapterFrameSize, optimizedCodeStackPointer);
            }

            // On entry to the adapter, the top of the stack points to the last arguments of the JITed caller.
            // We set an offset to that last argument (relative to the new stack pointer) and iterate over the arguments in
            // reverse order, from last to first.

            int jitCallerStackOffset = SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT + adapterFrameSize;
            for (int i = parameterLocations.length - 1; i > 0;  i--) {
                final Kind parameterKind = parametersKinds[i];
                adaptParameter(parameterKind, parameterLocations[i], jitCallerStackOffset);
                jitCallerStackOffset += JitStackFrameLayout.stackSlotSize(parameterKind);
            }
            assert parameterLocations[0] instanceof EirRegister;
            assembler().call(methodEntryPoint);
            // Emit the adaptation of the first parameter in the delay slot of the call to the method entry point.
            final int adapterPosition = assembler().currentPosition();
            adaptParameter(parametersKinds[0], parameterLocations[0], jitCallerStackOffset);
            assert assembler().currentPosition() == adapterPosition + InstructionSet.SPARC.instructionWidth;
            jitCallerStackOffset += JitStackFrameLayout.stackSlotSize(parametersKinds[0]);

            // Amount to retract to the stack: the adapter frame plus the parameters of the call. The following gives exactly that.
            // Note that the adapter does not save the callee frame pointer nor the return address on the stack. Instead, it exploits the
            // pushing of a register window by the callee to save these in local registers:  the frame pointer is already in a local register;
            // the caller's address is saved in the SAVED_CALLER_ADDRESS register.  Note that this is only for calls from JIT.
            final int stackAmountInBytes = jitCallerStackOffset - SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT;

            // Return to the JITed caller. The frame adapter saved the call address in local register SAVED_CALLER_ADDRESS.
            assembler().bindLabel(adapterReturnPoint);
            assembler().jmpl(SAVED_CALLER_ADDRESS, 8, GPR.G0);
            assembler().add(optimizedCodeStackPointer, stackAmountInBytes, optimizedCodeStackPointer);
        }

        private void adaptParameter(Kind kind, EirLocation parameterLocation, int offset32) {
            final int offset = offset32 + JitStackFrameLayout.offsetWithinWord(kind);
            switch (parameterLocation.category()) {
                case INTEGER_REGISTER:
                    assert parameterLocation instanceof SPARCEirRegister.GeneralPurpose;
                    GPR gprParameterRegister = ((SPARCEirRegister.GeneralPurpose) parameterLocation).as();
                    switch (kind.asEnum) {
                        case BYTE:
                        case BOOLEAN:
                            assembler().ldsb(optimizedCodeStackPointer, offset, gprParameterRegister);
                            break;
                        case SHORT:
                        case CHAR:
                            assembler().ldsh(optimizedCodeStackPointer, offset, gprParameterRegister);
                            break;
                        case INT:
                            assembler().ldsw(optimizedCodeStackPointer, offset, gprParameterRegister);
                            break;
                        case LONG:
                        case WORD:
                        case REFERENCE:
                            assembler().ldx(optimizedCodeStackPointer, offset, gprParameterRegister);
                            break;
                        default: {
                            ProgramError.unexpected();
                        }
                    }
                    break;
                case FLOATING_POINT_REGISTER:
                    assert parameterLocation instanceof SPARCEirRegister.FloatingPoint;
                    FloatingPoint fpParameterRegister = (SPARCEirRegister.FloatingPoint) parameterLocation;
                    switch (kind.asEnum) {
                        case FLOAT:
                            assembler().ld(optimizedCodeStackPointer, offset, fpParameterRegister.asSinglePrecision());
                            break;
                        case DOUBLE:
                            assembler().ldd(optimizedCodeStackPointer, offset, fpParameterRegister.asDoublePrecision());
                            break;
                        default: {
                            ProgramError.unexpected();
                        }
                    }
                    break;
                case STACK_SLOT:
                    final int biasedOptToStackOffset32 = SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT + parameterLocation.asStackSlot().offset;
                    // SPARC is a 64-bit architecture, so there's no performance advantage to copying fewer than 64 bits.
                    // So, we move an entire word regardless of the kind.
                    assembler().ldx(optimizedCodeStackPointer, offset32, longScratchRegister);
                    assembler().stx(longScratchRegister, optimizedCodeStackPointer, biasedOptToStackOffset32);
                    break;
                default:
                    ProgramError.unexpected();
            }
        }
    }

    /**
     * Frame Adapter Generator for calls from optimized code to JIT-ed code.
     * The frame adapter store the Java parameters at the top of its frame
     * to mimic the calling convention of the JIT.
     * The bottom of its frame comprises a floating point temporary area
     * used to load immediate floating point values (single and double precision).
     * This area is shared by all JIT code with a frame between this adapter and
     * the next optimized code frame up the call stack.
     * Next is a saving area where the instruction pointer of the caller is saved.
     * This simplify the job of stack walkers which don't need to track the
     * register window for this frame. The caller's stack and frame pointer
     * can be inferred from the adapter frame pointer.
     *
     *             +--------------------------------+
     *             |  opt caller saved              |
     *             | register window                |
     *             +--------------------------------+ < Frame pointer of adapter frame
     *  [FP - 16]  | floating point temp area       |
     *             +--------------------------------+
     *  [FP - 24]  | Opt caller's RIP               |
     *             +--------------------------------+
     *             | JIT frame incoming             |
     *             | Java parameters                |
     *             +--------------------------------+ < top of adapter frame
     *                                                  == bottom of JIT frame.
     *
     */
    static final class OptimizedToJitFrameAdapterGenerator extends SPARCAdapterFrameGenerator {
        private Label jitEntryPoint;

        OptimizedToJitFrameAdapterGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
            super(classMethodActor, optimizingCompilerAbi);
        }

        @Override
        public void setJitEntryPoint(Label jitEntryPoint) {
            this.jitEntryPoint = jitEntryPoint;
        }

        @Override
        public void emitPrologue(SPARCAssembler assembler) {
        }

        @Override
        public void emitEpilogue(SPARCAssembler assembler) {
            emitFrameAdapter(assembler);
        }

        static int adapterFrameSize(Kind[] parametersKinds, TargetABI targetABI) {
            // The adapter frame comprises space for the arguments plus spaces for the mandatory SPARC register save area,
            // one word for the opt caller's rip (do we need this ?), and the floating point temp area, which is a staging area
            // to load immediate floating point value (single and double precision).
            final int parameterSize = JitStackFrameLayout.parametersFrameSize(parametersKinds);
            return targetABI.alignFrameSize(parameterSize +
                Word.size() +
                SPARCJitStackFrameLayout.FLOATING_POINT_TEMP_AREA_SIZE +
                SPARCStackFrameLayout.MIN_STACK_FRAME_SIZE);
        }

        /**
         * Emits instructions to adapt calls from optimized code to JIT code. A register window is provided for the subsequent sequence of JIT-compiled method.
         * The adapter then pushes the register arguments on the stack, followed by the memory arguments building a stack of arguments as expected by the JIT.
         * A call to the entry point is then made.
         * On return, the register window is popped.
         */
        @Override
        protected void emit(Kind[] parametersKinds, EirLocation[] parameterLocations, Label adapterReturnPoint, Label methodEntryPoint) {
            final int adapterFrameSize = adapterFrameSize(parametersKinds, optimizedABI().targetABI());
            int stackOffset = SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT;
            final int ripSaveAreaOffset =  SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA - Word.size();

            assert SPARCAssembler.isSimm13(adapterFrameSize);
            assert SPARCAssembler.isSimm13(stackOffset + adapterFrameSize);

            SPARCEirPrologue.emitFrameBuilder(assembler(), adapterFrameSize, optimizedCodeStackPointer, intScratchRegister, true);
            assembler().stx(GPR.I7, optimizedCodeFramePointer, ripSaveAreaOffset);

            // emit the parameters in reverse order.
            for (int i = parameterLocations.length - 1; i >= 0; i--) {
                adaptParameter(parametersKinds[i], parameterLocations[i], stackOffset);
                stackOffset += JitStackFrameLayout.stackSlotSize(parametersKinds[i]);
            }

            // Initialized JIT frame pointer. Stack walker assumes that they have a valid frame pointer in the JIT frame pointer
            // FIXME: we may remove this if we change the JIT abi to use %i6 as well as their frame pointer.

            // We store the unbiased frame pointer in the JITed frame pointer.
            // This allow JITed code return template to avoid checking whether the caller is an adapter frame when restoring the
            // their literal base. Instead, they can blindly load at  FP - 8. When the caller is an adapter, this is harmless (it loads
            // an arbitrary value off the floating point temp area).
            assembler().add(optimizedCodeFramePointer, StackBias.SPARC_V9.stackBias(), jitedCodeFramePointer);
            final boolean largeFrame = !SPARCAssembler.isSimm13(jitedCodeFrameSize);

            assembler().call(methodEntryPoint);
            if (largeFrame) {
                assembler().sethi(SPARCAssembler.hi(jitedCodeFrameSize), BytecodeToSPARCTargetTranslator.PROLOGUE_SCRATCH_REGISTER);
            } else {
                assembler().sub(optimizedCodeStackPointer, jitedCodeFrameSize, optimizedCodeStackPointer);
            }

            // Return from the adapter frame
            assembler().bindLabel(adapterReturnPoint);
            // The registers %i7 and %i6 were untouched by JITed code and still represent, respectively, the caller's PC and stack pointer.
            // Results by the JITed code follow the JIT calling convention and are stored in %o0 and %f0.
            // We need to move %o0 to the %o0 of the caller's window.
            assembler().ret();
            assembler().restore(GPR.O0, GPR.G0, GPR.O0);
            assembler().bindLabel(jitEntryPoint);
            // Save the caller's literal base.  The saving area for the literal base is immediately below its frame pointer.
            assembler().stx(literalBaseRegister, jitedCodeFramePointer, -STACK_SLOT_SIZE);
            assembler().bindLabel(methodEntryPoint);
        }

        private void adaptParameter(Kind kind, EirLocation parameterLocation, int offset32) {
            switch (parameterLocation.category()) {
                case INTEGER_REGISTER:
                    assert parameterLocation instanceof SPARCEirRegister.GeneralPurpose;
                    GPR gprParameterRegister = ((SPARCEirRegister.GeneralPurpose) parameterLocation).as();
                    if (kind == Kind.LONG || kind == Kind.WORD || kind == Kind.REFERENCE) {
                        assembler().stx(gprParameterRegister, optimizedCodeStackPointer, offset32);
                    } else {
                        assembler().stw(gprParameterRegister, optimizedCodeStackPointer, offset32 + JitStackFrameLayout.CATEGORY1_OFFSET_WITHIN_WORD);
                    }
                    break;
                case FLOATING_POINT_REGISTER:
                    assert parameterLocation instanceof SPARCEirRegister.FloatingPoint;
                    FloatingPoint fpParameterRegister = (SPARCEirRegister.FloatingPoint) parameterLocation;
                    if (kind == Kind.FLOAT) {
                        assembler().st(fpParameterRegister.asSinglePrecision(), optimizedCodeStackPointer, offset32 + JitStackFrameLayout.CATEGORY1_OFFSET_WITHIN_WORD);
                    } else {  // Kind.DOUBLE
                        assembler().std(fpParameterRegister.asDoublePrecision(), optimizedCodeStackPointer, offset32);
                    }
                    break;
                case STACK_SLOT:
                    final int slotOffsetFromCallerFP = SPARCStackFrameLayout.OFFSET_FROM_SP_TO_FIRST_SLOT + parameterLocation.asStackSlot().offset;
                    if (kind.isCategory2() || kind == Kind.WORD || kind == Kind.REFERENCE) {
                        assembler().ldx(optimizedCodeFramePointer, slotOffsetFromCallerFP, longScratchRegister);
                        assembler().stx(longScratchRegister, optimizedCodeStackPointer, offset32);
                    } else {
                        assembler().lduw(optimizedCodeFramePointer, slotOffsetFromCallerFP + JitStackFrameLayout.CATEGORY1_OFFSET_WITHIN_WORD, intScratchRegister);
                        assembler().stw(intScratchRegister, optimizedCodeStackPointer, offset32 + JitStackFrameLayout.CATEGORY1_OFFSET_WITHIN_WORD);
                    }
                    break;
                default:
                    ProgramError.unexpected();
            }
        }

    }

}

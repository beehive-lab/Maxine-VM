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

import com.sun.max.asm.*;
import com.sun.max.asm.Assembler.*;
import com.sun.max.asm.amd64.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.asm.amd64.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * Specific adapter from / to template-based JIT (with explicit stack management) to the optimizing CPS-based compiler.
 * When calling from code produced by one compiler to code produced by another, an adapter frame is created in between
 * to adjust for different calling conventions. The JIT maintains an expression stack adjusted dynamically, and use both
 * a frame pointer and a stack pointer. The former is used to access arguments and local variables, the latter is used
 * to maintain the Java expression stack. All arguments to Java calls are passed via the Java expression stack. Result
 * are returned in a result register.
 *
 * Both compilers embed extra code in the target method to create an "adapter frame".
 *
 * @author Laurent Daynes
 */
public abstract class AMD64AdapterFrameGenerator extends AdapterFrameGenerator<AMD64Assembler> {

    protected AMD64AdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizedAbi) {
        super(classMethodActor, optimizedAbi);
        scratchRegister = (AMD64EirRegister.General) optimizedAbi.getScratchRegister(Kind.LONG);
    }

    /*
     * Scratch register for memory-to-memory moves.
     */
    protected AMD64EirRegister.General scratchRegister;

    public AMD64GeneralRegister64 stackPointer() {
        return AMD64GeneralRegister64.RSP;
    }

    public AMD64GeneralRegister64 framePointer() {
        return AMD64GeneralRegister64.RBP;
    }

    public AMD64GeneralRegister64 resultRegister() {
        return AMD64GeneralRegister64.RAX;
    }

    private static final byte SUB_IMM8 = (byte) 0x83;
    private static final byte SUB_IMM32 = (byte) 0x81;

    public static AMD64AdapterFrameGenerator jitToOptimizingCompilerAdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
        return new JitToOptimizingFrameAdapterGenerator(classMethodActor, optimizingCompilerAbi);
    }

    public static AMD64AdapterFrameGenerator optimizingToJitCompilerAdapterFrameGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
        return new OptimizingToJitFrameAdapterGenerator(classMethodActor, optimizingCompilerAbi);
    }

    abstract void adapt(Kind kind, AMD64EirRegister.General parameterRegister, int jitStackOffset32);

    abstract void adapt(Kind kind, int optoCompilerStackOffset32, int jitStackOffset32);

    abstract void adapt(Kind kind, AMD64EirRegister.XMM parameterRegister, int jitStackOffset32);

    protected void stackCopy(Kind kind, int sourceStackOffset, int destStackOffset) {
        // First, load into a scratch register of appropriate size for the kind, then write to memory location
        if (kind.isCategory2() || kind == Kind.WORD || kind == Kind.REFERENCE) {
            assembler().mov(scratchRegister.as64(), sourceStackOffset, stackPointer().indirect());
            assembler().mov(destStackOffset, stackPointer().indirect(), scratchRegister.as64());
        } else {
            assembler().movzxd(scratchRegister.as64(), sourceStackOffset, stackPointer().indirect());
            assembler().mov(destStackOffset, stackPointer().indirect(), scratchRegister.as32());
        }
    }

    protected void emitStackIncrease(int size) {
        if (WordWidth.signedEffective(size) == WordWidth.BITS_8) {
            assembler().subq(stackPointer(), (byte) size);
        } else {
            assembler().subq(stackPointer(), size);
        }
    }

    protected void emitStackDecrease(int size) {
        if (WordWidth.signedEffective(size) == WordWidth.BITS_8) {
            assembler().addq(stackPointer(), (byte) size);
        } else {
            assembler().addq(stackPointer(), size);
        }
    }

    protected void adaptParameter(Kind kind, EirLocation parameterLocation, int offset32) {
        switch (parameterLocation.category()) {
            case INTEGER_REGISTER:
                assert parameterLocation instanceof AMD64EirRegister.General;
                adapt(kind, (AMD64EirRegister.General) parameterLocation, offset32);
                break;
            case FLOATING_POINT_REGISTER:
                assert parameterLocation instanceof AMD64EirRegister.XMM;
                adapt(kind, (AMD64EirRegister.XMM) parameterLocation, offset32);
                break;
            case STACK_SLOT:
                assert parameterLocation instanceof EirStackSlot && parameterLocation.asStackSlot().purpose == EirStackSlot.Purpose.PARAMETER;
                adapt(kind, parameterLocation.asStackSlot().offset, offset32);
                break;
            default:
                ProgramError.unexpected();
        }
    }

    /**
     * Emit instructions for an adapter frame when calling from code produced by the JIT compiler to code produced by
     * the optimizing compiler. JIT compiler always enters the method at the
     * {@linkplain CallEntryPoint#JIT_ENTRY_POINT JIT entry point}, which is 8 bytes before the method entry point.
     */
    static class JitToOptimizingFrameAdapterGenerator extends AMD64AdapterFrameGenerator {

        JitToOptimizingFrameAdapterGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
            super(classMethodActor, optimizingCompilerAbi);
        }

        @Override
        public void emitPrologue(AMD64Assembler assembler) {
            final Directives dir = assembler.directives();
            // On entering the adapter prologue, RSP points to the top of the JIT's Java stack.

            // This instruction is initially 2 bytes long, but may expand to 5 bytes
            assembler.jmp(adapterStart);

            // Pad with bytes to make sure the alignment directive below will always yield an 8-byte long prologue,
            // regardless of the size of the jump instruction and of the alignment of the code (currently, 4-byte aligned).
            // Forcing the alignment to the next 4-byte boundary will always give an 8-byte long prologue.
            assembler.nop();
            assembler.nop();
            assembler.nop();
            dir.align(4); // align to four bytes
            assembler.bindLabel(methodEntryPoint);
        }

        @Override
        public void emitEpilogue(AMD64Assembler assembler) {
            emitFrameAdapter(assembler);
        }

        @Override
        protected void emit(Kind[] parametersKinds, EirLocation[] parameterLocations, Label adapterReturnPoint, Label methodEntryPoint) {
            if (isDynamicTrampoline(classMethodActor())) {
                emitDynamicTrampolineAdapter(methodEntryPoint);
                return;
            }
           // Computing how much space need to be allocated on the stack for the adapter. It's at least one slot for saving framePointer(), plus
            // space for out-of-band arguments.
            // The stack will look like this:
            //        <low address>  oarg(n), oarg(n-1), oarg(m), RBP, RIP, jarg(n), jarg(n -1), ... jarg0, ...  <high address>
            //                               |<--------------------------------------------->|
            //                                             adapterFrameSize
            // where "oarg" is an overflow argument and "jarg" is an argument from the caller's java stack.
            // save the caller's RBP
            final int wordSize =  Word.size();
            // Adapter frame includes space for save the JITed-callee's frame pointer (RBP)
            int overflowArgumentsSize = optimizedABI().overflowArgumentsSize(parameterLocations);
            final int adapterFrameSize = (short) optimizedABI().frameSize(0, overflowArgumentsSize + wordSize) - wordSize;

            // Allocate space on the stack (adapted parameters + caller's frame pointer)
            assert adapterFrameSize >= 0 && adapterFrameSize <= Short.MAX_VALUE;
            assembler().enter((short) adapterFrameSize, (byte) 0);

             // Prefix of a frame is RIP + saved RBP.
            final int framePrefixSize = 2 * wordSize;
            // On entry to the adapter, the top of the stack contains the RIP. The last argument on the stack is
            // immediately above the RIP.
            // We set an offset to that last argument (relative to the new stack pointer) and iterate over the arguments
            // in reverse order,
            // from last to first.
            // This avoids computing the size of the arguments to get the offset to the first argument.
            int jitCallerStackOffset = adapterFrameSize + framePrefixSize;

            for (int i = parameterLocations.length - 1; i >= 0;  i--) {
                adaptParameter(parametersKinds[i], parameterLocations[i], jitCallerStackOffset);
                jitCallerStackOffset += JitStackFrameLayout.stackSlotSize(parametersKinds[i]);
            }
            // jitCallerOffset is now set to the first location before the first parameter, i.e., the point where
            // the caller stack will retract to.
            assembler().call(methodEntryPoint);
            assembler().bindLabel(adapterReturnPoint);
            final int jitCallArgumentSize = jitCallerStackOffset - (adapterFrameSize + framePrefixSize);
            if (adapterFrameSize != 0) {
                emitStackDecrease(adapterFrameSize);
            }
            assembler().pop(framePointer());
            assert WordWidth.signedEffective(jitCallArgumentSize).lessEqual(WordWidth.BITS_16);
            // Retract the stack pointer back to its position before the first argument on the caller's stack.
            assembler().ret((short) jitCallArgumentSize);
        }

        private void emitDynamicTrampolineAdapter(Label methodEntryPoint) {
            final byte wordSize =  (byte) Word.size();
            assembler().enter((short) 0, (byte) 0);
            assembler().call(methodEntryPoint);
            // Restore caller's frame pointer.
            assembler().mov(framePointer(), wordSize, stackPointer().indirect());
            assembler().ret(wordSize);
        }

        @Override
        void adapt(Kind kind, AMD64EirRegister.General destinationRegister, int offset32) {
            switch (kind.asEnum) {
                case BYTE: {
                    assembler().movsxb(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                case BOOLEAN: {
                    assembler().movzxb(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                case SHORT: {
                    assembler().movsxw(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                case CHAR: {
                    assembler().movzxw(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                case INT: {
                    assembler().movsxd(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                case LONG:
                case WORD:
                case REFERENCE: {
                    assembler().mov(destinationRegister.as64(), offset32, stackPointer().indirect());
                    break;
                }
                default: {
                    ProgramError.unexpected();
                }
            }
        }

        @Override
        void adapt(Kind kind, AMD64EirRegister.XMM destinationRegister, int offset32) {
            switch (kind.asEnum) {
                case FLOAT:
                    assembler().movss(destinationRegister.as(), offset32, stackPointer().indirect());
                    break;
                case DOUBLE:
                    assembler().movsd(destinationRegister.as(), offset32, stackPointer().indirect());
                    break;
                default:
                    ProgramError.unexpected();
            }
        }

        @Override
        void adapt(Kind kind, int optoCompilerStackOffset32, int jitStackOffset32) {
            // we need to adjust the stack pointer offset by one word because in the adapter code, the return address
            // has not been pushed yet
            stackCopy(kind, jitStackOffset32 /* = source */, optoCompilerStackOffset32 /* = dest */);
        }
    }

    static class OptimizingToJitFrameAdapterGenerator extends AMD64AdapterFrameGenerator {
        private int adapterFrameSize = 0;

        OptimizingToJitFrameAdapterGenerator(MethodActor classMethodActor, EirABI optimizingCompilerAbi) {
            super(classMethodActor, optimizingCompilerAbi);
        }

        @Override
        public void emitPrologue(AMD64Assembler assembler) {
        }

        @Override
        public void emitEpilogue(AMD64Assembler assembler) {
            emitFrameAdapter(assembler);
        }

        /**
         * Emit instructions to adapt calls from optimized code to JIT code. This just pushes the register arguments on
         * the stack, followed by the memory arguments building a stack of arguments as expected by the JIT. A call to
         * the entry point is then made.
         */
        @Override
        protected void emit(Kind[] parametersKinds, EirLocation[] parameterLocations, Label adapterReturnPoint, Label methodEntryPoint) {
            if (parameterLocations.length > 0) {
                // only emit an adapter frame for JIT methods that take arguments.
                // On entry to the adapter, the stack pointer points to the stack slot holding the RIP.
                // Arguments are in registers, while overflow parameters are on the stack.
                // The adapter frame constructs a JIT-like operand stack and then calls the main method entrypoint.
                final int parameterSize = JitStackFrameLayout.parametersFrameSize(parametersKinds);
                // align the adapter frame to the target ABI's stack pointer alignment
                adapterFrameSize = optimizedABI().targetABI().alignFrameSize(parameterSize);
                final int alignmentAdjustment = adapterFrameSize - parameterSize;

                // allocate space on the stack for storing the parameters and alignment
                emitStackIncrease(adapterFrameSize);
                int stackOffset = 0;
                // emit the parameters in reverse order.
                for (int i = parameterLocations.length - 1; i >= 0; i--) {
                    adaptParameter(parametersKinds[i], parameterLocations[i], stackOffset);
                    stackOffset += JitStackFrameLayout.stackSlotSize(parametersKinds[i]);
                }

                assembler().call(methodEntryPoint);
                assembler().bindLabel(adapterReturnPoint);

                // calling the JIT entrypoint will return with the parameters popped off of the stack by the callee
                // thus, only the alignment space, if any, remains.
                if (alignmentAdjustment > 0) {
                    emitStackDecrease(alignmentAdjustment);
                }
                assembler().ret();
            }
            assembler().bindLabel(methodEntryPoint);
        }

        @Override
        void adapt(Kind kind, AMD64EirRegister.General parameterRegister, int offset32) {
            switch (kind.asEnum) {
                case BYTE:
                case BOOLEAN:
                case SHORT:
                case CHAR:
                case INT: {
                    // because we are writing into a local, we always need to move the whole register.
                    assembler().mov(offset32, stackPointer().indirect(), parameterRegister.as32());
                    break;
                }
                case LONG:
                case WORD:
                case REFERENCE: {
                    assembler().mov(offset32, stackPointer().indirect(), parameterRegister.as64());
                    break;
                }
                default: {
                    ProgramError.unexpected();
                }
            }
        }

        @Override
        void adapt(Kind kind, AMD64EirRegister.XMM parameterRegister, int offset32) {
            switch (kind.asEnum) {
                case FLOAT:
                    assembler().movss(offset32, stackPointer().indirect(), parameterRegister.as());
                    break;
                case DOUBLE:
                    assembler().movsd(offset32, stackPointer().indirect(), parameterRegister.as());
                    break;
                default:
                    ProgramError.unexpected();
            }
        }

        @Override
        void adapt(Kind kind, int optoCompilerStackOffset32, int jitStackOffset32) {
            // Add word size to take into account the slot used by the RIP of the caller
            stackCopy(kind, adapterFrameSize + optoCompilerStackOffset32 + Word.size() /* = source */, jitStackOffset32 /* = dest */);
        }
    }
}

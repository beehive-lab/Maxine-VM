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
/*VCSID=f281c8f4-bc6c-4082-8ea6-4b1ba2e3e1f0*/
package com.sun.max.vm.jit.sparc;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.b.c.d.e.sparc.target.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.VMRegister.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.thread.*;

/**
 * Template-based implementation of JIT compiler for SPARC.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public class SPARCJitCompiler extends JitCompiler {
    private static GPR _jitFramePointerRegister;
    private static GPR _jitLiteralBaseRegister;

    private final SPARCTemplateBasedTargetGenerator _targetGenerator;

    public SPARCJitCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _targetGenerator = new SPARCTemplateBasedTargetGenerator(this);
    }

    public SPARCJitCompiler(VMConfiguration vmConfiguration, TemplateTable templateTable) {
        this(vmConfiguration);
        _targetGenerator.initializeTemplateTable(templateTable);
    }

    @Override
    protected TemplateBasedTargetGenerator targetGenerator() {
        return _targetGenerator;
    }

    /**
     * Unwinds a thread's stack to an exception handler in JITed code. We have to unwind first to a stub that uses as
     * its frame the frame of the closest JIT -> OPT frame adapter or JIT -> OPT call. The exit stub then re-establishes
     * the stack pointer to that of the JITed frame that catches the exception.
     *
     * We need this two-step unwinding for the following reason: a JIT-frame may not have a register window on its own.
     * It is shared with all the JIT frames between an opt->jit and a jit->opt adapter (or jit->runtime call). The
     * register window keeps moving with the stack pointer. If we return directly to the JIT frame, the saved register
     * window for the JIT frame is lost (the stack pointer of the JIT frame doesn't point to where the register window
     * was saved) and the first attempt to restore the window will load arbitrary values in the registers (with whatever
     * is at the top of the stack). So we have first to return to a frame with a stack pointer that points to a register
     * window, then set the stack pointer to where the actual top of the stack is for the JIT frame where the exception
     * is caught.
     *
     * <p>
     * The critical state of the registers before the RET instruction is:
     * <ul>
     * <li>%i0 must hold the address of the register window to be restored</li>
     * <li>%i7 must hold the unwind stub address.</li>
     * <li>%i6 must hold the stack pointer of the handler</li>
     * </ul>
     *
     * @param context the stack unwinding context
     * @param catchAddress the address of the exception handler (actually, the dispatch code)
     * @param stackPointer the stack pointer for the exception handler
     * @param framePointer the frame pointer for exception handler
     */
    @NEVER_INLINE
    private static Address unwind(SPARCStackUnwindingContext context, Address catchAddress, Pointer stackPointer, Pointer framePointer, Pointer literalBase) {
        SpecialBuiltin.flushRegisterWindows();
        final Address returnAddress = _stackUnwindStub.start();
        final Pointer unwindStubFramePointer = context.isTopFrame() ? context.stackPointer() : context.framePointer();

        // Fix the local registers in the register window of the catcher. These will be used by the unwind stub to set the catcher's frame context.
        SPARCStackFrameLayout.setRegisterInSavedWindow(unwindStubFramePointer, GPR.L0, catchAddress);
        SPARCStackFrameLayout.setRegisterInSavedWindow(unwindStubFramePointer, GPR.L1, stackPointer);
        SPARCStackFrameLayout.setRegisterInSavedWindow(unwindStubFramePointer, _jitLiteralBaseRegister,  literalBase);
       // Patch the frame pointer in the register window to that of the JIT frame we'll unwind to.
        SPARCStackFrameLayout.setRegisterInSavedWindow(unwindStubFramePointer, _jitFramePointerRegister,  framePointer);

        // Save caller instruction pointer in call address register. This is only to help inspector figuring out what's the
        // call stack once we've smashed our return address with that of the unwind stub.
        // Other stack walkers should care much as this stack unwinding business is atomic to them.
        final Pointer callerInstructionPointer = VMRegister.getCallAddressRegister();
        SpecialBuiltin.setIntegerRegister(Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS, callerInstructionPointer);
        // Set our own return register so we return to stack unwind stub.
        VMRegister.setCallAddressRegister(returnAddress);
        return unwindStubFramePointer;
    }

    private boolean walkAdapterFrame(StackFrameWalker stackFrameWalker, TargetMethod targetMethod, Purpose purpose, Object context, boolean isTopFrame) {
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
        // The frame pointer read off the JIT frame called by this adapter is unbiased.
        final Pointer framePointer;
        final Pointer stackPointer;
        final int adapterTopFrameSize =  SPARCAdapterFrameGenerator.optimizedToJitAdapterFrameSize(targetMethod);

        final int adapterFrameSize =  isTopFrame ? adapterTopFrameSize :  adapterTopFrameSize - SPARCStackFrameLayout.minStackFrameSize();

        //final boolean inCallerRegisterWindow = instructionPointer.equals(optimizedEntryPoint);
        final boolean inCallerRegisterWindow = BcdeTargetSPARCCompiler.inCallerRegisterWindow(instructionPointer, optimizedEntryPoint, adapterTopFrameSize);

        if (inCallerRegisterWindow) {
            // The save instruction hasn't been executed. The frame pointer is the same as the caller's stack pointer.
            // We need to compute the stack pointer for this frame
            framePointer =  stackFrameWalker.stackPointer();
            stackPointer = framePointer.minus(adapterFrameSize);
        } else {
            framePointer = isTopFrame ?  stackFrameWalker.framePointer() : STACK_BIAS.SPARC_V9.bias(stackFrameWalker.framePointer());
            stackPointer =  stackFrameWalker.stackPointer();
        }

        switch (purpose) {
            case EXCEPTION_HANDLING: {
                // cannot have exception handler in adapter frame
                break;
            }
            case REFERENCE_MAP_PREPARING: {
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                final StackFrame stackFrame = new SPARCOptimizedToJitAdapterFrame(stackFrameWalker.calleeStackFrame(), targetMethod, instructionPointer, framePointer, stackPointer, adapterFrameSize);
                if (!stackFrameVisitor.visitFrame(stackFrame)) {
                    return false;
                }
                break;
            }
            default: {
                ProgramError.unknownCase();
            }
        }

        final Pointer callerFramePointer;
        final Pointer callerInstructionPointer;
        final Pointer callerStackPointer;

        if (isTopFrame) {
            // We're in the top frame. The bottom of the frame comprises the register window's saving areas. We can
            // retrieve values from there.
            if (inCallerRegisterWindow) {
                callerInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(targetMethod.abi()).asPointer();
                callerFramePointer = stackFrameWalker.framePointer();
                callerStackPointer =  stackFrameWalker.stackPointer();
            } else {
                callerInstructionPointer = SPARCStackFrameLayout.getCallerPC(stackFrameWalker);
                callerFramePointer = SPARCStackFrameLayout.getCallerFramePointer(stackFrameWalker);
                callerStackPointer = framePointer;
            }
        } else {
            final int ripSaveAreaOffset =  SPARCJitStackFrameLayout.OFFSET_TO_FLOATING_POINT_TEMP_AREA - Word.size();
            final Pointer ripSaveArea = framePointer.plus(ripSaveAreaOffset);
            // Get caller's instruction pointer from the rip save area in the adapter frame.
            callerInstructionPointer = stackFrameWalker.readWord(ripSaveArea, 0).asPointer();
            // We can obtain the caller's frame pointer from its register window saving area, which is at the
            // adapter frame's frame pointer.
            final Pointer unbiasedFramePointer = stackFrameWalker.framePointer();
            callerFramePointer = SPARCStackFrameLayout.getCallerFramePointer(stackFrameWalker, unbiasedFramePointer);
            callerStackPointer = framePointer;
        }

        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);

        return true;
    }

    private static Pointer getCallerInstructionPointer(FRAME_STATE frameState, StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
        final Pointer callerInstructionPointer;
        if (frameState.isReturnInstructionPointerOnStack()) {
            final Pointer returnInstructionPointer = frameState.returnInstructionPointer(stackFrameWalker, targetMethod);
            callerInstructionPointer = stackFrameWalker.readWord(returnInstructionPointer, 0).asPointer();
        } else {
            callerInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(targetMethod.abi()).asPointer();
        }
        return callerInstructionPointer;
    }

    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, Purpose purpose, Object context) {
        assert targetMethod instanceof SPARCJitTargetMethod;
        final SPARCJitTargetMethod jitTargetMethod = (SPARCJitTargetMethod) targetMethod;
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final Pointer jitEntryPoint = JIT_ENTRY_POINT.in(targetMethod);
        final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
        final boolean hasAdapterFrame = !jitEntryPoint.equals(optimizedEntryPoint);

        if (hasAdapterFrame) {
            final Pointer endOfAdapter = optimizedEntryPoint.plus(jitTargetMethod.optimizedCallerAdapterFrameCodeSize());

            if (instructionPointer.greaterEqual(optimizedEntryPoint) && instructionPointer.lessThan(endOfAdapter)) {
                return walkAdapterFrame(stackFrameWalker, targetMethod, purpose, context, isTopFrame);
            }
        }
        // In JITed code.
        if (isTopFrame) {
            // Make sure the appropriate ABI is used to obtain the top frame's stack and frame pointers.
            stackFrameWalker.useABI(targetMethod.abi());
        }

        final FRAME_STATE frameState = stackFrameState(stackFrameWalker, jitTargetMethod);
        final Pointer localVariablesBase = frameState.localVariablesBase(stackFrameWalker, jitTargetMethod);

        switch (purpose) {
            case REFERENCE_MAP_PREPARING: {
                Problem.unimplemented();
                break;
            }
            case EXCEPTION_HANDLING: {
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(instructionPointer);
                if (!catchAddress.isZero()) {
                    final SPARCStackUnwindingContext unwindingContext = UnsafeLoophole.cast(context);
                    final Pointer stackPointer = stackFrameWalker.stackPointer();
                    if (!(unwindingContext._throwable instanceof StackOverflowError) || VmThread.current().hasSufficentStackToReprotectGuardPage(stackPointer)) {
                        // The Java operand stack of the method that handles the exception is always cleared before pushing the
                        // thrown object.
                        // Compute the offset to the first stack slot of the Java Stack: frame pointer -
                        // (space for non-local parameters + saved literal base (1 slot) + space of the first slot itself).
                        final int offsetToFirstOperandStackSlot = jitTargetMethod.stackFrameLayout().sizeOfNonParameterLocals() + 2 * JitStackFrameLayout.JIT_SLOT_SIZE;
                        final Pointer catcherTopOfStackPointer = localVariablesBase.minus(offsetToFirstOperandStackSlot);
                        // Push the exception on top of the stack first
                        catcherTopOfStackPointer.writeReference(0, Reference.fromJava(unwindingContext._throwable));

                        // Compute the catcher stack pointer: this one will be the top frame, so we need to augment it with space for saving a register window plus
                        // mandatory output register. We also need to bias it.
                        final Pointer catcherStackPointer = STACK_BIAS.JIT_SPARC_V9.bias(catcherTopOfStackPointer.minus(SPARCStackFrameLayout.minStackFrameSize()));
                        final Pointer literalBase = localVariablesBase.readWord(-JitStackFrameLayout.STACK_SLOT_SIZE).asPointer();

                        // found an exception handler, and thus we are done with the stack walker
                        stackFrameWalker.reset();

                        // Completes the exception handling protocol (with respect to the garbage collector) initiated in
                        // Throwing.raise()
                        Safepoint.enable();

                        unwind(unwindingContext, catchAddress, catcherStackPointer, localVariablesBase, literalBase);
                        // We should never reach here
                    }
                }
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                final StackFrame stackFrame = new SPARCJitStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod,
                                stackFrameWalker.instructionPointer(), stackFrameWalker.stackPointer(), localVariablesBase, localVariablesBase);
                if (!stackFrameVisitor.visitFrame(stackFrame)) {
                    return false;
                }
                break;
            }
        }

        final Pointer callerInstructionPointer = getCallerInstructionPointer(frameState, stackFrameWalker, jitTargetMethod);
        final Pointer callerStackPointer = frameState.callerStackPointer(stackFrameWalker, jitTargetMethod);
        final Pointer callerFramePointer = frameState.callerFramePointer(stackFrameWalker, jitTargetMethod);
        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);

        return true;
    }

    private FRAME_STATE stackFrameState(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
        if (instructionPointer.lessThan(optimizedEntryPoint)) {
            return FRAME_STATE.IN_CALLER_FRAME;
        }
        // The stack frame of JITed code is setup by a small sequence of instructions called the frame builder below.
        // The frame builder starts at the end of the optimized-to-jit adapter. Its size depends on the frame size.
        // Hence this code to figure out the boundaries of the frame builder.
        final Pointer startOfFrameBuilder = optimizedEntryPoint.plus(targetMethod.optimizedCallerAdapterFrameCodeSize());
        final int builderSize = BytecodeToSPARCTargetTranslator.frameBuilderSize(targetMethod);
        final Pointer endOfFrameBuilder = startOfFrameBuilder.plus(builderSize);

        if (instructionPointer.greaterEqual(endOfFrameBuilder)) {
            final int currentInstruction = stackFrameWalker.readInt(instructionPointer, 0);
            final int prevInstruction = stackFrameWalker.readInt(instructionPointer.minus(SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH), 0);
            if (currentInstruction == BytecodeToSPARCTargetTranslator.RET_TEMPLATE || prevInstruction ==  BytecodeToSPARCTargetTranslator.RET_TEMPLATE) {
                return FRAME_STATE.EXITING_CALLEE;
            }
            return FRAME_STATE.NORMAL;
        }
        // We're in the frame builder
        // If the target method's frame size is large, the ABI frame pointer is change by the second instruction only.
        // So we're still in IN_CALLER_FRAME state if we haven't passed the first two instructions of the frame builder.
        if (!SPARCAssembler.isSimm13(targetMethod.frameSize())) {
            if (instructionPointer.equals(startOfFrameBuilder) ||
                            instructionPointer.equals(startOfFrameBuilder.plus(SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH))) {
                return FRAME_STATE.IN_CALLER_FRAME;
            }
        }
        // We're building the frame. The frame pointer is already set up (can be obtained off the ABI frame pointer register).
        return FRAME_STATE.BUILDING_CALLEE_FRAME;
    }

    /**
     *
     *
     */
    enum FRAME_STATE {
        /**
         * Normal state. Frame pointer is in the Frame Pointer register defined by the ABI. Caller's address and frame pointer are on the stack,
         * just above the template slots. Stack pointer is just the ABI stack pointer.
         */
        NORMAL {
            @Override
            boolean isReturnInstructionPointerOnStack() {
                return true;
            }
            @Override
            Pointer localVariablesBase(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                return stackFrameWalker.framePointer();
            }

            @Override
            Pointer returnInstructionPointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {

                // The RIP is the top slot in the caller save
                // area, so we have to remove a stack slot to the computed size.
                final int dispToRip = offsetToTopOfFrame(targetMethod) -  JitStackFrameLayout.STACK_SLOT_SIZE;
                return stackFrameWalker.framePointer().plus(dispToRip);
            }

            @Override
            Pointer callerFramePointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                return stackFrameWalker.readWord(returnInstructionPointer(stackFrameWalker, targetMethod), -Word.size()).asPointer();
            }
        },
        /**
         * State when entering the method. The callee's frame isn't allocated yet. The Frame Pointer register is still set to the caller's.
         * The callee's frame pointer can be computed from the caller's Stack Pointer and the called method's frame size.
         */
        IN_CALLER_FRAME {

            @Override
            Pointer localVariablesBase(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                //  We just need to subtract the offset to the top of the frame from the frame pointer.
                final int offsetToCalleeFramePointer = offsetToTopOfFrame(targetMethod);
                return stackFrameWalker.stackPointer().minus(offsetToCalleeFramePointer);
            }
            @Override
            Pointer callerStackPointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                return stackFrameWalker.stackPointer();
            }
        },
        /**
         * State when building the stack frame on entering the method. The callee's frame is allocated, but the frame pointer isn't setup.
         * Further, the call save area may not be initialized.
         * The callee's frame pointer can be computed from the callee's stack pointer.
         */
        BUILDING_CALLEE_FRAME {
            @Override
            Pointer localVariablesBase(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                final int offsetToCalleeFramePointer = targetMethod.stackFrameLayout().sizeOfNonParameterLocals()  +
                    SPARCStackFrameLayout.offsetToFirstFreeSlotFromStackPointer();
                return stackFrameWalker.stackPointer().plus(offsetToCalleeFramePointer);
            }
       },

        EXITING_CALLEE{
            @Override
            Pointer localVariablesBase(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
                // The following assume that on exiting, the operand stack is empty.
                // This is not crucial as this is only used for inspection.
                // A better approach would be to read the callee frame pointer directly off the O5 register.
                final int offsetToCalleeFramePointer =   targetMethod.stackFrameLayout().sizeOfNonParameterLocals();
                return stackFrameWalker.stackPointer().plus(offsetToCalleeFramePointer);
            }
        };

        abstract Pointer localVariablesBase(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod);

        Pointer returnInstructionPointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
            ProgramError.unexpected("Must call returnInstructionPointer only when in normal frame state");
            return null;
        }

        Pointer callerFramePointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
            return stackFrameWalker.framePointer();
        }
        boolean isReturnInstructionPointerOnStack() {
            return false;
        }

        Pointer callerStackPointer(StackFrameWalker stackFrameWalker, SPARCJitTargetMethod targetMethod) {
            return stackFrameWalker.stackPointer().plus(targetMethod.stackFrameLayout().frameSize());
        }

        /**
         * Return the offset to the top of the frame from the frame pointer.
         * @param targetMethod
         * @return an offset in byte
         */
        int offsetToTopOfFrame(SPARCJitTargetMethod targetMethod) {
            // The frame pointer points at the base of the template slots.
            // The caller save area is just above the template slots. There might be padding between the template slots and the call area size to
            // align the frame to the required boundary.
            final int unalignedSize = targetMethod.stackFrameLayout().sizeOfTemplateSlots() + SPARCJitStackFrameLayout.CALL_SAVE_AREA_SIZE;
            return targetMethod.abi().alignFrameSize(unalignedSize);
        }
    }

    public void advance(StackFrameWalker stackFrameWalker, Word instructionPointer, Word stackPointer, Word framePointer) {
        stackFrameWalker.advance(instructionPointer, stackPointer, framePointer);
    }

    @Override
    public StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        return new SPARCStackUnwindingContext(stackPointer, framePointer, throwable);
    }

    static class StackUnwindStub extends RuntimeStub {
        static byte [] createStubCode(VMConfiguration vmConfiguration) {
            try {
                final SPARCAssembler asm =  SPARCAssembler.createAssembler(vmConfiguration.wordWidth());
                // FIXME: the two nops are just to keep the invariant that the caller address is at 2 instructions after
                // the current pc. Thus we can use StackUnwindStub.start() as the address where to return to on an unwind.
                asm.nop();
                asm.nop();
                // O0 holds the pointer to the saved register window of the jited frame.
                asm.mov(GPR.O0, GPR.I6);
                // Restore the window
                asm.restore();
                // We have now the catcher's address in L0 and its stack pointer in L1. The frame pointer is already setup in L6, and the
                // base literal register is already setup.
                // Catcher address now in O0.
                asm.jmp(GPR.L0, GPR.G0);
                asm.mov(GPR.L1, GPR.O6);
                return asm.toByteArray();
            } catch (AssemblyException e) {
                FatalError.unexpected("Failed to create stack unwind stub");
                return null;
            }
        }

        private final Address _jumpAddress;
        private final TargetABI _callerTargetABI;

        StackUnwindStub(VMConfiguration vmConfiguration) {
            super(createStubCode(vmConfiguration));
            _jumpAddress = start().plus(4 * SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH);
            _callerTargetABI = vmConfiguration.targetABIsScheme().jitABI();
        }

        @Override
        public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, Purpose purpose, Object context) {
            FatalError.check(purpose == Purpose.INSPECTING, "Cannot walk stack unwind stub unless inspecting");
            final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
            final Pointer instructionPointer = stackFrameWalker.instructionPointer();
            final Pointer callerInstructionPointer;
            final Pointer callerStackPointer = stackFrameWalker.framePointer();
            final Pointer callerFramePointer;
            if (!isTopFrame) {
                // The stack unwind stub is always a top frame. When this is not the case, it's because the unwind method has just patched it's
                // return address with this. In this case, the real instruction pointer for this frame is saved in the frame-less call address register.
                // We just set the stack frame walker to the real frame and return.
                final Pointer realInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(_callerTargetABI).asPointer();
                stackFrameWalker.advance(realInstructionPointer, stackFrameWalker.stackPointer(), stackFrameWalker.framePointer());
                return true;
            }

            if (instructionPointer.lessThan(_jumpAddress)) {
                callerInstructionPointer = SPARCStackFrameLayout.getReturnAddress(stackFrameWalker);
                callerFramePointer = SPARCStackFrameLayout.getCallerFramePointer(stackFrameWalker);
            } else {
                // We're in the exception catcher's register windows. Get return address and frame pointer directly from the register
                callerInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(_callerTargetABI).asPointer();
                // Reload stack and frame pointer from registers specified by the JIT abi.
                stackFrameWalker.useABI(_callerTargetABI);
                callerFramePointer = stackFrameWalker.framePointer();
            }
            final StackFrame stackFrame = new RuntimeStubStackFrame(stackFrameWalker.calleeStackFrame(), this,
                            stackFrameWalker.instructionPointer(), stackFrameWalker.stackPointer(), stackFrameWalker.framePointer());

            if (!stackFrameVisitor.visitFrame(stackFrame)) {
                return false;
            }

            stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);
            return true;
        }

        @Override
        public String name() {
            return "stack unwind stub";
        }
    }
    private static RuntimeStub _stackUnwindStub;

    public static RuntimeStub unwindStub() {
        return _stackUnwindStub;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.STARTING) {
            final TargetABI jitABI = vmConfiguration().targetABIsScheme().jitABI();
            _jitFramePointerRegister = (GPR) jitABI.framePointer();
            _jitLiteralBaseRegister = (GPR) jitABI.literalBaseRegister();
            _stackUnwindStub = new StackUnwindStub(vmConfiguration());
        }
    }
}

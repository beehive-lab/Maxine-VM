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
package com.sun.max.vm.compiler.b.c.d.e.sparc.target;

import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.asm.sparc.complete.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.d.e.sparc.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.sparc.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.runtime.sparc.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.stack.sparc.*;
import com.sun.max.vm.thread.*;

/**
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class BcdeTargetSPARCCompiler extends BcdeSPARCCompiler implements TargetGeneratorScheme {

    private final SPARCEirToTargetTranslator _eirToTargetTranslator;
    /**
     * Shortcut to the jit frame pointer. Used for walking adapter frames.
     */
    private final GPR _jitFramePointer;

    protected SPARCEirToTargetTranslator createTargetTranslator() {
        return new SPARCEirToTargetTranslator(this);
    }

    public BcdeTargetSPARCCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _jitFramePointer = (GPR) vmConfiguration.targetABIsScheme().jitABI().framePointer();
        _eirToTargetTranslator = new SPARCEirToTargetTranslator(this);
    }

    public TargetGenerator targetGenerator() {
        return _eirToTargetTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return _eirToTargetTranslator;
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), irGenerator());
    }

    private EirABI optimizedABI() {
        return eirGenerator().eirABIsScheme().javaABI();
    }

    public TargetABI javaTargetABI() {
        return optimizedABI().targetABI();
    }

    private static final int CALL_INSTRUCTION = 0x40000000;
    private static final int CALL_DISP_MASK = 0x3fffffff;

    /**
     * This is a snippet implementation and we want to access the snippet's caller via our stack pointer.
     * So we eliminate one call layer around the body of this method by making inlining into the snippet mandatory.
     *
     * @see StaticTrampoline
     */
    @NEVER_INLINE
    @Override
    public void staticTrampoline() {
        // Walk the stack frame to find the instruction calling the trampoline
        final StaticTrampolineContext context = new StaticTrampolineContext();
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VMRegister.getInstructionPointer(),
                        VMRegister.getCpuStackPointer(),
                        VMRegister.getCpuFramePointer(),
                        context);
        final Pointer callSite = context.instructionPointer();
        // Get the target method that calls the static trampoline
        final TargetMethod caller = Code.codePointerToTargetMethod(callSite);
        // We can now search the caller for the ClassMethodActor corresponding to the direct call.
        final ClassMethodActor callee = caller.callSiteToCallee(callSite);
        // Compile the callee, and use the caller's abi to get the correct entry point.
        final Address calleeEntryPoint = CompilationScheme.Static.compile(callee, caller.abi().callEntryPoint(), CompilationDirective.DEFAULT);
        // Compute offset to the callee from the caller
        final int calleeOffset = calleeEntryPoint.minus(callSite).toInt();
        // Compute the 30 bits field displacement for the call instruction (see SPARC manual, A.8).
        final int disp30 = (calleeOffset >> 2) & CALL_DISP_MASK;
        final int instr = CALL_INSTRUCTION | disp30;
        callSite.writeInt(0, instr);
    }

    @Override
    public Word createInitialVTableEntry(int vTableIndex, VirtualMethodActor dynamicMethodActor) {
        return  vmConfiguration().trampolineScheme().makeVirtualCallEntryPoint(vTableIndex);
    }

    @Override
    public Word createInitialITableEntry(int iIndex, VirtualMethodActor dynamicMethodActor) {
        return  vmConfiguration().trampolineScheme().makeInterfaceCallEntryPoint(iIndex);
    }

    public void patchCallSite(TargetMethod targetMethod, int callOffset, Word callEntryPoint) {
        final Pointer callSite = targetMethod.codeStart().plus(callOffset).asPointer();
        final Label label = new Label();
        SPARCAssembler assembler;
        if (vmConfiguration().platform().processorKind().dataModel().wordWidth().equals(WordWidth.BITS_64)) {
            final SPARC64Assembler asm = new SPARC64Assembler(callSite.toLong());
            asm.fixLabel(label, callEntryPoint.asAddress().toLong());
            assembler = asm;
        } else {
            final SPARC32Assembler asm = new SPARC32Assembler(callSite.toInt());
            asm.fixLabel(label, callEntryPoint.asAddress().toInt());
            assembler = asm;
        }
        assembler.call(label);
        try {
            final byte[] code = assembler.toByteArray();
            Bytes.copy(code, 0, targetMethod.code(), callOffset, code.length);
        } catch (AssemblyException assemblyException) {
            ProgramError.unexpected("patching call site failed", assemblyException);
        }
    }

    /**
     * Unwinds a thread's stack to an exception handler.
     * <p>
     * The critical state of the registers before the RET instruction is:
     * <ul>
     * <li> %i0 must hold the exception object</li>
     * <li> %i7 must hold the catch address, minus 8 -- the ret instruction assume %i7 holds the address of a call instruction and always jump to %i7 + 2 instructions.
     * So we have pass it the catch address - 2 instructions.</li>
     * <li> %i6 must hold the stack pointer of the handler</li>
     * The register windows must be flushed before unwinding so all register windows are cleaned.
     * </ul>
     *
     * @param throwable
     *                the exception object
     * @param catchAddress
     *                the address of the handler code (actually the dispatcher code)
     * @param stackPointer
     *                the stack pointer denoting the frame of the handler to which the stack is unwound upon returning
     *                from this method
     */
    @NEVER_INLINE
    private static Throwable unwind(Throwable throwable, Address catchAddress, Pointer stackPointer) {
        SpecialBuiltin.flushRegisterWindows();
        VMRegister.setCallAddressRegister(catchAddress.minus(SPARCStackFrameLayout.OFFSET_TO_RETURN_PC));
        VMRegister.setAbiFramePointer(stackPointer);
        return throwable;
    }

    private boolean walkAdapterFrame(StackFrameWalker stackFrameWalker, TargetMethod targetMethod, Purpose purpose, Object context, Pointer startOfAdapter, boolean isTopFrame) {
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final int adapterFrameSize = SPARCAdapterFrameGenerator.jitToOptimizedAdapterFrameSize(stackFrameWalker, startOfAdapter);
        final Pointer stackPointer = stackFrameWalker.stackPointer();

        switch(purpose) {
            case EXCEPTION_HANDLING: {
                assert !isTopFrame;
                // Record this JIT -> OPT adapter frame.
                final SPARCStackUnwindingContext unwindingContext = UnsafeLoophole.cast(context);
                unwindingContext.record(stackPointer, stackFrameWalker.framePointer());
                break;
            }
            case REFERENCE_MAP_PREPARING: {
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                final StackFrame stackFrame = new SPARCJitToOptimizedAdapterFrame(stackFrameWalker.calleeStackFrame(), targetMethod, instructionPointer, stackFrameWalker.framePointer(), stackPointer, adapterFrameSize);
                if (!stackFrameVisitor.visitFrame(stackFrame)) {
                    return false;
                }
                break;
            }
            default: {
                ProgramError.unknownCase();
            }
        }
        // Jit to Opt adapter frame exploit the use of register windows by optimized code to leave the frame pointer
        //  of the jited caller in its local register and save the return address in another one (SAVED_CALLER_ADDRESS).
        // This avoids writing these to the stack then reloading them.
        // The saving of the return address register takes place at the JIT entry point, so that by the first instruction
        // of the adapter, the return address is in SAVED_CALLER_ADDRESS.
        // Thus, when in the entry point, the return address can be found in %o7, and when in the adapter it can be found
        // in SAVED_CALLER_ADDRESS. The caller frame pointer is always in the local register defined by the JIT abi (_jitFramePointer).
        final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
        final Pointer callerFramePointer = SPARCStackFrameLayout.getRegisterInSavedWindow(stackFrameWalker, _jitFramePointer).asPointer();
        final Pointer callerStackPointer;
        final Pointer callerInstructionPointer;
        if (instructionPointer.greaterThan(optimizedEntryPoint)) {
            // We're past the jit entry point. The return address has been saved in the local register L1 which can be obtained in the register window's saved area.
            callerInstructionPointer = SPARCStackFrameLayout.getRegisterInSavedWindow(stackFrameWalker, SPARCAdapterFrameGenerator.SAVED_CALLER_ADDRESS).asPointer();
        } else {
            callerInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(targetMethod.abi()).asPointer();
        }
        if (adapterFrameSize > 0 && instructionPointer.greaterThan(startOfAdapter)) {
            callerStackPointer = stackPointer.plus(adapterFrameSize);
        } else {
            callerStackPointer = stackPointer;
        }
        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);
        return true;
    }

    private static boolean inAdapterFrameCode(boolean inTopFrame, final Pointer instructionPointer, final Pointer optimizedEntryPoint, final Pointer startOfAdapter) {
        if (instructionPointer.lessThan(optimizedEntryPoint)) {
            return true;
        }
        // Currently, when walking stack frame, the instruction pointer always points at the call instruction.
        // So whether we on the top frame or not, we have the precise location of the call instruction in the instruction pointer
        // (that's different from what's done for AMD64)
        return instructionPointer.greaterEqual(startOfAdapter);
    }

    public static boolean inCallerRegisterWindow(Pointer instructionPointer, Pointer entryPoint, int frameSize) {
        final int frameBuilderSize = SPARCStackFrameLayout.SPARC_INSTRUCTION_WIDTH * (SPARCEirPrologue.numberOfFrameBuilderInstructions(frameSize));
        return instructionPointer.lessThan(entryPoint.plus(frameBuilderSize));
    }

    @Override
    public boolean walkFrame(StackFrameWalker stackFrameWalker, boolean isTopFrame, TargetMethod targetMethod, Purpose purpose, Object context) {
        final Pointer instructionPointer = stackFrameWalker.instructionPointer();
        final Pointer entryPoint;
        if (targetMethod.abi().callEntryPoint().equals(C_ENTRY_POINT)) {
            // Simple case (no adapter)
            entryPoint = C_ENTRY_POINT.in(targetMethod);
        } else {
            // we may be in an adapter
            final Pointer optimizedEntryPoint = OPTIMIZED_ENTRY_POINT.in(targetMethod);
            final Pointer jitEntryPoint = JIT_ENTRY_POINT.in(targetMethod);
            final boolean hasAdapterFrame = !(jitEntryPoint.equals(optimizedEntryPoint)) &&
                SPARCAdapterFrameGenerator.jitToOptimizedCallNeedsAdapterFrame(targetMethod.classMethodActor());
            if (hasAdapterFrame) {
                final Pointer startOfAdapter = SPARCAdapterFrameGenerator.jitEntryPointBranchTarget(stackFrameWalker, targetMethod);
                if (inAdapterFrameCode(isTopFrame, instructionPointer, optimizedEntryPoint, startOfAdapter)) {
                    return walkAdapterFrame(stackFrameWalker, targetMethod, purpose, context, startOfAdapter, isTopFrame);
                }
            }
            entryPoint = optimizedEntryPoint;
        }

        // Currently, all optimized stack frames are allocated a new register window in the first instruction of their prologue.
        // The caller's window is restored in the delay slot of the return instruction.
        // When on the first instruction of the prologue of a method, the callee is still operating with the caller's register window,
        // i.e., both the stack and frame pointer are that of the callers. Further, the instruction pointer
        // is in %o7 (the "frameless call instruction register"), not %i7, and therefore cannot be found on the stack.
        // This situation occurs only in the first instruction of the optimized method. Because the restoring of the
        // caller's window takes place in the last instruction of the method, this situation doesn't occur in the method's epilogue.

        final Pointer framePointer;
        final Pointer stackPointer;
        // We use lessEqual here because the instruction pointer may be on one of the two nops preceding the optimized entry point if the
        // method doesn't need an adapter frame.
        final boolean inCallerRegisterWindow = inCallerRegisterWindow(instructionPointer, entryPoint, targetMethod.frameSize());
        if (inCallerRegisterWindow) {
            // The save instruction hasn't been executed. The frame pointer is the same as the caller's stack pointer.
            // We need to compute the stack pointer for this frame
            framePointer =  stackFrameWalker.stackPointer();
            stackPointer = framePointer.minus(targetMethod.frameSize());
        } else {
            framePointer = stackFrameWalker.framePointer();
            stackPointer =  stackFrameWalker.stackPointer();
        }

        switch (purpose) {
            case REFERENCE_MAP_PREPARING: {
                FatalError.unimplemented();
                // FIXME: this need to be revisited
                if (!targetMethod.prepareFrameReferenceMap((StackReferenceMapPreparer) context, instructionPointer, stackPointer, stackPointer)) {
                    return false;
                }
                break;
            }
            case EXCEPTION_HANDLING: {
                final Address catchAddress = targetMethod.throwAddressToCatchAddress(instructionPointer);
                if (!catchAddress.isZero()) {
                    final StackUnwindingContext stackUnwindingContext = UnsafeLoophole.cast(context);
                    final Throwable throwable = stackUnwindingContext._throwable;
                    if (!(throwable instanceof StackOverflowError) || VmThread.current().hasSufficentStackToReprotectGuardPage(stackPointer)) {
                        // Reset the stack walker
                        stackFrameWalker.reset();

                        // Completes the exception handling protocol (with respect to the garbage collector) initiated in Throw.raise()
                        Safepoint.enable();

                        unwind(throwable, catchAddress, stackPointer);
                    }
                }
                final SPARCStackUnwindingContext unwindingContext = UnsafeLoophole.cast(context);
                unwindingContext.record(stackFrameWalker.stackPointer(), stackFrameWalker.framePointer());
                break;
            }
            case INSPECTING: {
                final StackFrameVisitor stackFrameVisitor = (StackFrameVisitor) context;
                if (!stackFrameVisitor.visitFrame(new SPARCJavaStackFrame(stackFrameWalker.calleeStackFrame(), targetMethod, instructionPointer, framePointer, stackPointer))) {
                    return false;
                }
                break;
            }
        }
        Pointer callerInstructionPointer;
        // Only the top most frame can still be in its caller's register window, in this case, we need to find the return address from the O7 register.
        // However, the stack frame at a trampoline call may look like a top frame because
        // the trampoline's return address is patched into the entry point of the method invoked via the trampoline.
        // In that case, we still need to get the caller address from the stack. Testing if we're in the top frame filters out these trampoline call.
        if (inCallerRegisterWindow && isTopFrame) {
            if (purpose.equals(Purpose.INSPECTING)) {
                // In that case, the return pointer can only be found in the FRAMELESS_CALL_INSTRUCTION_ADDRESS register.
                callerInstructionPointer = stackFrameWalker.readFramelessCallAddressRegister(targetMethod.abi()).asPointer();
            } else {
                // When purpose is other than inspecting, this situation can only occur when we trapped in a prologue (e.g.,
                // when banging the stack).
                // We can fish for the caller's instruction pointer in the trapped state, which is located at the
                // top of the callee stack.
                final SPARCSafepoint safepoint = (SPARCSafepoint) VMConfiguration.hostOrTarget().safepoint();
                final Pointer trapState = STACK_BIAS.SPARC_V9.unbias(stackFrameWalker.stackPointer()).minus(SPARCSafepoint.TRAP_STATE_SIZE);
                callerInstructionPointer = safepoint.getCallAddressRegister(trapState);
            }
        } else {
            callerInstructionPointer = SPARCStackFrameLayout.getCallerPC(stackFrameWalker);
        }

        final Pointer callerStackPointer = framePointer;
        Pointer callerFramePointer = inCallerRegisterWindow ? stackFrameWalker.framePointer() : SPARCStackFrameLayout.getCallerFramePointer(stackFrameWalker);
        final TargetMethod caller = stackFrameWalker.targetMethodFor(callerInstructionPointer);
        if (caller instanceof JitTargetMethod) {
            if (inCallerRegisterWindow) {
                if (purpose.equals(Purpose.INSPECTING)) {
                    // FIXME: this is a gross hack to read the caller's frame pointer (it isn't in the standard frame pointer register.
                    // The stackFrameWalker doesn't provide an API to obtain the value of arbitrary register and we cannot use the
                    // VMRegister class here for it wouldn't work when inspecting. So the hack here consists of forcing the walker
                    // to use the caller's ABI, which will set the frame pointer to the appropriate
                    // register. This works only because we're in the caller's register window.
                    stackFrameWalker.useABI(caller.abi());
                    callerFramePointer = stackFrameWalker.framePointer();
                } else {
                    final Pointer savedRegisterWindow = STACK_BIAS.SPARC_V9.unbias(stackFrameWalker.stackPointer());
                    callerFramePointer = SPARCStackFrameLayout.getRegisterInSavedWindow(stackFrameWalker, savedRegisterWindow, GPR.L6).asPointer();
                }
            } else {
                final Pointer savedRegisterWindow = STACK_BIAS.SPARC_V9.unbias(callerStackPointer);
                callerFramePointer = SPARCStackFrameLayout.getRegisterInSavedWindow(stackFrameWalker, savedRegisterWindow, GPR.L6).asPointer();
            }
        }
        stackFrameWalker.advance(callerInstructionPointer, callerStackPointer, callerFramePointer);
        return true;
    }

    @Override
    public Pointer namedVariablesBasePointer(Pointer stackPointer, Pointer framePointer) {
        return framePointer;
    }

    @Override
    public void advance(StackFrameWalker stackFrameWalker, Word instructionPointer, Word stackPointer, Word framePointer) {
        stackFrameWalker.advance(instructionPointer, stackPointer, framePointer);
    }

    @Override
    public StackUnwindingContext makeStackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        return new SPARCStackUnwindingContext(stackPointer, framePointer, throwable);
    }
}

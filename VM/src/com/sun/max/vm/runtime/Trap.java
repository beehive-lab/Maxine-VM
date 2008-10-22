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
package com.sun.max.vm.runtime;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * This class handles operating systems traps that can arise from implicit null pointer checks,
 * integer divide by zero, GC safepoint triggering, etc. It contains a number of very low-level
 * functions to directly handle operating system signals (e.g. SIGSEGV on POSIX) and dispatch to
 * the correct handler (e.g. construct and throw a null pointer exception object or stop the
 * current thread for GC).
 *
 * A small amount of native code supports this class by connecting to the OS-specific signal
 * handling mechanisms.
 *
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Karthik Manivannan
 */
public final class Trap {
    private static VMOption _dumpStackOnSegfault =
        new VMOption("-XX:DumpStackOnSegFault", "Reports a stack trace for every segmentation fault, regardless of the cause.", MaxineVM.Phase.PRISTINE);

    private static final CriticalMethod _illegalInstructionHandler = lookup("handleIllegalInstruction");
    private static final CriticalMethod _segmentationFaultHandler = lookup("handleSegmentationFault");
    private static final CriticalMethod _stackFaultHandler = lookup("handleStackFault");
    private static final CriticalMethod _divideByZeroHandler = lookup("handleDivideByZero");
    private static final CriticalMethod _interruptHandler = lookup("handleInterrupt");
    private static final CriticalMethod _busErrorHandler = lookup("handleBusError");

    public static final CriticalMethod _nullPointerExceptionStub = lookup("nullPointerExceptionStub");
    public static final CriticalMethod _divideByZeroExceptionStub = lookup("divideByZeroExceptionStub");
    public static final CriticalMethod _vmHardExitStub = lookup("vmHardExitStub");
    public static final CriticalMethod _vmHardExitWhenNativeCodeHitsGuardPageStub = lookup("vmHardExitWhenNativeCodeHitsGuardPageStub");
    public static final CriticalMethod _stackOverflowErrorStub = lookup("stackOverflowErrorStub");
    public static final CriticalMethod _deoptimizationStub = lookup("deoptimizationStub");

    @PROTOTYPE_ONLY
    private static CriticalMethod lookup(String methodName) {
        return new CriticalMethod(Trap.class, methodName, CallEntryPoint.C_ENTRY_POINT);
    }

    static {
        new CriticalNativeMethod(MaxineVM.class, "native_exit");
        new CriticalNativeMethod(MaxineVM.class, "native_trap_exit");
        new CriticalNativeMethod(MaxineVM.class, "native_stack_trap_exit");
    }

    @PROTOTYPE_ONLY
    private Trap() {
    }

    /**
     * The signature that all {@linkplain C_FUNCTION#isSignalHandler() Java trap handlers} must match.
     */
    @PROTOTYPE_ONLY
    private static final SignatureDescriptor javaTrapHandlerSignature = SignatureDescriptor.create(Word.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class, Pointer.class);

    static {
        // Verifies that all Java trap handlers called from the C trap handler ('trapHandler' in trap.c) have the expected signature
        for (Method method : Trap.class.getDeclaredMethods()) {
            final C_FUNCTION annotation = method.getAnnotation(C_FUNCTION.class);
            if (annotation != null) {
                if (annotation.isSignalHandler()) {
                    final SignatureDescriptor signature = SignatureDescriptor.fromJava(method);
                    if (!javaTrapHandlerSignature.equals(signature)) {
                        ProgramError.unexpected(String.format("Incorrect signature for Java trap handler: %s%n  expected: %s%n       got: %s", method, javaTrapHandlerSignature, signature));
                    }
                }
            }
        }
    }

    /**
     * Records the information about the frame in which the trap occurred. This information is recorded in these thread local variables:
     * {@link VmThreadLocal#TRAP_INSTRUCTION_POINTER}, {@link VmThreadLocal#TRAP_FRAME_POINTER} and {@link VmThreadLocal#TRAP_STACK_POINTER}.
     *
     * @param triggeredVmThreadLocals the safepoint latch pointer as determined by native code for the TRIGGERED state
     * @param stackPointer the value of the stack pointer when the trap occurred
     * @param framePointer the value of the frame pointer when the trap occurred
     * @param instructionPointer the value of the instruction pointer when the trap occurred
     * @param integerRegisters a pointer to a memory buffer containing the contents of the integer registers at the time
     *            of the fault
     * @return the safepoints-disabled VM thread locals
     */
    private static Pointer prepareVmThreadLocals(Pointer triggeredVmThreadLocals, Pointer stackPointer, Pointer framePointer, Pointer instructionPointer, Pointer integerRegisters) {
        if (!SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(triggeredVmThreadLocals).equals(triggeredVmThreadLocals)) {
            // TODO: fail more gracefully
            MaxineVM.native_exit(-22);
        }
        final Pointer disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(triggeredVmThreadLocals).asPointer();
        Safepoint.setLatchRegister(disabledVmThreadLocals);
        integerRegisters.setWord(SAFEPOINT_LATCH_REGISTER_INDEX, disabledVmThreadLocals);
        // record the excepting point and return the vm thread locals pointer
        TRAP_INSTRUCTION_POINTER.setVariableWord(disabledVmThreadLocals, instructionPointer);
        TRAP_STACK_POINTER.setVariableWord(disabledVmThreadLocals, stackPointer);
        TRAP_FRAME_POINTER.setVariableWord(disabledVmThreadLocals, framePointer);
        if (!TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME.getVariableWord(disabledVmThreadLocals).isZero()) {
            Debug.println("TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME should be zero");
        }
        TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME.setVariableWord(disabledVmThreadLocals, Address.fromLong(1));

        return disabledVmThreadLocals;
    }

    /**
     * Saves the CPU's register contents in a thread local space.
     */
    private static void saveRegisters(Pointer vmThreadLocals, Pointer integerRegisters, Pointer floatingPointRegisters) {
        Pointer p = REGISTERS.pointer(vmThreadLocals);
        for (int i = 0; i < NUMBER_OF_INTEGER_REGISTERS; i++) {
            p.setWord(integerRegisters.getWord(i));
            p = p.plusWords(1);
        }
        for (int i = 0; i < NUMBER_OF_FLOATING_POINT_REGISTERS; i++) {
            p.setDouble(i, floatingPointRegisters.getDouble(i));
        }
    }

    @NEVER_INLINE
    private static Word leaveHandler(Word stubInstructionPointer, Pointer vmThreadLocals) {
        TRAP_HANDLER_HAS_RECORDED_TRAP_FRAME.setVariableWord(vmThreadLocals, Address.zero());
        return stubInstructionPointer;
    }

    private static final int SAFEPOINT_LATCH_REGISTER_INDEX = VMConfiguration.hostOrTarget().safepoint().latchRegister().value();

    /**
     * This method is called by the native trap handler when a segmentation fault occurs. The native code gathers the
     * stack pointer, frame pointer, instruction pointer, and the set of integer and floating registers.
     *
     * @param triggeredVmThreadLocals the safepoint latch pointer as determined by native code for the TRIGGERED state
     * @param stackPointer the stack pointer at the time of the fault
     * @param framePointer the frame pointer at the time of the fault
     * @param instructionPointer the instruction pointer at the time of the fault
     * @param integerRegisters a pointer to a memory buffer containing the contents of the integer registers at the time
     *            of the fault
     * @param floatingPointRegisters a pointer to a memory buffer containing the contents of the floating point
     *            registers at the time of the fault
     * @param faultAddress
     * @return the entry point of the stub to execute on return from the signal handler
     */
    @C_FUNCTION(isSignalHandler = true)
    private static Word handleSegmentationFault(Pointer triggeredVmThreadLocals,
                                                Pointer stackPointer,
                                                Pointer framePointer,
                                                Pointer instructionPointer,
                                                Pointer integerRegisters,
                                                Pointer floatingPointRegisters,
                                                Pointer faultAddress) {
        final Word safepointLatch = integerRegisters.getWord(SAFEPOINT_LATCH_REGISTER_INDEX);
        final Pointer disabledVmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod == null) {
            if (Code.codePointerToRuntimeStub(instructionPointer) != null) {
                Debug.print("Seg fault in runtime stub at ");
                Debug.println(instructionPointer);
            } else {
                Debug.print("Seg fault in native code at ");
                Debug.println(instructionPointer);
            }
            return _vmHardExitStub.address();
        } else if (targetMethod instanceof JitTargetMethod) {
            // We may have recorded the incorrect frame pointer if the JIT ABI doesn't use the CPU frame pointer.
            final Pointer abiFramePointer = ((JitTargetMethod) targetMethod).getFramePointer(stackPointer, framePointer, integerRegisters);
            TRAP_FRAME_POINTER.setVariableWord(disabledVmThreadLocals, abiFramePointer);
        }

        final boolean isSafepointTriggered = safepointLatch.equals(triggeredVmThreadLocals);

        if (_dumpStackOnSegfault.isPresent()) {
            Throw.stackDump("Segmentation fault @ " + instructionPointer.toHexString() + " faulting address: " + faultAddress.toHexString(), instructionPointer, stackPointer, framePointer);
        }

        Address stub;
        if (isSafepointTriggered && VMConfiguration.hostOrTarget().safepoint().isAt(instructionPointer)) {
            // only handle a safepoint if BOTH the latch register has been signaled AND we are at a safepoint instruction.
            final Word enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(disabledVmThreadLocals);
            saveRegisters(enabledVmThreadLocals.asPointer(), integerRegisters, floatingPointRegisters);
            stub = Safepoint.trapHandler(disabledVmThreadLocals);
        } else if (inJava(disabledVmThreadLocals)) {
            stub = _nullPointerExceptionStub.address();
        } else {
            // segmentation fault happened in native code somewhere
            stub = _vmHardExitStub.address();
        }
        return leaveHandler(stub, disabledVmThreadLocals);
    }

    @C_FUNCTION(isSignalHandler = true)
    private static Word handleStackFault(Pointer triggeredVmThreadLocals,
                    Pointer stackPointer,
                    Pointer framePointer,
                    Pointer instructionPointer,
                    Pointer integerRegisters,
                    Pointer floatingPointRegisters,
                    Pointer faultAddress) {
        final Pointer disabledVmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);

        Address stub;
        final Address redZone = Pointer.zero(); // TODO: find the correct red zone
        if (!inJava(disabledVmThreadLocals)) {
            // stack fault occurred somewhere in native code.
            stub = _vmHardExitStub.address();
        } else if (faultAddress.greaterEqual(redZone) && faultAddress.lessThan(redZone.plus(VmThread.guardPageSize()))) {
            // the red zone was reached.
            stub = _vmHardExitStub.address();
        } else {
            // Java code caused a stack overflow.
            stub = _stackOverflowErrorStub.address();
        }
        return leaveHandler(stub, disabledVmThreadLocals);
    }

    @C_FUNCTION(isSignalHandler = true)
    private static Word handleIllegalInstruction(Pointer triggeredVmThreadLocals,
                                                 Pointer stackPointer,
                                                 Pointer framePointer,
                                                 Pointer instructionPointer,
                                                 Pointer integerRegisters,
                                                 Pointer floatingPointRegisters,
                                                 Pointer faultAddress) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        final Pointer vmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);
        saveRegisters(SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer(), integerRegisters, floatingPointRegisters);
        if (targetMethod == null) {
            // Illegal instruction in a native method, i.e. this is a bug:
            return _vmHardExitStub.address();
        } else if (targetMethod instanceof JitTargetMethod) {
            // We may have recorded the incorrect frame pointer if the JIT abi doesn't use the cpu frame pointer.
            final Pointer abiFramePointer = ((JitTargetMethod) targetMethod).getFramePointer(stackPointer, framePointer, integerRegisters);
            TRAP_FRAME_POINTER.setVariableWord(vmThreadLocals, abiFramePointer);
        }
        final Deoptimizer.ReferenceOccurrences occurrences = Deoptimizer.determineReferenceOccurrences(targetMethod, instructionPointer);
        switch (occurrences) {
            case ERROR: {
                return leaveHandler(_vmHardExitStub.address(), vmThreadLocals);
            }
            case NONE: {
                break;
            }
            case RETURN: {
                break;
            }
            case SAFEPOINT: {
                DEOPTIMIZER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, instructionPointer);
                break;
            }
        }
        DEOPTIMIZER_REFERENCE_OCCURRENCES.setVariableReference(vmThreadLocals, Reference.fromJava(occurrences));
        return leaveHandler(_deoptimizationStub.address(), vmThreadLocals);
    }

    @C_FUNCTION(isSignalHandler = true)
    private static Word handleInterrupt(Pointer triggeredVmThreadLocals,
                                        Pointer stackPointer,
                                        Pointer framePointer,
                                        Pointer instructionPointer,
                                        Pointer integerRegisters,
                                        Pointer floatingPointRegisters,
                                        Pointer faultAddress) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        final Pointer vmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);
        if (targetMethod == null) {
            return _vmHardExitStub.address();
        } else if (targetMethod instanceof JitTargetMethod) {
            // We may have recorded the incorrect frame pointer if the JIT abi doesn't use the cpu frame pointer.
            final Pointer abiFramePointer = ((JitTargetMethod) targetMethod).getFramePointer(stackPointer, framePointer, integerRegisters);
            TRAP_FRAME_POINTER.setVariableWord(vmThreadLocals, abiFramePointer);
        }
        return leaveHandler(_vmHardExitStub.address(), vmThreadLocals);
    }

    @C_FUNCTION(isSignalHandler = true)
    private static Word handleDivideByZero(Pointer triggeredVmThreadLocals,
                                           Pointer stackPointer,
                                           Pointer framePointer,
                                           Pointer instructionPointer,
                                           Pointer integerRegisters,
                                           Pointer floatingPointRegisters,
                                           Pointer faultAddress) {
        final Pointer vmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod == null) {
            return _vmHardExitStub.address();
        } else if (targetMethod instanceof JitTargetMethod) {
            // We may have recorded the incorrect frame pointer if the JIT abi doesn't use the cpu frame pointer.
            final Pointer abiFramePointer = ((JitTargetMethod) targetMethod).getFramePointer(stackPointer, framePointer, integerRegisters);
            TRAP_FRAME_POINTER.setVariableWord(vmThreadLocals, abiFramePointer);
        }
        if (inJava(vmThreadLocals)) {
            return leaveHandler(_divideByZeroExceptionStub.address(), vmThreadLocals);
        }
        return leaveHandler(_vmHardExitStub.address(), vmThreadLocals);
    }

    @C_FUNCTION(isSignalHandler = true)
    private static Word handleBusError(Pointer triggeredVmThreadLocals,
                                       Pointer stackPointer,
                                       Pointer framePointer,
                                       Pointer instructionPointer,
                                       Pointer integerRegisters,
                                       Pointer floatingPointRegisters,
                                       Pointer faultAddress) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        final Pointer vmThreadLocals = prepareVmThreadLocals(triggeredVmThreadLocals, stackPointer, framePointer, instructionPointer, integerRegisters);

        if (targetMethod == null) {
            return _vmHardExitStub.address();
        } else if (targetMethod instanceof JitTargetMethod) {
            // We may have recorded the incorrect frame pointer if the JIT abi doesn't use the cpu frame pointer.
            final Pointer abiFramePointer = ((JitTargetMethod) targetMethod).getFramePointer(stackPointer, framePointer, integerRegisters);
            TRAP_FRAME_POINTER.setVariableWord(vmThreadLocals, abiFramePointer);
        }
        final Word stub = inJava(vmThreadLocals) ? _divideByZeroExceptionStub.address() : _vmHardExitStub.address();
        return leaveHandler(stub, vmThreadLocals);
    }

    @C_FUNCTION
    private static native void native_setIllegalInstructionHandler(Word trapHandler);

    @C_FUNCTION
    private static native void native_setSegmentationFaultHandler(Word trapHandler);

    @C_FUNCTION
    private static native void native_setStackFaultHandler(Word trapHandler);

    @C_FUNCTION
    private static native void native_setBusErrorHandler(Word trapHandler);

    @C_FUNCTION
    private static native void native_setDivideByZeroHandler(Word trapHandler);

    @C_FUNCTION
    private static native void native_setInterruptHandler(Word trapHandler);

    /**
     * Installs the trap handlers using the operating system's API.
     */
    public static void initialize() {
        native_setIllegalInstructionHandler(_illegalInstructionHandler.address());
        native_setSegmentationFaultHandler(_segmentationFaultHandler.address());
        native_setStackFaultHandler(_stackFaultHandler.address());
        native_setDivideByZeroHandler(_divideByZeroHandler.address());
        native_setBusErrorHandler(_busErrorHandler.address());
        native_setInterruptHandler(_interruptHandler.address());
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void nullPointerExceptionStub() {
        final Pointer instructionPointer = TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer();
        final Pointer stackPointer = TRAP_STACK_POINTER.getVariableWord().asPointer();
        final Pointer framePointer = TRAP_FRAME_POINTER.getVariableWord().asPointer();

        // allocating a new exception here (with TRAP_INSTRUCTION_POINTER set) will cause the stack trace to
        // be filled in starting from the implicit exception point
        final NullPointerException exception = new NullPointerException();
        Throw.raise(exception, stackPointer, framePointer, instructionPointer);
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void vmHardExitStub() {
        // TODO: attempt to report a stack trace (without recursion)
        // In the meantime say something (but not risking another trap)
        MaxineVM.native_trap_exit(MaxineVM.HARD_EXIT_CODE, TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer());
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void vmHardExitWhenNativeCodeHitsGuardPageStub() {
        // TODO: attempt to report a stack trace (without recursion)
        // In the meantime say something (but not risking another trap)
        MaxineVM.native_stack_trap_exit(MaxineVM.HARD_EXIT_CODE, TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer());
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void divideByZeroExceptionStub() {
        final Pointer instructionPointer = TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer();
        final Pointer stackPointer = TRAP_STACK_POINTER.getVariableWord().asPointer();
        final Pointer framePointer = TRAP_FRAME_POINTER.getVariableWord().asPointer();

        // allocating a new exception here (with TRAP_INSTRUCTION_POINTER set) will cause the stack trace to
        // be filled in starting from the implicit exception point
        final ArithmeticException exception = new ArithmeticException();
        Throw.raise(exception, stackPointer, framePointer, instructionPointer);
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void stackOverflowErrorStub() {
        final Pointer instructionPointer = TRAP_INSTRUCTION_POINTER.getVariableWord().asPointer();
        final Pointer stackPointer = TRAP_STACK_POINTER.getVariableWord().asPointer();
        final Pointer framePointer = TRAP_FRAME_POINTER.getVariableWord().asPointer();

        final StackOverflowError error = new StackOverflowError();
        Throw.raise(error, stackPointer, framePointer, instructionPointer);
    }

    @C_FUNCTION(isSignalHandlerStub = true)
    private static void deoptimizationStub() {
        VMConfiguration.hostOrTarget().compilerScheme().fakeCall(TRAP_INSTRUCTION_POINTER.getVariableWord().asAddress());
        Deoptimizer.deoptimizeTopFrame();
    }
}

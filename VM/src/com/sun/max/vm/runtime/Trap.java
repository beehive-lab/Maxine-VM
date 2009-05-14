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

import static com.sun.max.vm.runtime.Trap.Number.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * This class handles operating systems traps that can arise from implicit null pointer checks, integer divide by zero,
 * GC safepoint triggering, etc. It contains a number of very low-level functions to directly handle operating system
 * signals (e.g. SIGSEGV on POSIX) and dispatch to the correct handler (e.g. construct and throw a null pointer
 * exception object or stop the current thread for GC).
 *
 * A small amount of native code supports this class by connecting to the OS-specific signal handling mechanisms.
 *
 * @author Ben L. Titzer
 */
public final class Trap {

    /**
     * The numeric identifiers for the traps that can be handled by the VM. Note that these do not correspond with the
     * native signals.
     *
     * The values defined here (except for {@link #NULL_POINTER_EXCEPTION} and {@link #SAFEPOINT}) must correspond to
     * those of the same name defined in Native/substrate/trap.c.
     *
     * The {@link #NULL_POINTER_EXCEPTION} and {@link #SAFEPOINT} values are used in
     * {@link Trap#handleMemoryFault(Pointer, TargetMethod, Pointer, Pointer, Pointer, Address)} to disambiguate a memory fault.
     */
    public static final class Number {
        public static final int MEMORY_FAULT = 0;
        public static final int STACK_FAULT = 1;
        public static final int ILLEGAL_INSTRUCTION = 2;
        public static final int ARITHMETIC_EXCEPTION = 3;
        public static final int ASYNC_INTERRUPT = 4;
        public static final int NULL_POINTER_EXCEPTION = 5;
        public static final int SAFEPOINT = 6;

        public static boolean isImplicitException(int trapNumber) {
            return trapNumber == ARITHMETIC_EXCEPTION || trapNumber == NULL_POINTER_EXCEPTION || trapNumber == STACK_FAULT;
        }
    }

    private static VMOption _dumpStackOnTrap =
        new VMOption("-XX:DumpStackOnTrap", "Reports a stack trace for every trap, regardless of the cause.", MaxineVM.Phase.PRISTINE);

    /**
     * This method is {@linkplain #isTrapStub(MethodActor) known} by the compilation system. In particular, no adapter
     * frame code generated for it. As such, it's entry point is at it's first compiled instruction which corresponds
     * with it's entry point it it were to be called from C code.
     */
    private static final CriticalMethod _trapStub = new CriticalMethod(Trap.class, "trapStub", null, CallEntryPoint.C_ENTRY_POINT);

    private static final CriticalMethod _nativeExit = new CriticalNativeMethod(MaxineVM.class, "native_exit");

    @PROTOTYPE_ONLY
    private static final Method _trapStubMethod = Classes.getDeclaredMethod(Trap.class, "trapStub", int.class, Pointer.class, Address.class);

    /**
     * Determines if a given method actor denotes the method used to handle runtime traps.
     *
     * @param methodActor the method actor to test
     * @return true if {@code classMethodActor} is the actor for {@link #trapStub(int, Pointer, Address)}
     */
    public static boolean isTrapStub(MethodActor methodActor) {
        if (MaxineVM.isPrototyping()) {
            return !methodActor.isInitializer() && MaxineVM.isMaxineClass(methodActor.holder()) && methodActor.toJava().equals(_trapStubMethod);
        }
        return methodActor == _trapStub.classMethodActor();
    }

    @PROTOTYPE_ONLY
    private Trap() {
    }

    @C_FUNCTION
    private static native void nativeInitialize(Word trapHandler);

    /**
     * Installs the trap handlers using the operating system's API.
     */
    public static void initialize() {
        nativeInitialize(_trapStub.address());
    }

    /**
     * This method handles traps that occurred during execution. This method has a special ABI produced by the compiler
     * that saves the entire register state onto the stack before beginning execution. When a trap occurs, the native
     * trap handler (see trap.c) saves a small amount of state in the disabled thread locals
     * for the thread (the trap number, the instruction pointer, and the fault address) and then returns to this stub.
     * This trap stub saves all of the registers onto the stack which are available in the {@code trapState}
     * pointer.
     *
     * @param trapNumber the trap number that occurred
     * @param trapState a pointer to the stack location where trap state is stored
     * @param faultAddress the faulting address that caused this trap (memory faults only)
     */
    @C_FUNCTION
    private static void trapStub(int trapNumber, Pointer trapState, Address faultAddress) {
        if (trapNumber == ASYNC_INTERRUPT) {
            // do nothing for an asynchronous interrupt.
            return;
        }
        final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
        final Pointer instructionPointer = safepoint.getInstructionPointer(trapState);
        final Object origin = checkTrapOrigin(trapNumber, trapState, faultAddress);
        if (origin instanceof TargetMethod) {
            final TargetMethod targetMethod = (TargetMethod) origin;
            // the trap occurred in Java
            final Pointer stackPointer = safepoint.getStackPointer(trapState, targetMethod);
            final Pointer framePointer = safepoint.getFramePointer(trapState, targetMethod);

            switch (trapNumber) {
                case MEMORY_FAULT:
                    handleMemoryFault(instructionPointer, targetMethod, stackPointer, framePointer, trapState, faultAddress);
                    break;
                case STACK_FAULT:
                    // stack overflow
                    final StackOverflowError error = new StackOverflowError();
                    raise(trapState, targetMethod, error, stackPointer, framePointer, instructionPointer);
                    break; // unreachable
                case ILLEGAL_INSTRUCTION:
                    // deoptimization
                    Deoptimizer.deoptimizeTopFrame();
                    break;
                case ARITHMETIC_EXCEPTION:
                    // integer divide by zero
                    final ArithmeticException exception = new ArithmeticException();
                    raise(trapState, targetMethod, exception, stackPointer, framePointer, instructionPointer);
                    break; // unreachable
            }
        } else {
            // the fault occurred in native code
            Log.print("Trap in native code (or runtime stub) @ ");
            Log.print(instructionPointer);
            Log.println(", exiting.");
            FatalError.unexpected("Trap in native code", instructionPointer);
        }
    }

    /**
     * Checks the origin of a trap by looking for a target method or runtime stub in the code regions. If found, this
     * method will return a reference to the {@code TargetMethod} that produced the trap. If a runtime stub produced
     * the trap, this method will return a reference to that runtime stub. Otherwise, this method returns {@code null},
     * indicating the trap occurred in native code.
     *
     * @param trapNumber the trap number
     * @param trapState the trap state area on the stack
     * @param trapInstructionPointer the instruction pointer where the trap occurred
     * @return a reference to the {@code TargetMethod} containing the instruction pointer that caused the trap; {@code
     *         null} if neither a runtime stub nor a target method produced the trap
     */
    private static Object checkTrapOrigin(int trapNumber, Pointer trapState, Address trapInstructionPointer) {
        final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
        final Pointer instructionPointer = safepoint.getInstructionPointer(trapState);

        if (_dumpStackOnTrap.isPresent()) {
            Log.print("Trap ");
            Log.print(trapNumber);
            Log.print(" @ ");
            Log.print(instructionPointer);
            Log.print(", trap instruction pointer: ");
            Log.print(trapInstructionPointer);
            Throw.stackDump("", instructionPointer, safepoint.getStackPointer(trapState, null), safepoint.getFramePointer(trapState, null));
        }

        // check to see if this fault originated in a target method
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod != null) {
            return targetMethod;
        }

        // check to see if this fault originated in a runtime stub
        final RuntimeStub runtimeStub = Code.codePointerToRuntimeStub(instructionPointer);
        if (runtimeStub != null) {
            Log.print("Trap ");
            Log.print(trapNumber);
            Log.print(" in runtime stub @ ");
            return runtimeStub;
        }

        // this fault occurred in native code
        Log.print("Trap ");
        Log.print(trapNumber);
        Log.print(" in native code @ ");
        Log.println(instructionPointer);

        return null;
    }

    /**
     * Handle a memory fault for this thread. A memory fault can be caused by an implicit null pointer check,
     * a safepoint being triggered, or a segmentation fault in native code.
     *
     * @param instructionPointer the instruction pointer that caused the fault
     * @param targetMethod the TargetMethod containing {@code instructionPointer}
     * @param stackPointer the stack pointer at the time of the fault
     * @param framePointer the frame pointer at the time of the fault
     * @param trapState a pointer to the trap state at the time of the fault
     * @param faultAddress the address that caused the fault
     */
    private static void handleMemoryFault(Pointer instructionPointer, TargetMethod targetMethod, Pointer stackPointer, Pointer framePointer, Pointer trapState, Address faultAddress) {
        final Pointer disabledVmThreadLocals = VmThread.currentVmThreadLocals();

        final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
        final Pointer triggeredVmThreadLocals = VmThreadLocal.SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(disabledVmThreadLocals).asPointer();
        final Pointer safepointLatch = safepoint.getSafepointLatch(trapState);

        if (VmThread.current().isGCThread()) {
            FatalError.unexpected("Memory fault on a GC thread");
        }

        // check to see if a safepoint has been triggered for this thread
        if (safepointLatch.equals(triggeredVmThreadLocals) && safepoint.isAt(instructionPointer)) {
            // a safepoint has been triggered for this thread. run the specified procedure
            final Reference reference = VmThreadLocal.SAFEPOINT_PROCEDURE.getVariableReference(triggeredVmThreadLocals);
            final Safepoint.Procedure runnable = UnsafeLoophole.cast(reference.toJava());
            safepoint.setTrapNumber(trapState, Number.SAFEPOINT);
            if (runnable != null) {
                // run the procedure and then set the vm thread local to null
                runnable.run(trapState);
                // the state of the safepoint latch was TRIGGERED when the trap happened. reset it back to ENABLED.
                final Pointer enabledVmThreadLocals = VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(disabledVmThreadLocals).asPointer();
                safepoint.setSafepointLatch(trapState, enabledVmThreadLocals);
                Safepoint.reset(enabledVmThreadLocals);
                // reset the procedure to be null
                VmThreadLocal.SAFEPOINT_PROCEDURE.setVariableReference(triggeredVmThreadLocals, null);
            }
        } else if (inJava(disabledVmThreadLocals)) {
            safepoint.setTrapNumber(trapState, Number.NULL_POINTER_EXCEPTION);
            // null pointer exception
            final NullPointerException error = new NullPointerException();
            raise(trapState, targetMethod, error, stackPointer, framePointer, instructionPointer);
        } else {
            // segmentation fault happened in native code somewhere, die.
            MaxineVM.native_trap_exit(MaxineVM.HARD_EXIT_CODE, instructionPointer);
        }
    }

    /**
     * Raises an implicit exception.
     *
     * If there is a local handler for the exception (i.e. a handler in the same frame in which the exception occurred)
     * and the method in which the exception occurred was compiled by the opto compiler, then the trap state is altered
     * so that the exception object is placed into the register typically used for an integer return value (e.g. RAX on
     * AMD64) and the return address for the trap frame is set to be the exception handler entry address. This means
     * that the register allocator in the opto compiler can assume that registers are not modified in the control flow
     * from an implicit exception to the exception handler (apart from the register now holding the exception object).
     *
     * Otherwise, the {@linkplain Throw#raise(Throwable, Pointer, Pointer, Pointer) standard mechanism} for throwing an
     * exception is used.
     *
     * @param trapState
     * @param targetMethod
     * @param throwable
     * @param stackPointer
     * @param framePointer
     * @param instructionPointer
     */
    private static void raise(Pointer trapState, TargetMethod targetMethod, Throwable throwable, Pointer stackPointer, Pointer framePointer, Pointer instructionPointer) {
        if (targetMethod instanceof JitTargetMethod) {
            Throw.raise(throwable, stackPointer, framePointer, instructionPointer);
        }
        final Address throwAddress = instructionPointer;
        final Address catchAddress = targetMethod.throwAddressToCatchAddress(throwAddress);
        if (!catchAddress.isZero()) {
            if (!(throwable instanceof StackOverflowError) || VmThread.current().hasSufficentStackToReprotectGuardPage(stackPointer)) {
                final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
                safepoint.setInstructionPointer(trapState, catchAddress.asPointer());
                safepoint.setReturnValue(trapState, Reference.fromJava(throwable).toOrigin());
            }
        } else {
            Throw.raise(throwable, stackPointer, framePointer, instructionPointer);
        }
    }
}

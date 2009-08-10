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

import static com.sun.max.vm.VMOptions.*;
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
public abstract class Trap {

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
        public static final int STACK_FATAL = 2;
        public static final int ILLEGAL_INSTRUCTION = 3;
        public static final int ARITHMETIC_EXCEPTION = 4;
        public static final int ASYNC_INTERRUPT = 5;
        public static final int NULL_POINTER_EXCEPTION = 6;
        public static final int SAFEPOINT = 7;

        public static boolean isImplicitException(int trapNumber) {
            return trapNumber == ARITHMETIC_EXCEPTION || trapNumber == NULL_POINTER_EXCEPTION || trapNumber == STACK_FAULT  || trapNumber == STACK_FATAL;
        }
    }

    private static VMBooleanXXOption dumpStackOnTrap =
        register(new VMBooleanXXOption("-XX:-DumpStackOnTrap", "Reports a stack trace for every trap, regardless of the cause."), MaxineVM.Phase.PRISTINE);

    /** The number of bytes reserved in the stack as a guard area.
     *  Note that SPARC code is more efficient if this is set below 6K.  Specifically, set to (6K - 1 - typical_frame_size).
     */
    public static final int stackGuardSize = 12 * Ints.K;
    // TODO (tw): Check why the LSRA needs the value 12K above. Can probably be reduced after implementing better stack slot sharing.

    /**
     * This method is {@linkplain #isTrapStub(MethodActor) known} by the compilation system. In particular, no adapter
     * frame code generated for it. As such, it's entry point is at it's first compiled instruction which corresponds
     * with its entry point it it were to be called from C code.
     */
    private static final CriticalMethod trapStub = new CriticalMethod(Trap.class, "trapStub", null, CallEntryPoint.C_ENTRY_POINT);

    private static final CriticalMethod nativeExit = new CriticalNativeMethod(MaxineVM.class, "native_exit");

    @PROTOTYPE_ONLY
    private static final Method trapStubMethod = Classes.getDeclaredMethod(Trap.class, "trapStub", int.class, Pointer.class, Address.class);

    /**
     * Determines if a given method actor denotes the method used to handle runtime traps.
     *
     * @param methodActor the method actor to test
     * @return true if {@code classMethodActor} is the actor for {@link #trapStub(int, Pointer, Address)}
     */
    public static boolean isTrapStub(MethodActor methodActor) {
        if (MaxineVM.isPrototyping()) {
            return !methodActor.isInitializer() && MaxineVM.isMaxineClass(methodActor.holder()) && methodActor.toJava() != null && methodActor.toJava().equals(trapStubMethod);
        }
        return methodActor == trapStub.classMethodActor;
    }

    @PROTOTYPE_ONLY
    protected Trap() {
    }

    /**
     * The address of the 'traceTrap' static variable in 'trap.c'.
     */
    static Pointer nativeTraceTrapVariable = Pointer.zero();

    /**
     * A VM option to enable tracing of traps, both in the C and Java parts of trap handling.
     */
    private static VMBooleanXXOption traceTrap = register(new VMBooleanXXOption("-XX:-TraceTraps", "Trace traps.") {
        @Override
        public boolean parseValue(Pointer optionValue) {
            if (getValue() && !nativeTraceTrapVariable.isZero()) {
                nativeTraceTrapVariable.writeBoolean(0, true);
            }
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    /**
     * Initializes the native side of trap handling by informing the C code of the address of {@link #trapStub(int, Pointer, Address)}.
     *
     * @param the entry point of {@link #trapStub(int, Pointer, Address)}
     * @return the address of the 'traceTrap' static variable in 'trap.c'
     */
    @C_FUNCTION
    private static native Pointer nativeInitialize(Word trapHandler);

    /**
     * Installs the trap handlers using the operating system's API.
     */
    public static void initialize() {
        nativeTraceTrapVariable = nativeInitialize(trapStub.address());
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
        final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
        final Pointer instructionPointer = trapStateAccess.getInstructionPointer(trapState);
        final Object origin = checkTrapOrigin(trapNumber, trapState, faultAddress);
        if (origin instanceof TargetMethod) {
            final TargetMethod targetMethod = (TargetMethod) origin;
            // the trap occurred in Java
            final Pointer stackPointer = trapStateAccess.getStackPointer(trapState, targetMethod);
            final Pointer framePointer = trapStateAccess.getFramePointer(trapState, targetMethod);

            switch (trapNumber) {
                case MEMORY_FAULT:
                    handleMemoryFault(instructionPointer, targetMethod, stackPointer, framePointer, trapState, faultAddress);
                    break;
                case STACK_FAULT:
                    // stack overflow
                    raise(trapState, targetMethod, new StackOverflowError(), stackPointer, framePointer, instructionPointer);
                    break; // unreachable
                case ILLEGAL_INSTRUCTION:
                    // deoptimization
                    // TODO: deoptimization
                    FatalError.unexpected("illegal instruction", false, null, trapState);
                    break;
                case ARITHMETIC_EXCEPTION:
                    // integer divide by zero
                    raise(trapState, targetMethod, new ArithmeticException(), stackPointer, framePointer, instructionPointer);
                    break; // unreachable
                case STACK_FATAL:
                    // fatal stack overflow
                    FatalError.unexpected("fatal stack fault in red zone", false, null, trapState);
                    break; // unreachable
            }
        } else {
            // the fault occurred in native code
            Log.print("Trap in native code (or a runtime stub) @ ");
            Log.print(instructionPointer);
            Log.println(", exiting.");
            FatalError.unexpected("Trap in native code or a runtime stub", true, null, trapState);
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
     * @param faultAddress the faulting address that caused the trap (memory faults only)
     * @return a reference to the {@code TargetMethod} or {@link RuntimeStub} containing the instruction pointer that
     *         caused the trap or {@code null} if trap occurred in native code
     */
    private static Object checkTrapOrigin(int trapNumber, Pointer trapState, Address faultAddress) {
        final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
        final Pointer instructionPointer = trapStateAccess.getInstructionPointer(trapState);

        // check to see if this fault originated in a target method
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);

        if (traceTrap.getValue() || dumpStackOnTrap.getValue()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printVmThread(VmThread.current(), false);
            if (targetMethod != null) {
                Log.print(": Trapped in ");
                Log.printMethodActor(targetMethod.classMethodActor(), true);
            } else {
                Log.println(": Trapped in <unknown>");
            }
            Log.print("  Trap number=");
            Log.println(trapNumber);
            Log.print("  Instruction pointer=");
            Log.println(instructionPointer);
            Log.print("  Fault address=");
            Log.println(faultAddress);
            trapStateAccess.logTrapState(trapState);
            if (dumpStackOnTrap.getValue()) {
                Throw.stackDump("Stack trace:", instructionPointer, trapStateAccess.getStackPointer(trapState, null), trapStateAccess.getFramePointer(trapState, null));
            }
            Log.unlock(lockDisabledSafepoints);
        }

        if (targetMethod != null) {
            return targetMethod;
        }

        // check to see if this fault originated in a runtime stub
        final RuntimeStub runtimeStub = Code.codePointerToRuntimeStub(instructionPointer);
        if (runtimeStub != null) {
            return runtimeStub;
        }

        // this fault occurred in native code
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

        final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint;
        final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
        final Pointer triggeredVmThreadLocals = VmThreadLocal.SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(disabledVmThreadLocals).asPointer();
        final Pointer safepointLatch = trapStateAccess.getSafepointLatch(trapState);

        if (VmThread.current().isGCThread()) {
            FatalError.unexpected("Memory fault on a GC thread", false, null, trapState);
        }

        // check to see if a safepoint has been triggered for this thread
        if (safepointLatch.equals(triggeredVmThreadLocals) && safepoint.isAt(instructionPointer)) {
            // a safepoint has been triggered for this thread. run the specified procedure
            final Reference reference = VmThreadLocal.SAFEPOINT_PROCEDURE.getVariableReference(triggeredVmThreadLocals);
            final Safepoint.Procedure runnable = UnsafeLoophole.cast(reference.toJava());
            trapStateAccess.setTrapNumber(trapState, Number.SAFEPOINT);
            if (runnable != null) {
                // run the procedure and then set the vm thread local to null
                runnable.run(trapState);

                // reset the procedure to be null
                SAFEPOINT_PROCEDURE.setVariableReference(triggeredVmThreadLocals, null);
            } else {
                /*
                 * The interleaving of a mutator thread and a GC thread below demonstrates
                 * one case where this can occur:
                 *
                 *    Mutator thread        |  GC thread
                 *  ------------------------+-----------------------------------------------------------------
                 *                          |  trigger safepoints and set safepoint procedure for all threads
                 *  loop: safepoint         |
                 *        block on mutex    |
                 *                          |  complete GC
                 *                          |  reset safepoints and cancel safepoint procedure for all threads
                 *        wake from mutex   |
                 *  loop: safepoint         |
                 *
                 * The second safepoint instruction on the mutator thread will cause a trap when
                 * the safepoint procedure for the mutator is null.
                 */
            }
            // The state of the safepoint latch was TRIGGERED when the trap happened. It must be reset back to ENABLED
            // here otherwise another trap will occur as soon as the trap stub returns and re-executes the
            // safepoint instruction.
            final Pointer enabledVmThreadLocals = VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(disabledVmThreadLocals).asPointer();
            trapStateAccess.setSafepointLatch(trapState, enabledVmThreadLocals);

        } else if (inJava(disabledVmThreadLocals)) {
            trapStateAccess.setTrapNumber(trapState, Number.NULL_POINTER_EXCEPTION);
            // null pointer exception
            raise(trapState, targetMethod, new NullPointerException(), stackPointer, framePointer, instructionPointer);
        } else {
            // segmentation fault happened in native code somewhere, die.
            FatalError.unexpected("Trap in native code", true, null, trapState);
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
                final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
                trapStateAccess.setInstructionPointer(trapState, catchAddress.asPointer());
                trapStateAccess.setExceptionObject(trapState, throwable);
            }
        } else {
            Throw.raise(throwable, stackPointer, framePointer, instructionPointer);
        }
    }
}

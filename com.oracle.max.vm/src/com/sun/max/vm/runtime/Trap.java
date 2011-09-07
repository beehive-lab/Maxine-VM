/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.runtime;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.runtime.Trap.Number.*;
import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * This class handles synchronous operating systems signals used to implement the following VM features:
 * <ul>
 * <li>safepoints</li>
 * <li>runtime exceptions: {@link NullPointerException}, {@link ArithmeticException}, {@link StackOverflowError}</li>
 * <li>de-opt</li>
 * </ul>
 * The execution path from an OS signal to the {@linkplain Stubs#trapStub trap stub} is as follows:
 * <ol>
 * <li>A native handler is notified of the signal (see 'vmSignalHandler' in trap.c)</li>
 * <li>The native handler analyzes the context of the signal to detect stack-overflow.</li>
 * <li>The native handler writes trap {@linkplain VmThreadLocal#TRAP_NUMBER number},
 *     {@linkplain VmThreadLocal#TRAP_FAULT_ADDRESS address} and
 *     {@link VmThreadLocal#TRAP_INSTRUCTION_POINTER pc} into thread locals.</li>
 * <li>The native handler disables safepoints by modifying the register context of the
 *     trap in (almost) the same way as {@link SafepointPoll#disable()}.</li>
 * <li>The native handler modifies the instruction pointer in the trap context to point to the
 *     entry point of the trap stub.</li>
 * <li>The native handler returns which effects a jump to the trap stub in the frame of
 *     the trapped method/function.</li>
 * </ol>
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

        public static String toExceptionName(int trapNumber) {
            switch (trapNumber) {
                case MEMORY_FAULT:
                    return "MEMORY_FAULT";
                case STACK_FAULT:
                    return "STACK_FAULT";
                case ILLEGAL_INSTRUCTION:
                    return "ILLEGAL_INSTRUCTION";
                case ARITHMETIC_EXCEPTION:
                    return "ARITHMETIC_EXCEPTION";
                case ASYNC_INTERRUPT:
                    return "ASYNC_INTERRUPT";
                case NULL_POINTER_EXCEPTION:
                    return "NULL_POINTER_EXCEPTION";
                case SAFEPOINT:
                    return "SAFEPOINT";
                default:
                    return "unknown";
            }
        }

        public static boolean isImplicitException(int trapNumber) {
            return trapNumber == ARITHMETIC_EXCEPTION || trapNumber == NULL_POINTER_EXCEPTION || trapNumber == STACK_FAULT  || trapNumber == STACK_FATAL;
        }

        public static Class<? extends Throwable> toImplicitExceptionClass(int trapNumber) {
            if (trapNumber == ARITHMETIC_EXCEPTION) {
                return ArithmeticException.class;
            } else if (trapNumber == NULL_POINTER_EXCEPTION) {
                return NullPointerException.class;
            } else if (trapNumber == STACK_FAULT || trapNumber == STACK_FATAL) {
                return StackOverflowError.class;
            }
            return null;
        }

        private Number() {
        }

        public static boolean isStackOverflow(Pointer trapFrame) {
            return vm().trapFrameAccess.getTrapNumber(trapFrame) == STACK_FAULT;
        }
    }

    private static boolean DumpStackOnTrap;
    static {
        VMOptions.addFieldOption("-XX:", "DumpStackOnTrap", Trap.class, "Reports a stack trace for every trap, regardless of the cause.", MaxineVM.Phase.PRISTINE);
    }

    /**
     * Whether to bang on the stack in the method prologue.
     */
    public static final boolean STACK_BANGING = true;

    /**
     * The number of bytes reserved in the stack as a guard area.
     * Note that SPARC code is more efficient if this is set below 6K.  Specifically, set to (6K - 1 - typical_frame_size).
     */
    public static final int stackGuardSize = 12 * Ints.K;

    /**
     * Handle to {@link #handleTrap(int, Pointer, Address)}.
     */
    public static final CriticalMethod handleTrap = new CriticalMethod(Trap.class, "handleTrap", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

    @HOSTED_ONLY
    protected Trap() {
    }

    /**
     * A VM option to enable tracing of traps, both in the C and Java parts of trap handling.
     */
    private static VMBooleanXXOption TraceTrapsOption = register(new VMBooleanXXOption("-XX:-TraceTraps", "Trace traps.") {
        @Override
        public boolean parseValue(Pointer optionValue) {
            TraceTraps = TraceTrapsOption.getValue();
            nativeSetTrapTracing(TraceTraps);
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    private static boolean TraceTraps = TraceTrapsOption.getValue();

    /**
     * Initializes the native side of trap handling by informing the C code of the address of {@link Stubs#trapStub}.
     *
     * @param the entry point of {@link Stubs#trapStub}
     */
    @C_FUNCTION
    private static native void nativeTrapInitialize(Word vmTrapHandler);

    /**
     * Updates the tracing flag for traps in the native substrate.
     */
    @C_FUNCTION // called on primordial thread
    private static native void nativeSetTrapTracing(boolean flag);

    /**
     * Installs the trap handlers using the operating system's API.
     */
    public static void initialize() {
        nativeTrapInitialize(vm().stubs.trapStub().codeStart());
        nativeSetTrapTracing(TraceTraps);
    }

    /**
     * This method is called from the {@linkplain Stubs#trapStub trap stub} and does the actual trap handling.
     *
     * @param trapNumber the trap that occurred
     * @param trapFrame the trap frame
     * @param faultAddress the faulting address that caused this trap (memory faults only)
     */
    private static void handleTrap(int trapNumber, Pointer trapFrame, Address faultAddress) {
        // From this point on until we return from the trap stub,
        // this variable is used to communicate to the VM operation thread
        // whether a thread was stopped at a safepoint or in native code
        TRAP_INSTRUCTION_POINTER.store3(Pointer.zero());

        if (trapNumber == ASYNC_INTERRUPT) {
            VmThread.current().setInterrupted();
            return;
        }

        final TrapFrameAccess tfa = vm().trapFrameAccess;
        final Pointer pc = tfa.getPC(trapFrame);
        final Object origin = checkTrapOrigin(trapNumber, trapFrame, faultAddress, pc);
        if (origin instanceof TargetMethod) {
            // the trap occurred in Java
            final TargetMethod targetMethod = (TargetMethod) origin;
            final Pointer sp = tfa.getSP(trapFrame);
            final Pointer fp = tfa.getFP(trapFrame);

            switch (trapNumber) {
                case MEMORY_FAULT:
                    handleMemoryFault(pc, targetMethod, sp, fp, trapFrame, faultAddress);
                    break;
                case STACK_FAULT:
                    // stack overflow:

                    // the native trap handler unprotected the yellow zone -
                    // propagate this to the thread object
                    VmThread.current().nativeTrapHandlerUnprotectedYellowZone();

                    raiseImplicitException(trapFrame, targetMethod, new StackOverflowError(), sp, fp, pc);
                    break; // unreachable, except when returning to a local exception handler
                case ILLEGAL_INSTRUCTION:
                    // deoptimization
                    // TODO: deoptimization
                    FatalError.unexpected("illegal instruction", false, null, trapFrame);
                    break;
                case ARITHMETIC_EXCEPTION:
                    // integer divide by zero
                    raiseImplicitException(trapFrame, targetMethod, new ArithmeticException(), sp, fp, pc);
                    break; // unreachable
                case STACK_FATAL:
                    // fatal stack overflow
                    FatalError.unexpected("fatal stack fault in red zone", false, null, trapFrame);
                    break; // unreachable
                default:
                    FatalError.unexpected("unknown trap number", false, null, trapFrame);

            }
        } else {
            // the fault occurred in native code
            Log.print("Trap in native code (or a runtime stub) @ ");
            Log.print(pc);
            Log.println(", exiting.");
            FatalError.unexpected("Trap in native code or a runtime stub", true, null, trapFrame);
        }
    }

    /**
     * Checks the origin of a trap by looking for a target method or runtime stub in the code regions. If found, this
     * method will return a reference to the {@code TargetMethod} that produced the trap. If a runtime stub produced
     * the trap, this method will return a reference to that runtime stub. Otherwise, this method returns {@code null},
     * indicating the trap occurred in native code.
     *
     * @param trapNumber the trap number
     * @param trapFrame the trap frame
     * @param faultAddress the faulting address that caused the trap (memory faults only)
     * @param pc the address of instruction causing the trap
     * @return a reference to the {@code TargetMethod} or {@link RuntimeStub} containing the instruction pointer that
     *         caused the trap or {@code null} if trap occurred in native code
     */
    private static Object checkTrapOrigin(int trapNumber, Pointer trapFrame, Address faultAddress, Pointer pc) {
        final TrapFrameAccess tfa = vm().trapFrameAccess;

        // check to see if this fault originated in a target method
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(pc);

        if (TraceTraps || DumpStackOnTrap) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.printCurrentThread(false);
            if (targetMethod != null) {
                Log.print(": Trapped in ");
                Log.printMethod(targetMethod, true);
            } else {
                Log.println(": Trapped in <unknown>");
            }
            Log.print("  Trap number=");
            Log.println(trapNumber);
            Log.print("  Instruction pointer=");
            Log.println(pc);
            Log.print("  Fault address=");
            Log.println(faultAddress);
            tfa.logTrapFrame(trapFrame);
            if (DumpStackOnTrap) {
                Throw.stackDump("Stack trace:", pc, tfa.getSP(trapFrame), tfa.getFP(trapFrame));
            }
            Log.unlock(lockDisabledSafepoints);
        }

        if (targetMethod != null) {
            return targetMethod;
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
     * @param trapFrame a pointer to the trap frame
     * @param faultAddress the address that caused the fault
     */
    private static void handleMemoryFault(Pointer instructionPointer, TargetMethod targetMethod, Pointer stackPointer, Pointer framePointer, Pointer trapFrame, Address faultAddress) {
        final Pointer dtla = currentTLA();
        final SafepointPoll safepoint = vm().safepointPoll;
        final TrapFrameAccess tfa = vm().trapFrameAccess;
        final Pointer ttla = TTLA.load(dtla);
        final Pointer safepointLatch = tfa.getSafepointLatch(trapFrame);

        if (VmThread.current().isVmOperationThread()) {
            FatalError.unexpected("Memory fault on the VM operation thread", false, null, trapFrame);
        }

        // check to see if a safepoint has been triggered for this thread
        if (safepointLatch.equals(ttla) && safepoint.isAt(instructionPointer)) {
            // a safepoint has been triggered for this thread
            final Pointer etla = ETLA.load(dtla);
            final Reference reference = VM_OPERATION.loadRef(etla);
            final VmOperation vmOperation = (VmOperation) reference.toJava();
            tfa.setTrapNumber(trapFrame, Number.SAFEPOINT);
            if (vmOperation != null) {
                TRAP_INSTRUCTION_POINTER.store3(instructionPointer);
                vmOperation.doAtSafepoint(trapFrame);
                TRAP_INSTRUCTION_POINTER.store3(Pointer.zero());
            } else {
                /*
                 * The interleaving of a mutator thread and a freezer thread below demonstrates
                 * one case where this can occur:
                 *
                 *    Mutator thread        |  VmOperationThread
                 *  ------------------------+-----------------------------------------------------------------
                 *                          |  set VM_OPERATION and trigger safepoints for mutator thread
                 *  loop: safepoint         |
                 *        enter native      |
                 *                          |  complete operation (e.g. GC)
                 *                          |  reset safepoints and clear VM_OPERATION for mutator thread
                 *        return from native|
                 *  loop: safepoint         |
                 *
                 * The first safepoint instruction above loads the address of triggered VM thread locals
                 * into the latch register. The second safepoint instruction dereferences the latch
                 * register causing the trap. That is, the VM operation thread triggered safepoints in the
                 * mutator to freeze but actually froze it as a result of the mutator making a
                 * native call between 2 safepoint instructions (it takes 2 executions of a safepoint
                 * instruction to cause the trap).
                 *
                 * The second safepoint instruction on the mutator thread will cause a trap when
                 * VM_OPERATION for the mutator is null.
                 */
            }
            // The state of the safepoint latch was TRIGGERED when the trap happened. It must be reset back to ENABLED
            // here otherwise another trap will occur as soon as the trap stub returns and re-executes the
            // safepoint instruction.
            tfa.setSafepointLatch(trapFrame, etla);

        } else if (inJava(dtla)) {
            tfa.setTrapNumber(trapFrame, Number.NULL_POINTER_EXCEPTION);
            // null pointer exception
            raiseImplicitException(trapFrame, targetMethod, new NullPointerException(), stackPointer, framePointer, instructionPointer);
        } else {
            // segmentation fault happened in native code somewhere, die.
            FatalError.unexpected("Trap in native code", true, null, trapFrame);
        }
    }

    /**
     * Raises an implicit exception.
     *
     * If there is a local handler for the exception (i.e. a handler in the same frame in which the exception occurred)
     * and the method in which the exception occurred was compiled by the opto compiler, then the trap state is altered
     * so that the return address for the trap frame is set to be the exception handler entry address.
     * This means that the register allocator can assume that registers are not modified in the control flow
     * from an implicit exception to the exception handler.
     *
     * Otherwise, the {@linkplain Throw#raise(Throwable, Pointer, Pointer, Pointer) standard mechanism} for throwing an
     * exception is used.
     *
     * @param trapFrame a pointer to the trap frame
     * @param targetMethod the target method containing the trap address
     * @param throwable the throwable to raise
     * @param sp the stack pointer at the time of the trap
     * @param fp the frame pointer at the time of the trap
     * @param ip the instruction pointer which caused the trap
     */
    private static void raiseImplicitException(Pointer trapFrame, TargetMethod targetMethod, Throwable throwable, Pointer sp, Pointer fp, Pointer ip) {
        Throw.traceThrow(throwable);
        if (targetMethod.preserveRegistersForLocalExceptionHandler()) {
            final Address catchAddress = targetMethod.throwAddressToCatchAddress(true, ip, throwable.getClass());
            if (!catchAddress.isZero()) {
                // Store the exception so that the handler can find it.
                VmThread.current().storeExceptionForHandler(throwable, targetMethod, targetMethod.posFor(catchAddress));

                final TrapFrameAccess tfa = vm().trapFrameAccess;
                tfa.setPC(trapFrame, catchAddress.asPointer());
                return;
            }
        }
        Throw.raise(throwable, sp, fp, ip);
    }
}

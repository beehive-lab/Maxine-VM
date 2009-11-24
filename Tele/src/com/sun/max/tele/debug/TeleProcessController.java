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
package com.sun.max.tele.debug;

import com.sun.max.program.*;
import com.sun.max.tele.debug.TeleTargetBreakpoint.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * Manages debugging operations on the {@link TeleProcess}.
 *
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class TeleProcessController {

    private static final int TRACE_VALUE = 2;

    private static final String RUN_TO_INSTRUCTION = "runToInstruction";
    private static final String TERMINATE = "terminate";
    private static final String PAUSE = "pause";
    private static final String RESUME = "resume";
    private static final String SINGLE_STEP = "singleStep";
    private static final String STEP_OVER = "stepOver";

    private String  tracePrefix() {
        return "[TeleProcessController: " + Thread.currentThread().getName() + "] ";
    }

    TeleProcessController(TeleProcess teleProcess) {
        this.teleProcess = teleProcess;
    }

    private TeleProcess teleProcess;

    /**
     * @return the {@link TeleProcess} associated with this controller.
     */
    public TeleProcess teleProcess() {
        return teleProcess;
    }

    private final Object runToInstructionScheduleTracer = new Tracer(RUN_TO_INSTRUCTION, "schedule");
    private final Object runToInstructionPerformTracer = new Tracer(RUN_TO_INSTRUCTION, "perform");

    /**
     * Resumes a process to make it run until a given destination instruction. All breakpoints encountered between the
     * current instruction and the destination instruction are ignored.
     *
     * @param instructionPointer the destination instruction
     * @throws InvalidProcessRequestException
     * @throws OSExecutionRequestException
     */
    public void runToInstruction(final Address instructionPointer, final boolean synchronous, final boolean disableBreakpoints) throws OSExecutionRequestException, InvalidProcessRequestException {
        Trace.begin(TRACE_VALUE, runToInstructionScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(RUN_TO_INSTRUCTION, null) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, runToInstructionPerformTracer);
                updateWatchpointCaches();
                final Factory breakpointFactory = teleProcess.targetBreakpointFactory();

                // Create a temporary breakpoint if there is not already an enabled, non-persistent breakpoint for the target address:
                TeleTargetBreakpoint breakpoint = breakpointFactory.getClientTargetBreakpointAt(instructionPointer);
                if (breakpoint == null || !breakpoint.isEnabled()) {
                    breakpoint = breakpointFactory.makeTransientBreakpoint(instructionPointer);
                    breakpoint.setDescription("transient breakpoint for the low-level run-to-instruction operation");
                }

                for (TeleNativeThread thread : teleProcess().threads()) {
                    thread.evadeBreakpoint();
                }

                if (!disableBreakpoints) {
                    breakpointFactory.setActiveAll(true);
                } else {
                    breakpoint.setActive(true);
                }
                teleProcess().resume();
                Trace.end(TRACE_VALUE, runToInstructionPerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
        Trace.end(TRACE_VALUE, runToInstructionScheduleTracer);
    }

    private final Object terminatePerformTracer = new Tracer(TERMINATE, "perform");

    public void terminate() throws Exception {
        Trace.begin(TRACE_VALUE, terminatePerformTracer);
        teleProcess().terminate();
        Trace.end(TRACE_VALUE, terminatePerformTracer);
    }

    private final Object pausePerformTracer = new Tracer(PAUSE, "perform");

    public void pause() throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, pausePerformTracer);
        teleProcess().pause();
        Trace.end(TRACE_VALUE, pausePerformTracer);
    }

    private final Object resumeScheduleTracer = new Tracer(RESUME, "schedule");
    private final Object resumePerformTracer = new Tracer(RESUME, "perform");

    public void resume(final boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, resumeScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(RESUME, null) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, resumePerformTracer);
                updateWatchpointCaches();
                for (TeleNativeThread thread : teleProcess().threads()) {
                    thread.evadeBreakpoint();
                }
                if (!disableBreakpoints) {
                    teleProcess.targetBreakpointFactory().setActiveAll(true);
                }
                teleProcess().resume();
                Trace.end(TRACE_VALUE, resumePerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
        Trace.end(TRACE_VALUE, resumeScheduleTracer);
    }

    private final Object singleStepScheduleTracer = new Tracer(SINGLE_STEP, "schedule");
    private final Object singleStepPerformTracer = new Tracer(SINGLE_STEP, "perform");

    public void singleStep(final TeleNativeThread thread, boolean isSynchronous) throws InvalidProcessRequestException, OSExecutionRequestException    {
        Trace.begin(TRACE_VALUE, singleStepScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(SINGLE_STEP, thread) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, singleStepPerformTracer);
                updateWatchpointCaches();
                teleProcess().singleStep(thread, false);
                Trace.end(TRACE_VALUE, singleStepPerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, isSynchronous);
        Trace.end(TRACE_VALUE, singleStepScheduleTracer);
    }

    private final Object stepOverScheduleTracer = new Tracer(STEP_OVER, "schedule");
    private final Object stepOverPerformTracer = new Tracer(STEP_OVER, "perform");

    public void stepOver(final TeleNativeThread thread, boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, stepOverScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(STEP_OVER, thread) {

            private Pointer oldInstructionPointer;
            private Pointer oldReturnAddress;

            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, stepOverPerformTracer);
                updateWatchpointCaches();
                oldInstructionPointer = thread.instructionPointer();
                oldReturnAddress = thread.getReturnAddress();
                teleProcess().singleStep(thread, false);
                Trace.end(TRACE_VALUE, stepOverPerformTracer);
            }

            @Override
            public void notifyProcessStopped() {
                final Pointer stepOutAddress = getStepoutAddress(thread, oldReturnAddress, oldInstructionPointer, thread.instructionPointer());
                if (stepOutAddress != null) {
                    try {
                        runToInstruction(stepOutAddress, true, disableBreakpoints);
                    } catch (OSExecutionRequestException e) {
                        e.printStackTrace();
                    } catch (InvalidProcessRequestException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
        Trace.end(TRACE_VALUE, stepOverScheduleTracer);
    }

    /**
     * Given the instruction pointer before and after a single step, this method determines if the step represents a call
     * from one target method to another (or a recursive call from a target method to itself) and, if so, returns the
     * address of the next instruction that will be executed in the target method that is the origin of the step (i.e.
     * the return address of the call).
     *
     * @param thread the executing thread
     * @param oldReturnAddress the return address of the thread just before the single step
     * @param oldInstructionPointer the instruction pointer of the thread just before the single step
     * @param newInstructionPointer the instruction pointer of the thread just after the single step
     * @return if {@code oldInstructionPointer} and {@code newInstructionPointer} indicate two different target methods
     *         or a recursive call to the same target method, then the return address of the call is returned.
     *         Otherwise, null is returned, indicating that the step over is really just a single step.
     */
    private Pointer getStepoutAddress(TeleNativeThread thread, Pointer oldReturnAddress, Pointer oldInstructionPointer, Pointer newInstructionPointer) {
        if (newInstructionPointer.equals(oldReturnAddress)) {
            // Executed a return
            return null;
        }
        final TeleTargetMethod oldTeleTargetMethod = TeleTargetMethod.make(teleProcess.teleVM(), oldInstructionPointer);
        if (oldTeleTargetMethod == null) {
            // Stepped from native code:
            return null;
        }
        final TeleTargetMethod newTeleTargetMethod = TeleTargetMethod.make(teleProcess.teleVM(), newInstructionPointer);
        if (newTeleTargetMethod == null) {
            // Stepped into native code:
            return null;
        }
        if (oldTeleTargetMethod != newTeleTargetMethod || newTeleTargetMethod.callEntryPoint().equals(newInstructionPointer)) {
            // Stepped into a different target method or back into the entry of the same target method (i.e. a recursive call):
            return thread.getReturnAddress();
        }
        // Stepped over a normal, non-call instruction:
        return null;
    }

    private void updateWatchpointCaches() {
        teleProcess.watchpointFactory().updateWatchpointCaches();
    }

    /**
     * An object that delays evaluation of a trace message for controller actions.
     */
    private class Tracer {

        private final String processAction;
        private final String controllerAction;

        /**
         * An object that delays evaluation of a trace message.
         * @param processAction the name of the process action being requested
         * @param controllerAction the step being taken by the controller
         */
        public Tracer(String processAction, String controllerAction) {
            this.processAction = processAction;
            this.controllerAction = controllerAction;
        }
        @Override
        public String toString() {
            return tracePrefix() + controllerAction + " " + processAction;
        }
    }
}

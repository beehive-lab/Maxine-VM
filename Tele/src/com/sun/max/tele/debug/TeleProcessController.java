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
 * Controls the {@link TeleProcess} to execute different debugging operations and separates it from the GUI.
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
        _teleProcess = teleProcess;
    }

    private TeleProcess _teleProcess;

    /**
     * Gets the {@link TeleProcess} associated with this controller.
     */
    public TeleProcess teleProcess() {
        return _teleProcess;
    }

    private final Object _runToInstructionScheduleTracer = new Tracer(RUN_TO_INSTRUCTION, "schedule");
    private final Object _runToInstructionPerformTracer = new Tracer(RUN_TO_INSTRUCTION, "perform");

    /**
     * Resumes a process to make it run until a given destination instruction. All breakpoints encountered between the
     * current instruction and the destination instruction are ignored.
     *
     * @param instructionPointer the destination instruction
     * @throws InvalidProcessRequestException
     * @throws OSExecutionRequestException
     */
    public void runToInstruction(final Address instructionPointer, final boolean synchronous, final boolean disableBreakpoints) throws OSExecutionRequestException, InvalidProcessRequestException {
        Trace.begin(TRACE_VALUE, _runToInstructionScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(RUN_TO_INSTRUCTION, null) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, _runToInstructionPerformTracer);
                final Factory breakpointFactory = _teleProcess.targetBreakpointFactory();

                // Create a temporary breakpoint if there is not already an enabled, non-persistent breakpoint for the target address:
                TeleTargetBreakpoint breakpoint = breakpointFactory.getNonTransientBreakpointAt(instructionPointer);
                if (breakpoint == null || !breakpoint.isEnabled()) {
                    breakpoint = breakpointFactory.makeBreakpoint(instructionPointer, true);
                }

                for (TeleNativeThread thread : teleProcess().threads()) {
                    thread.evadeBreakpoint();
                }

                if (!disableBreakpoints) {
                    breakpointFactory.activateAll();
                } else {
                    breakpoint.activate();
                }
                teleProcess().resume();
                Trace.end(TRACE_VALUE, _runToInstructionPerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
        Trace.end(TRACE_VALUE, _runToInstructionScheduleTracer);
    }


    private final Object _terminatePerformTracer = new Tracer(TERMINATE, "perform");

    public void terminate() throws Exception {
        Trace.begin(TRACE_VALUE, _terminatePerformTracer);
        teleProcess().terminate();
        Trace.end(TRACE_VALUE, _terminatePerformTracer);
    }


    private final Object _pausePerformTracer = new Tracer(PAUSE, "perform");

    public void pause() throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, _pausePerformTracer);
        teleProcess().pause();
        Trace.end(TRACE_VALUE, _pausePerformTracer);
    }


    private final Object _resumeScheduleTracer = new Tracer(RESUME, "schedule");
    private final Object _resumePerformTracer = new Tracer(RESUME, "perform");

    public void resume(final boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, _resumeScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(RESUME, null) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, _resumePerformTracer);
                for (TeleNativeThread thread : teleProcess().threads()) {
                    thread.evadeBreakpoint();
                }
                if (!disableBreakpoints) {
                    _teleProcess.targetBreakpointFactory().activateAll();
                }
                teleProcess().resume();
                Trace.end(TRACE_VALUE, _resumePerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
        Trace.end(TRACE_VALUE, _resumeScheduleTracer);
    }


    private final Object _singleStepScheduleTracer = new Tracer(SINGLE_STEP, "schedule");
    private final Object _singleStepPerformTracer = new Tracer(SINGLE_STEP, "perform");

    public void singleStep(final TeleNativeThread thread, boolean isSynchronous) throws InvalidProcessRequestException, OSExecutionRequestException    {
        Trace.begin(TRACE_VALUE, _singleStepScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(SINGLE_STEP, thread) {
            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, _singleStepPerformTracer);
                teleProcess().performSingleStep(thread);
                Trace.end(TRACE_VALUE, _singleStepPerformTracer);
            }
        };
        teleProcess().scheduleRequest(request, isSynchronous);
        Trace.end(TRACE_VALUE, _singleStepScheduleTracer);
    }


    private final Object _stepOverScheduleTracer = new Tracer(STEP_OVER, "schedule");
    private final Object _stepOverPerformTracer = new Tracer(STEP_OVER, "perform");

    public void stepOver(final TeleNativeThread thread, boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, OSExecutionRequestException {
        Trace.begin(TRACE_VALUE, _stepOverScheduleTracer);
        final TeleEventRequest request = new TeleEventRequest(STEP_OVER, thread) {

            private Pointer _oldInstructionPointer;
            private Pointer _oldReturnAddress;

            @Override
            public void execute() throws OSExecutionRequestException {
                Trace.begin(TRACE_VALUE, _stepOverPerformTracer);
                _oldInstructionPointer = thread.instructionPointer();
                _oldReturnAddress = thread.getReturnAddress();
                teleProcess().performSingleStep(thread);
                Trace.end(TRACE_VALUE, _stepOverPerformTracer);
            }

            @Override
            public void notifyProcessStopped() {
                final Pointer stepOutAddress = getStepoutAddress(thread, _oldReturnAddress, _oldInstructionPointer, thread.instructionPointer());
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
        Trace.end(TRACE_VALUE, _stepOverScheduleTracer);
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
     *         Otherwise, null is returned, indicating that the step over is really a single step.
     */
    private Pointer getStepoutAddress(TeleNativeThread thread, Pointer oldReturnAddress, Pointer oldInstructionPointer, Pointer newInstructionPointer) {
        if (newInstructionPointer.equals(oldReturnAddress)) {
            // Executed a return
            return null;
        }
        final TeleTargetMethod oldTeleTargetMethod = TeleTargetMethod.make(_teleProcess.teleVM(), oldInstructionPointer);
        if (oldTeleTargetMethod == null) {
            // Stepped from native code:
            return null;
        }
        final TeleTargetMethod newTeleTargetMethod = TeleTargetMethod.make(_teleProcess.teleVM(), newInstructionPointer);
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

    /**
     * An object that delays evaluation of a trace message for controller actions.
     */
    private class Tracer {

        private final String _processAction;
        private final String _controllerAction;

        /**
         * An object that delays evaluation of a trace message.
         * @param processAction the name of the process action being requested
         * @param controllerAction the step being taken by the controller
         */
        public Tracer(String processAction, String controllerAction) {
            _processAction = processAction;
            _controllerAction = controllerAction;
        }
        @Override
        public String toString() {
            return tracePrefix() + _controllerAction + " " + _processAction;
        }
    }
}

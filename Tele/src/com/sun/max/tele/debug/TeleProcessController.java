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

import com.sun.max.tele.debug.TeleTargetBreakpoint.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


/**
 * Controls the {@link TeleProcess} to execute different debugging operations and separates it from the GUI.
 *
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 */
public final class TeleProcessController {

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

    /**
     * Resumes a process to make it run until a given destination instruction. All breakpoints encountered between the
     * current instruction and the destination instruction are ignored.
     *
     * @param instructionPointer the destination instruction
     * @throws InvalidProcessRequestException
     * @throws ExecutionRequestException
     */
    public void runToInstruction(final Address instructionPointer, final boolean synchronous, final boolean disableBreakpoints) throws ExecutionRequestException, InvalidProcessRequestException {
        final TeleEventRequest request = new TeleEventRequest("runToInstruction", null) {
            @Override
            public void execute() throws ExecutionRequestException {
                final Factory breakpointFactory = _teleProcess.targetBreakpointFactory();

                // Create a temporary breakpoint if there is not already an enabled, non-persistent breakpoint for the target address:
                TeleTargetBreakpoint breakpoint = breakpointFactory.getNonTransientBreakpointAt(instructionPointer);
                if (breakpoint == null || !breakpoint.enabled()) {
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
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
    }

    public void terminate() throws Exception {
        teleProcess().terminate();
    }

    public void pause() throws InvalidProcessRequestException, ExecutionRequestException {
        teleProcess().pause();
    }

    public void resume(final boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, ExecutionRequestException {
        final TeleEventRequest request = new TeleEventRequest("resume", null) {
            @Override
            public void execute() throws ExecutionRequestException {
                for (TeleNativeThread thread : teleProcess().threads()) {
                    thread.evadeBreakpoint();
                }
                if (!disableBreakpoints) {
                    _teleProcess.targetBreakpointFactory().activateAll();
                }
                teleProcess().resume();
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
    }

    public void singleStep(final TeleNativeThread thread, boolean isSynchronous) throws InvalidProcessRequestException, ExecutionRequestException    {
        final TeleEventRequest request = new TeleEventRequest("singleStep", thread) {
            @Override
            public void execute() throws ExecutionRequestException {
                teleProcess().performSingleStep(thread);
            }
        };
        teleProcess().scheduleRequest(request, isSynchronous);
    }

    public void stepOver(final TeleNativeThread thread, boolean synchronous, final boolean disableBreakpoints) throws InvalidProcessRequestException, ExecutionRequestException {
        final TeleEventRequest request = new TeleEventRequest("stepOver", thread) {

            private Pointer _oldInstructionPointer;
            private Pointer _oldReturnAddress;

            @Override
            public void execute() throws ExecutionRequestException {
                _oldInstructionPointer = thread.instructionPointer();
                _oldReturnAddress = thread.getReturnAddress();
                teleProcess().performSingleStep(thread);
            }

            @Override
            public void notifyProcessStopped() {
                final Pointer stepOutAddress = getStepoutAddress(thread, _oldReturnAddress, _oldInstructionPointer, thread.instructionPointer());
                if (stepOutAddress != null) {
                    try {
                        runToInstruction(stepOutAddress, true, disableBreakpoints);
                    } catch (ExecutionRequestException e) {
                        e.printStackTrace();
                    } catch (InvalidProcessRequestException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        teleProcess().scheduleRequest(request, synchronous);
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
    public Pointer getStepoutAddress(TeleNativeThread thread, Pointer oldReturnAddress, Pointer oldInstructionPointer, Pointer newInstructionPointer) {
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
}

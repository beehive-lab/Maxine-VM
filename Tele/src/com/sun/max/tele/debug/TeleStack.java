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

import java.io.*;
import java.lang.management.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Access to the state of a stack in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleStack extends AbstractTeleVMHolder implements MaxStack {

    private static final int TRACE_LEVEL = 2;

    /**
     * Description of the memory region occupied by a {@linkplain MaxStack teleStack}.
     * <br>
     * This region has no parent; it is allocated from the OS.
     * <br>
     * This region's children are the individual {@linkplain MaxStackFrame teleStack frames}
     * contained in the teleStack.
     */
    private static final class StackMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxStack> {

        private TeleStack teleStack;
        private MaxVMState lastUpdatedState = null;

        private StackMemoryRegion(TeleVM teleVM, TeleStack owner, String regionName, Address start, Size size) {
            super(teleVM, regionName, start, size);
            this.teleStack = owner;
        }

        @Override
        public MemoryUsage getUsage() {
            return new MemoryUsage(-1, end().minus(teleStack.thread().registers().stackPointer()).toLong(), size().toLong(), -1);
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Stack memory is allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            final List<MaxStackFrame> frames = teleStack.frames();
            final List<MaxEntityMemoryRegion<? extends MaxEntity>> regions =
                new ArrayList<MaxEntityMemoryRegion<? extends MaxEntity>>(frames.size());
            for (MaxStackFrame stackFrame : frames) {
                regions.add(stackFrame.memoryRegion());
            }
            return Collections.unmodifiableList(regions);
        }

        public MaxStack owner() {
            return teleStack;
        }

        public boolean isBootRegion() {
            return false;
        }
    }

    private final TeleNativeThread teleNativeThread;

    private final String entityDescription;

    /**
     * The region of VM memory occupied by this teleStack.
     */
    private final StackMemoryRegion stackMemoryRegion;

    /**
     * Location of the caller return address relative to the saved location in a teleStack frame, usually 0 but see SPARC.
     */
    private final int  offsetToReturnPC;

    /**
     * VM state the last time we updated the frames.
     */
    private volatile TeleVMState lastUpdatedState = null;

    /**
     * VM state the last time the teleStack changed "structurally".
     */
    private volatile TeleVMState lastChangedState = null;

    /**
     * Most recently updated teleStack frames; may be empty, but non-null.
     */
    private volatile List<MaxStackFrame> maxStackFrames = Collections.emptyList();

    /**
     * Creates an object that models a teleStack in the VM.
     *
     * @param teleVM the VM
     * @param teleNativeThread the thread that owns the teleStack
     * @param teleFixedMemoryRegion description of the memory occupied by the teleStack
     */
    public TeleStack(TeleVM teleVM, TeleNativeThread teleNativeThread, String name, Address start, Size size) {
        super(teleVM);
        this.teleNativeThread = teleNativeThread;
        this.entityDescription = "The stack in " + vm().entityName() + " for " + teleNativeThread.entityName();
        this.stackMemoryRegion = new StackMemoryRegion(teleVM, this, name, start, size);
        this.offsetToReturnPC = teleVM.vmConfiguration().platform.instructionSet().offsetToReturnPC;
    }

    public String entityName() {
        return stackMemoryRegion.regionName();
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxStack> memoryRegion() {
        return stackMemoryRegion;
    }

    public boolean contains(Address address) {
        return stackMemoryRegion.contains(address);
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public MaxStackFrame top() {
        return frames().get(0);
    }

    public List<MaxStackFrame> frames() {
        final TeleVMState currentVmState = vm().state();
        if (currentVmState.newerThan(lastUpdatedState)) {
            if (vm().tryLock()) {
                try {
                    final List<StackFrame> frames = teleNativeThread.frames();
                    final List<MaxStackFrame> maxStackFrames = new ArrayList<MaxStackFrame>(frames.size());
                    int position = 0;
                    for (StackFrame stackFrame : frames) {
                        maxStackFrames.add(TeleStackFrame.createFrame(vm(), this, position, stackFrame));
                        position++;
                    }
                    this.maxStackFrames = maxStackFrames;
                    lastUpdatedState = currentVmState;

                    final Long lastChangedEpoch = teleNativeThread.framesLastChangedEpoch();
                    lastChangedState = currentVmState;
                    while (lastChangedState.previous() != null && lastChangedEpoch <= lastChangedState.previous().epoch()) {
                        lastChangedState = (TeleVMState) lastChangedState.previous();
                    }

                } finally {
                    vm().unlock();
                }
            }
        }
        return maxStackFrames;
    }

    public MaxStackFrame findStackFrame(Address address) {
        for (MaxStackFrame stackFrame : frames()) {
            if (stackFrame.memoryRegion().contains(address)) {
                return stackFrame;
            }
        }
        return null;
    }

    public TeleVMState lastUpdated() {
        return lastUpdatedState;
    }

    public TeleVMState lastChanged() {
        return lastChangedState;
    }

    public void writeSummary(PrintStream printStream) {
        printStream.println("Stack frames :");
        for (MaxStackFrame maxStackFrame : new ArrayList<MaxStackFrame>(frames())) {
            printStream.println("  " + maxStackFrame.toString());
        }
    }

    public CodeLocation returnLocation() {
        final StackFrame topFrame = teleNativeThread.frames().get(0);
        final StackFrame topFrameCaller = topFrame.callerFrame();
        if (topFrameCaller == null) {
            return null;
        }
        Pointer instructionPointer = topFrameCaller.ip;
        if (instructionPointer.isZero()) {
            return null;
        }
        final StackFrame callee = topFrameCaller.calleeFrame();
        if (callee == null) {
            // Top frame, not a call return so no adjustment.
            return codeManager().createMachineCodeLocation(instructionPointer, "top teleStack frame IP");
        }
        // Add a platform-specific offset from the stored code address to the actual call return site.
        final TargetMethod calleeTargetMethod = callee.targetMethod();
        if (calleeTargetMethod != null) {
            final ClassMethodActor calleeClassMethodActor = calleeTargetMethod.classMethodActor();
            if (calleeClassMethodActor != null) {
                if (calleeClassMethodActor.isTrapStub()) {
                    // Special case, where the IP caused a trap; no adjustment.
                    return codeManager().createMachineCodeLocation(instructionPointer, "teleStack frame return");
                }
            }
        }
        // An ordinary call; apply a platform-specific adjustment to get the real return address.
        return codeManager().createMachineCodeLocation(instructionPointer.plus(offsetToReturnPC), "teleStack frame return");
    }

}

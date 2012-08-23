/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.stack.*;

/**
 * Access to the state of a stack in the VM.
 */
public class TeleStack extends AbstractVmHolder implements MaxStack {

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

        private StackMemoryRegion(MaxVM vm, TeleStack owner, String regionName, Address start, long nBytes) {
            super(vm, regionName, start, nBytes);
            this.teleStack = owner;
        }

        @Override
        public MemoryUsage getUsage() {
            if (start().isNotZero() && nBytes() != 0L) {
                try {
                    return new MemoryUsage(-1L, end().minus(teleStack.thread().registers().stackPointer()).toLong(), nBytes(), -1L);
                } catch (IllegalArgumentException e) {
                }
            }
            return MaxMemoryRegion.Util.NULL_MEMORY_USAGE;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // Stack memory is allocated from the OS, not part of any other region
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            final List<MaxStackFrame> frames = teleStack.frames(Integer.MAX_VALUE);
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
     * @param teleNativeThread the thread that owns the teleStack
     * @param name name of the stack
     * @param start starting location in VM memory
     * @param nBytes size of the stack region in bytes
     */
    public TeleStack(TeleVM vm, TeleNativeThread teleNativeThread, String name, Address start, long nBytes) {
        super(vm);
        this.teleNativeThread = teleNativeThread;
        this.entityDescription = "The stack owned by VM thread " + teleNativeThread.entityName();
        this.stackMemoryRegion = new StackMemoryRegion(vm, this, name, start, nBytes);
        this.offsetToReturnPC = platform().isa.offsetToReturnPC;
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

    public TeleObject representation() {
        // No distinguished object in VM runtime represents the stack.
        return null;
    }
    public MaxThread thread() {
        return teleNativeThread;
    }

    public MaxStackFrame top() {
        if (vm().tryLock()) {
            try {
                return TeleStackFrame.createFrame(vm(), this, 0, teleNativeThread.top());
            } finally {
                vm().unlock();
            }
        }
        return null;
    }

    public List<MaxStackFrame> frames(int maxDepth) {
        final TeleVMState currentVmState = vm().state();
        if (currentVmState.newerThan(lastUpdatedState) || teleNativeThread.framesMaxDepth() != maxDepth) {
            if (vm().tryLock()) {
                try {
                    final List<StackFrame> frames = teleNativeThread.frames(maxDepth);
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
        for (MaxStackFrame stackFrame : frames(Integer.MAX_VALUE)) {
            MaxEntityMemoryRegion<MaxStackFrame> memoryRegion = stackFrame.memoryRegion();
            if (memoryRegion != null && memoryRegion.contains(address)) {
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
        for (MaxStackFrame maxStackFrame : new ArrayList<MaxStackFrame>(frames(Integer.MAX_VALUE))) {
            printStream.println("  " + maxStackFrame.toString());
        }
    }

    public CodeLocation returnLocation() {
        final StackFrame topFrameCaller = teleNativeThread.top().callerFrame();
        if (topFrameCaller != null && topFrameCaller.ip.isNotZero()) {
            final StackFrame callee = topFrameCaller.calleeFrame();
            if (callee == null) {
                // Top frame
                try {
                    return codeLocations().createMachineCodeLocation(topFrameCaller.ip, "top teleStack frame IP");
                } catch (InvalidCodeAddressException e) {
                    TeleWarning.message("Bad IP address " + e.getAddressString() + " for top frame in thread " + teleNativeThread.entityName() + ": " + e.getMessage());
                }
            } else {
                // A call return: add a platform-specific offset from the stored code address to the actual call return site.
                final TargetMethod calleeTargetMethod = callee.targetMethod();
                if (calleeTargetMethod != null && calleeTargetMethod.is(TrapStub)) {
                    // Special case, where the IP caused a trap; no adjustment.
                    try {
                        return codeLocations().createMachineCodeLocation(topFrameCaller.ip, "teleStack frame return");
                    } catch (InvalidCodeAddressException e) {
                        TeleWarning.message("Bad IP address " + e.getAddressString() + " for trap stub frame in thread " + teleNativeThread.entityName() + ": " + e.getMessage());
                    }
                } else {
                    // An ordinary call; apply a platform-specific adjustment to get the real return address.
                    try {
                        return codeLocations().createMachineCodeLocation(topFrameCaller.ip.plus(offsetToReturnPC), "teleStack frame return");
                    } catch (InvalidCodeAddressException e) {
                        TeleWarning.message("Bad IP address " + e.getAddressString() + " for frame in thread " + teleNativeThread.entityName() + ": " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }

}

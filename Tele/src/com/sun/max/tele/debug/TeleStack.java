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

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.vm.stack.*;

/**
 * Description of a stack in the VM.
 *
 * @author Michael Van De Vanter
 */
public class TeleStack extends AbstractTeleVMHolder implements MaxStack {

    private final TeleNativeThread teleNativeThread;
    private final TeleNativeStackMemoryRegion memoryRegion;

    /**
     * VM state the last time we updated the frames.
     */
    private volatile TeleVMState lastUpdatedState = null;

    /**
     * VM state the last time the stack changed "structurally".
     */
    private volatile TeleVMState lastChangedState = null;

    /**
     * Most recently updated stack frames; may be empty, but non-null.
     */
    private volatile IndexedSequence<MaxStackFrame> maxStackFrames = IndexedSequence.Static.empty(MaxStackFrame.class);

    public TeleStack(TeleVM teleVM, TeleNativeThread teleNativeThread, TeleNativeStackMemoryRegion memoryRegion) {
        super(teleVM);
        this.teleNativeThread = teleNativeThread;
        this.memoryRegion = memoryRegion;
    }

    public MaxThread thread() {
        return teleNativeThread;
    }

    public MemoryRegion memoryRegion() {
        return memoryRegion;
    }

    public MaxStackFrame top() {
        return frames().first();
    }

    public IndexedSequence<MaxStackFrame> frames() {
        final TeleVMState currentVmState = teleVM().vmState();
        if (currentVmState.newerThan(lastUpdatedState)) {
            if (teleVM().tryLock()) {
                try {
                    final IndexedSequence<StackFrame> frames = teleNativeThread.frames();
                    final VariableSequence<MaxStackFrame> maxStackFrames = new VectorSequence<MaxStackFrame>(frames.length());
                    int position = 0;
                    for (StackFrame stackFrame : frames) {
                        maxStackFrames.append(TeleStackFrame.createFrame(teleVM(), this, position, stackFrame));
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
                    teleVM().unlock();
                }
            }
        }
        return maxStackFrames;
    }

    public TeleVMState lastUpdated() {
        return lastUpdatedState;
    }

    public TeleVMState lastChanged() {
        return lastChangedState;
    }

    public void writeSummary(PrintStream printStream) {
        printStream.println("Stack frames :");
        for (MaxStackFrame maxStackFrame : frames().clone()) {
            printStream.println("  " + maxStackFrame.toString());
        }
    }

}

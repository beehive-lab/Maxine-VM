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
package com.sun.max.tele;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;

/**
 * The stack for a thread in the VM.
 *
 * @author Michael Van De Vanter
 */
public interface MaxStack {

    /**
     * Gets the thread that owns the stack; doesn't change.
     * <br>
     * Thread-safe
     *
     * @return the thread that owns this stack.
     */
    MaxThread thread();

    /**
     * Gets the allocated memory region.
     * <br>
     * Thread-safe
     *
     * @return the memory in the VM allocated for this stack.
     */
    MemoryRegion memoryRegion();

    /**
     * Gets the top frame from the currently updated stack.  If the VM is busy,
     * then the top of the previous stack is returned.
     * <br>
     * Thread-safe
     *
     * @return the to frame in the stack
     */
    MaxStackFrame top();

    /**
     * Gets the frames currently in the stack.  If the VM is busy,
     * then previous value is returned.
     * <br>
     * Thread-safe
     *
     * @return the frames in the stack
     */
    IndexedSequence<MaxStackFrame> frames();

    /**
     * Gets the frame, if any, whose memory location in the VM includes an address.
     *
     * @param address a memory location in the VM
     * @return the stack frame whose location includes the address, null if none.
     */
    MaxStackFrame findStackFrame(Address address);

    /**
     * Identifies the point in VM state history where this information was most recently updated.
     * <br>
     * Thread-safe
     *
     * @return the VM state recorded the last time this information was last updated.
     */
    MaxVMState lastUpdated();

    /**
     * Identifies the last point in VM state history when the stack "structurally" changed.
     * The stack is understood to be unchanged if the length is unchanged and the frames
     * are all equivalent in content (even if the object representing them differ) with the
     * exception of the top frame.
     * <br>
     * Thread-safe
     *
     * @return the last VM state at which the stack structurally changed.
     */
    MaxVMState lastChanged();

    /**
     * Writes a textual description of each stack frame.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    void writeSummary(PrintStream printStream);

}

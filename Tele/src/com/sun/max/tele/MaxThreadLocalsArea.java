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

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Access to the {@linkplain VmThreadLocal thread local variables} related to a particular
 * {@linkplain SafePoint.State safepoint state} for {@linkplain MaxThread thread} in the VM.
 * The variables are stored in a region of VM memory local to the thread that does not move.
 * Variables are word sized, stored in index-order, and are accessible by either name or index.
 * If the region starts at {@link Address#zero()} then the {@linkplain VmThreadLocal thread local variables}
 * are assumed to be invalid.
 *
 * @author Michael Van De Vanter
 */
public interface MaxThreadLocalsArea {

    /**
     * Gets the thread containing this thread locals area in the VM.
     * <br>
     * Thread-safe
     *
     * @return the thread holding these locals
     */
    MaxThread thread();

    /**
     * Gets the {@linkplain SafePoint.State safepoint state} with which this thread locals area is associated in the VM.
     * <br>
     * Thread-safe
     *
     * @return the state
     */
    Safepoint.State safepointState();

    /**
     * Gets a description of the memory occupied by this thread locals area in the VM.
     * <br>
     * Thread-safe
     *
     * @return the memory occupied by this thread locals area in the VM.
     */
    MemoryRegion memoryRegion();

    /**
     * Gets the number of thread local variables in this thread locals area in the VM.
     * <br>
     * Thread-safe
     *
     * @return the number of {@linkplain MaxThreadLocalVariable thread local variables}
     */
    int variableCount();

    /**
     * Gets a thread local variable in this thread locals area,  by index.
     * <br>
     * Thread-safe
     *
     * @return the {@linkplain MaxThreadLocalVariable thread local variable} at a specified index.
     */
    MaxThreadLocalVariable getThreadLocalVariable(int index);

    /**
     * Finds by address the thread local in this block that starts at this address in the thread locals area in the VM, if any.
     * <br>
     * Thread-safe
     *
     * @return the {@linkplain MaxThreadLocalVariable thread local variable} in this thread locals area that is stored at a particular memory location in the VM, null if none.
     */
    MaxThreadLocalVariable findThreadLocalVariable(Address address);

}

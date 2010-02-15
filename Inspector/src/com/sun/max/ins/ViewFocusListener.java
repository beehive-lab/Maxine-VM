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
package com.sun.max.ins;

import com.sun.max.memory.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.stack.*;

/**
 * Interface for listening to changes of focus (view state) in the Inspector.
 *
 * @author Michael Van De Vanter
 */
public interface ViewFocusListener {

    /**
     * Notifies that the global code location focus has been set (view state only), even if unchanged.
     *
     * @param codeLocation
     * @param interactiveForNative if true, user is given an opportunity to add information
     * when location is determined to be in unknown native code.
     */
    void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative);

    /**
     * Notifies that the global thread focus has been set (view state only), non-null once running.
     */
    void threadFocusSet(MaxThread oldThread, MaxThread thread);

    /**
     * Notifies that the global stack frame focus has been changed (view state only), non-null once running.
     */
    void stackFrameFocusChanged(StackFrame oldStackFrame, MaxThread threadForStackFrame, StackFrame stackFrame);

    /**
     * Notifies that the global {@link Address} focus has been changed (view state only).
     */
    void addressFocusChanged(Address oldAddress, Address address);

    /**
     * Notifies that the global {@link MemoryRegion} focus has been changed (view state only).
     */
    void memoryRegionFocusChanged(MemoryRegion oldMemoryRegion, MemoryRegion memoryRegion);

    /**
     * Notifies that the breakpoint focus has been changed (view state only), possibly to null.
     */
    void breakpointFocusSet(MaxBreakpoint oldBreakpoint, MaxBreakpoint breakpoint);

    /**
     * Notifies that the watchpoint focus has been changed (view state only), possibly to null.
     */
    void watchpointFocusSet(MaxWatchpoint oldWatchpoint, MaxWatchpoint watchpoint);

    /**
     * Notifies that the heap object focus has changed (view state only), possibly to null.
     */
    void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject);

}

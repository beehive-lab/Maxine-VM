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
/*VCSID=ae827321-399a-4a54-a777-ea0b80461790*/
package com.sun.max.ins;

import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
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
    void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative);

    /**
     * Notifies that the global thread focus has been set (view state only), non-null once running.
     */
    void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread);

    /**
     * Notifies that the global stack frame focus has been changed (view state only), non-null once running.
     */
    void stackFrameFocusChanged(StackFrame oldStackFrame, TeleNativeThread threadForStackFrame, StackFrame stackFrame);

    /**
     * Notifies that the breakpoint focus has been changed (view state only), possibly to null.
     */
    void breakpointFocusSet(TeleBreakpoint oldTeleBreakpoint, TeleBreakpoint teleBreakpoint);

    /**
     * Notifies that the heap object focus has changed (view state only), possibly to null.
     */
    void heapObjectFocusChanged(TeleObject oldTeleObject, TeleObject teleObject);

}

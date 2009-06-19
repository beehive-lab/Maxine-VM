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

import com.sun.max.tele.*;


/**
 * Notification service for changes to state in the VM.
 *
 *
 * @author Michael Van De Vanter
 */
public interface InspectionListener {

    /**
     * Notifies that  VM state has potentially changed and should be revisited.
     *
     * @param force suspend caching behavior; reload state unconditionally.
     */
    void vmStateChanged(boolean force);

    /**
     * Notifies that the state associated with a particular thread  in the VM has changed.
     */
    void threadStateChanged(MaxThread thread);

    /**
     * Notifies that the set and/or status (enabled/disabled) of breakpoints in the VM has changed.
     */
    void breakpointStateChanged();

    /**
     * Notifies that the set of watchpoints in the VM has changed.
     */
    void watchpointSetChanged();

    /**
     * Notifies that an important aspect of view style/parameters/configuration have changed,
     * and that views should be reconstructed if needed (view state change only).
     */
    void viewConfigurationChanged();

    /**
     * Notifies that the running process associated with a VM has
     * stopped running.
     */
    void vmProcessTerminated();
}

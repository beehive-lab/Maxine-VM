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
import com.sun.max.tele.debug.*;


/**
 * Notification service for changes to state in the {@link TeleVM}.
 *
 * Many of the notifications include the current "epoch" of the
 * underlying process ({@see TeleProcess#epoch()}), which can
 * be used by listeners to decide whether to update caches or
 * not.
 *
 * @author Michael Van De Vanter
 */
public interface InspectionListener {

    /**
     * Notifies that  {@link TeleVM} state has potentially changed and should be revisited.
     *
     * @param epoch current epoch of the VM process {@see TeleProcess#epoch()}.
     * @param force suspend caching behavior; reload state unconditionally.
     */
    void vmStateChanged(long epoch, boolean force);

    /**
     * Notifies that the set of threads in the {@link TeleVM} has changed; listeners can assume
     * that the set hasn't changed unless this notification is received.
     *
     * @param epoch current epoch of the VM process {@see TeleProcess#epoch()}.
     */
    void threadSetChanged(long epoch);

    /**
     * Notifies that the state associated with a particular thread  in the {@link TeleVM} has changed.
     */
    void threadStateChanged(TeleNativeThread teleNativeThread);

    /**
     * Notifies that the set of breakpoints in the {@link TeleVM} has changed.
     * @param epoch  current epoch of the VM process {@see TeleProcess#epoch()}.
     */
    void breakpointSetChanged(long epoch);

    /**
     * Notifies that an important aspect of view style/parameters/configuration have changed,
     * and that views should be reconstructed if needed (view state change only).
     *
     * @param epoch current epoch of the VM process {@see TeleProcess#epoch()}.
     */
    void viewConfigurationChanged(long epoch);

    /**
     * Notifies that a running {@link TeleProcess} associated with a {@link @TeleVm} has
     * stopped running.
     */
    void vmProcessTerminated();
}

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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.annotate.*;
import com.sun.max.vm.monitor.modal.sync.nat.*;

/**
 * Abstract interface to a mutex as used by JavaMonitors.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */

public abstract class Mutex {

    /**
     *  Perform any setup necessary on this mutex.
     */
    public abstract Mutex init();

    /**
     * Perform any cleanup necessary on this mutex.
     */
    public abstract void cleanup();

     /**
      * Causes the current thread to perform a lock on the mutex.
      *
      * This is not intended for recursive locking, even though the mutex implementation
      * may support it.
      *
      * @return true if an error occurred in native code; false otherwise
      */
    public abstract boolean lock();

     /**
      * Causes the current thread to perform an unlock on the mutex.
      *
      * The current thread must own the given mutex when calling this method, otherwise the
      * results are undefined.
      *
      * @return true if an error occurred in native code; false otherwise
      * @see NativeMutex#lock()
      */
    public abstract boolean unlock();

    /**
     * Return an id suitable for logging purposes.
     */
    public abstract long logId();

}

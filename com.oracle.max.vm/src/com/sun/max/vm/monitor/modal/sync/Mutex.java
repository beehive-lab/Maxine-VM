/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.sync;

import com.sun.max.vm.monitor.modal.sync.nat.*;

/**
 * Abstract interface to a mutex as used by JavaMonitors.
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

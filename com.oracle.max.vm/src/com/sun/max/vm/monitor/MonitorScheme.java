/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.thread.*;

/**
 * Abstracts the way in which monitors are implemented. This covers both the translation
 * of the monitorenter and monitorexit bytecodes and the implementation of the wait and notify methods.
 */
public interface MonitorScheme extends VMScheme {

    /**
     * Translate a monitorenter bytecode.
     * @param object the object being acquired
     */
    void monitorEnter(Object object);

    /**
     * Translate a monitorexit bytecode.
     * @param object the object being released
     */
    void monitorExit(Object object);

    // The following methods are called at run time.

    /**
     * The implementation of Object.wait().
     */
    void monitorWait(Object object, long timeout) throws InterruptedException;

    /**
     * The implementation of Object.notify().
     */
    void monitorNotify(Object object);

    /**
     * The implementation of Object.notifyAll().
     */
    void monitorNotifyAll(Object object);

    int makeHashCode(Object object);

    /**
     * Prototyping support.
     */
    Word createMisc(Object object);

    /**
     * GC support for MonitorSchemes which hold native pointers to Java objects.
     */
    void scanReferences(PointerIndexVisitor pointerIndexVisitor);

    /**
     *  Notification that we are at a global safe-point (i.e. the object graph is well-formed and non-mutating), pre-collection.
     */
    void beforeGarbageCollection();

    /**
     *  Notification that we are at a global safe-point (i.e. the object graph is well-formed and non-mutating), post-collection.
     */
    void afterGarbageCollection();

    boolean threadHoldsMonitor(Object object, VmThread thread);
}

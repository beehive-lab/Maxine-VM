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
/*VCSID=eaf2cf32-db9e-4b49-907c-aace6bc6673d*/
package com.sun.max.vm.monitor;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.thread.*;

/**
 * Abstracts the way in which monitors are implemented. This covers both the translation
 * of the monitorenter and monitorexit bytecodes and the implementation of the wait and notify methods.
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
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
     *  GC support for MonitorSchemes which hold native pointers to java objects.
     */
    void scanReferences(PointerIndexVisitor pointerIndexVisitor, RuntimeMemoryRegion from, RuntimeMemoryRegion to);

    /**
     *  Notification that we are at a global safe-point, pre-collection.
     */
    @INLINE(override = true)
    void beforeGarbageCollection();

    /**
     *  Notification that we are at a global safe-point, post-collection.
     */
    void afterGarbageCollection();

    boolean threadHoldsMonitor(Object object, VmThread thread);
}

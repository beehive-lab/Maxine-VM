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
/*VCSID=6c2041e6-29a7-475b-a9d2-51fc612f3b50*/
package com.sun.max.vm.monitor.modal.modehandlers.observer;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.debug.Debug.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.ObserverModeHandler.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 *
 * @author Simon Wilkinson
 */
public class GCTracingObserver implements MonitorObserver {

    @Override
    public void notify(Event event, Object object) {
        // Test for GC thread by id, as we might be in the middle of moving VmThread objects.
        if (VmThreadLocal.ID.getConstantWord().asAddress().toInt() == 1) {
            // We don't want any allocations in here.
            // So get the source file name of the object's class via its hub.
            // (It looks like all other class-name enquiries are going to allocate a new String).
            final DebugPrintStream out = Debug.out;
            final boolean lockDisabledSafepoints = Debug.lock();
            out.print(event.name());
            out.print(" on instance of class defined in: ");
            final Hub hub = ObjectAccess.readHub(object);
            if (hub == null) {
                out.println("Null Hub");
                return;
            }
            final ClassActor actor = hub.classActor();
            if (actor == null) {
                out.println("Null ClassActor");
                return;
            }
            final String sourceFileName = actor.sourceFileName();
            if (sourceFileName == null) {
                out.println("Null source file name");
                return;
            }
            out.println(sourceFileName);
            Debug.unlock(lockDisabledSafepoints);
        }
    }
}

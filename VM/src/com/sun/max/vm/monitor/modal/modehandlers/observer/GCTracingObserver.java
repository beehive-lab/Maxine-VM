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
package com.sun.max.vm.monitor.modal.modehandlers.observer;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
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
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print(event.name());
            Log.print(" on instance of class defined in: ");
            final Hub hub = ObjectAccess.readHub(object);
            if (hub == null) {
                Log.println("Null Hub");
                return;
            }
            final ClassActor actor = hub.classActor();
            if (actor == null) {
                Log.println("Null ClassActor");
                return;
            }
            final String sourceFileName = actor.sourceFileName();
            if (sourceFileName == null) {
                Log.println("Null source file name");
                return;
            }
            Log.println(sourceFileName);
            Log.unlock(lockDisabledSafepoints);
        }
    }
}

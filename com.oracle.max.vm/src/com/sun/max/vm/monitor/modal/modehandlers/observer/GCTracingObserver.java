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
package com.sun.max.vm.monitor.modal.modehandlers.observer;

import static com.sun.max.vm.thread.VmThread.*;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.monitor.modal.modehandlers.observer.ObserverModeHandler.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.thread.*;

/**
 */
public class GCTracingObserver implements MonitorObserver {

    public void notify(Event event, Object object) {
        // Test for GC thread by id, as we might be in the middle of moving VmThread objects.
        if (VmThreadLocal.ID.load(currentTLA()).toInt() == 1) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print(event.name());
            Log.print(" on instance of class defined in: ");
            final Hub hub = ObjectAccess.readHub(object);
            if (hub == null) {
                Log.println("Null Hub");
                return;
            }
            final ClassActor actor = hub.classActor;
            if (actor == null) {
                Log.println("Null ClassActor");
                return;
            }
            final String sourceFileName = actor.sourceFileName;
            if (sourceFileName == null) {
                Log.println("Null source file name");
                return;
            }
            Log.println(sourceFileName);
            Log.unlock(lockDisabledSafepoints);
        }
    }
}

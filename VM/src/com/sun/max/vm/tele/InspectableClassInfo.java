/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.tele;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;

/**
 * Makes critical state information about dynamically loaded classes
 * remotely inspectable.
 * Active only when VM is being inspected.
 *
 * CAUTION:  When active, this implementation hold references to
 * all dynamically loaded {@link ClassActor}s, and thus prevents class unloading.
 */
public final class InspectableClassInfo {

    private InspectableClassInfo() {
    }

    @INSPECTED
    private static ClassActor[] classActors;

    @INSPECTED
    private static int classActorCount = 0;

    /**
     * Adds to the inspectable record of dynamically loaded classes.
     */
    public static void notifyClassLoaded(ClassActor classActor) {
        if (Inspectable.isVmInspected()) {
            synchronized (InspectableClassInfo.class) {
                if (classActors == null) {
                    classActors = new ClassActor[100];
                }
                if (classActorCount == classActors.length) {
                    classActors = Arrays.copyOf(classActors, classActorCount * 2);
                }
                // The classActor needs to be set up before we increment classActorCount
                // otherwise we have a race condition where the Inspector might see
                // a null classActor.
                classActors[classActorCount] = classActor;
                classActorCount++;
            }
        }
    }

}

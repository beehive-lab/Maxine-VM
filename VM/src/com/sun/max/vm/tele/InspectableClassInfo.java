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
package com.sun.max.vm.tele;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;


/**
 * Makes critical state information about dynamically loaded classes
 * remotely inspectable.
 * Active only when VM is being inspected.
 *
 * CAUTION:  When active, this implementation hold references to
 * all dynamically loaded {@link ClassActor}s, and thus prevents class unloading.
 *
 * @author Michael Van De Vanter
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
    public static void registerClassLoaded(ClassActor classActor) {
        if (MaxineMessenger.isVmInspected()) {
            if (classActors == null) {
                classActors = new ClassActor[100];
            }
            if (classActorCount == classActors.length) {
                classActors = Arrays.extend(classActors, classActorCount * 2);
            }
            // The classActor needs to be set up before we increment classActorCount
            // otherwise we have a race condition where the Inspector might see
            // a null classActor.
            classActors[classActorCount] = classActor;
            classActorCount++;
        }
    }

}

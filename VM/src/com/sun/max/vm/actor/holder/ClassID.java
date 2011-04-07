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
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Management of unique integer identifiers for {@link ClassActor}s.
 *
 * Every class in the system is assigned a globally unique identifier. This
 * identifier is used in the implementation of interface dispatch, type
 * tests and also serves as the opaque {@code jclass} handle to a
 * class in JNI code.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class ClassID {

    /**
     * Value to be used as invalid class id.
     */
    public static int NULL_CLASS_ID = -1;

    private ClassID() {
    }

    static final int MINIMAL_CLASSES_POPULATIONS = 4000;

    private static VariableLengthArray<ClassActor> idToClassActor = new VariableLengthArray<ClassActor>(MINIMAL_CLASSES_POPULATIONS);

    /**
     * BitSet keeping track of the assigned class identifiers. A bit set to 1 doesn't necessarily means a non-null entry in {@link ClassID#idToClassActor}
     * because class identifiers are created eagerly for array classes, whereas array class actors are created lazily.
     * Thus it is possible to encounter a null entry for a used class identifiers.
     */
    private static BitSet usedIDs = new BitSet();

    /**
     * Inspector support.
     */
    @HOSTED_ONLY
    public static interface Mapping {
        ClassActor idToClassActor(int id);
    }

    @HOSTED_ONLY
    private static Mapping mapping;

    @HOSTED_ONLY
    public static void setMapping(Mapping map) {
        mapping = map;
    }

    public static synchronized ClassActor toClassActor(int id) {
        try {
            if (MaxineVM.isHosted()) {
                if (mapping != null) {
                    final ClassActor classActor = mapping.idToClassActor(id);
                    if (classActor != null) {
                        return classActor;
                    }
                }
            }
            return idToClassActor.get(id);
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            return null;
        }
    }

    static synchronized int create() {
        final int id = usedIDs.nextClearBit(0);
        idToClassActor.set(id, null);
        usedIDs.set(id);
        return id;
    }

    static synchronized void register(ClassActor classActor) {
        int id = classActor.id;
        FatalError.check(usedIDs.get(id), "Class ID must be allocated");
        idToClassActor.set(id, classActor);
    }

    static synchronized void clear(int id) {
        idToClassActor.set(id, null);
        usedIDs.clear(id);
    }

    public static synchronized int largestClassId() {
        return idToClassActor.length();
    }


    @HOSTED_ONLY
    private static final BitSet createdArrayClassIDs = new BitSet();

    @HOSTED_ONLY
    public static boolean traceArrayClassIDs = false;

    @HOSTED_ONLY
    public static void recordArrayClassID(ClassActor elementClass, int dimension, int id) {
        if (traceArrayClassIDs) {
            createdArrayClassIDs.set(id);
            StringBuffer sb = new StringBuffer(" ");
            for (int d = 0; d <= dimension; d++) {
                sb.append('[');
            }
            sb.append(elementClass.name());
            sb.append(" => ");
            sb.append(id);
            Trace.line(1, sb);
        }
    }

    @HOSTED_ONLY
    public static void validateUsedClassIds() {
        if (!traceArrayClassIDs) {
            return;
        }
        int totalUsedNotCreated = 0;
        int id = 0;
        id = createdArrayClassIDs.nextSetBit(0);
        while (id >= 0) {
            ClassActor classActor = idToClassActor.get(id);
            if (classActor == null) {
                System.out.print("Class ID " + id + " created for array isn't assigned");
                if (usedIDs.get(id)) {
                    System.out.print(" but recorded used");
                    totalUsedNotCreated++;
                }
                System.out.println();
            } else if (!(classActor instanceof ArrayClassActor)) {
                System.out.println("Class ID " + id + " created for array isn't assigned to class array but to " + classActor.name());
            }
            id = createdArrayClassIDs.nextSetBit(id + 1);
        }

        id = 0;
        while (id >= 0) {
            ClassActor classActor = idToClassActor.get(id);
            if (classActor != null && classActor.arrayClassIDs != null) {
                final int [] arrayClassIDs = classActor.arrayClassIDs;
                for (int i = 0; i < arrayClassIDs.length; i++) {

                }
            }
            id = usedIDs.nextSetBit(id);
        }
    }
}

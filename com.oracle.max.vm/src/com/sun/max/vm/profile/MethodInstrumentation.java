/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.profile;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.object.ArrayAccess;
import com.sun.max.vm.object.ObjectAccess;

/**
 * This class contains several utility methods for dealing with method instrumentation.
 */
public class MethodInstrumentation {

    public static int initialEntryBackedgeCount = 5000;
    public static final int DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES = 3;

    /**
     * Methods whose invocation count (entry count) is within 90 % of the recompilation threshold
     * (see {@link #initialEntryBackedgeCount}), are protected from {@linkplain CodeEviction code eviction}.
     */
    public static final double PROTECTION_PERCENTAGE = 0.9;

    public static int protectionThreshold = (int) (1 - PROTECTION_PERCENTAGE) * initialEntryBackedgeCount;

    private static boolean enabled;

    public static void enable(int initialEntryCount) {
        enabled = true;
        MethodInstrumentation.initialEntryBackedgeCount = initialEntryCount;
        MethodInstrumentation.protectionThreshold = (int) (1 - PROTECTION_PERCENTAGE) * initialEntryCount;
    }

    public static MethodProfile.Builder createMethodProfile(ClassMethodActor classMethodActor) {
        if (enabled) {
            return new MethodProfile.Builder();
        }
        return null;
    }

    @NEVER_INLINE
    public static void recordType(MethodProfile mpo, Object object, int mpoIndex, int entries) {
        if (object != null) {
            Hub hub = ObjectAccess.readHub(object);
            findAndIncrement(mpo, mpoIndex, entries, hub.classActor.id, MethodProfile.UNDEFINED_TYPE_ID);
        } else {
            incrementProfileCounterAtIndex(mpo, mpoIndex + entries * 2);
        }
    }

    @NEVER_INLINE
    public static void recordReceiver(MethodProfile mpo, int methodId, int mpoIndex, int entries) {
        findAndIncrement(mpo, mpoIndex, entries, methodId, MethodProfile.UNDEFINED_METHOD_ID);
    }

    @INLINE
    public static void recordEntrypoint(MethodProfile mpo, Object receiver) {
        if (--mpo.entryBackedgeCount <= 0) {
            CompilationBroker.instrumentationCounterOverflow(mpo, receiver);
        }
    }

    @INLINE
    public static void recordBackwardBranch(MethodProfile mpo) {
        mpo.entryBackedgeCount--;
    }

    @INLINE
    public static void recordTakenBranch(MethodProfile mpo, int mpoIndex) {
        incrementProfileCounterAtIndex(mpo, mpoIndex);
    }

    @INLINE
    public static void recordNonTakenBranch(MethodProfile mpo, int mpoIndex) {
        incrementProfileCounterAtIndex(mpo, mpoIndex);
    }

    @INLINE
    private static void incrementProfileCounterAtIndex(MethodProfile mpo, int index) {
        int[] data = mpo.rawData();
        int counter = ArrayAccess.getInt(data, index);

        if (counter < Integer.MAX_VALUE) {
            ArrayAccess.setInt(data, index, counter + 1);
        }
    }

    @INLINE
    private static void findAndIncrement(MethodProfile mpo, int index, int entries, int id, int emptyDataId) {
        int[] data = mpo.rawData();
        int max = index + entries * 2;
        for (int i = index; i < max; i += 2) {
            if (ArrayAccess.getInt(data, i) == id) {
                // this entry matches
                incrementProfileCounterAtIndex(mpo, i + 1);
                return;
            } else if (ArrayAccess.getInt(data, i) == emptyDataId) {
                // this entry is empty
                ArrayAccess.setInt(data, i, id);
                ArrayAccess.setInt(data, i + 1, 1);
                return;
            }
        }
        // failed to find matching entry, increment default
        incrementProfileCounterAtIndex(mpo, index + entries * 2 + 1);
    }

    public static Hub computeMostFrequentHub(MethodProfile mpo, int bci, int threshold, float ratio) {
        if (mpo != null) {
            Integer[] typeProfile = mpo.getTypeProfile(bci);
            if (typeProfile != null) {
                int total = 0;
                for (int i = 0; i < typeProfile.length; i++) {
                    // count up the total of all non anonymous entries
                    Integer typeId = typeProfile[i];
                    Integer count = typeProfile[i + 1];
                    if (typeId != MethodProfile.UNDEFINED_TYPE_ID) {
                        total += count;
                    }
                }
                if (total >= threshold) {
                    // if there are enough recorded entries
                    int thresh = (int) (ratio * total);
                    int mostFrequentTypeId = MethodProfile.UNDEFINED_TYPE_ID;
                    int mostFrequentTypeCount = thresh;
                    for (int i = 0; i < typeProfile.length; i++) {
                        Integer typeId = typeProfile[i];
                        Integer count = typeProfile[i + 1];
                        if (typeId != MethodProfile.UNDEFINED_TYPE_ID && count >= mostFrequentTypeCount) {
                            mostFrequentTypeCount = count;
                            mostFrequentTypeId = typeId;
                        }
                    }
                    if (mostFrequentTypeId != MethodProfile.UNDEFINED_TYPE_ID) {
                        return typeIdToHub(mostFrequentTypeId);
                    }
                }
            }
        }
        return null;
    }

    private static Hub typeIdToHub(Integer typeId) {
        if (typeId != MethodProfile.UNDEFINED_TYPE_ID) {
            ClassActor classActor = ClassIDManager.toClassActor(typeId);
            return classActor.dynamicHub();
        }
        return null;
    }

}

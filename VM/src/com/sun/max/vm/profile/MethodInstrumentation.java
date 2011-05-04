/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;

/**
 * This class contains several utility methods for dealing with method instrumentation.
 *
 * @author Ben L. Titzer
 */
public class MethodInstrumentation {

    public static int initialEntryCount = 5000;
    public static final int DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES = 3;

    private static boolean enabled;

    public static void enable(int initialEntryCount) {
        enabled = true;
        MethodInstrumentation.initialEntryCount = initialEntryCount;
    }

    public static MethodProfile.Builder createMethodProfile(ClassMethodActor classMethodActor) {
        if (enabled) {
            MethodProfile.Builder builder = new MethodProfile.Builder();
            MethodProfile mpo = builder.methodProfileObject();
            mpo.method = classMethodActor;
            return builder;
        }
        return null;
    }

    @NEVER_INLINE
    public static void recordType(MethodProfile mpo, Hub hub, int mpoIndex, int entries) {
        findAndIncrement(mpo, mpoIndex, entries, hub.classActor.id);
    }

    @NEVER_INLINE
    public static void recordReceiver(MethodProfile mpo, Word entrypoint, int mpoIndex, int entries) {
        findAndIncrement(mpo, mpoIndex, entries, entrypoint.asOffset().toInt());
    }

    @NEVER_INLINE
    public static void recordLocation(MethodProfile mpo, int mpoIndex) {
        int[] data = mpo.rawData();
        if (false) {
            // use checked array access
            if (--data[mpoIndex] == 0) {
                triggerRecompilation(mpo, null);
            }
        } else {
            // use unchecked array access
            Pointer ptr = ArrayAccess.elementPointer(data, mpoIndex);
            int nval = ptr.getInt() - 1;
            if (nval == 0) {
                triggerRecompilation(mpo, null);
            }
            ptr.setInt(nval);
        }
    }

    @INLINE
    public static void recordEntrypoint(MethodProfile mpo, Object receiver) {
        if (--mpo.entryCount == 0) {
            triggerRecompilation(mpo, receiver);
        }
    }

    private static void triggerRecompilation(MethodProfile mpo, Object receiver) {
        if (Heap.isAllocationDisabledForCurrentThread()) {
            Log.print("Stopped recompilation of ");
            Log.printMethod(mpo.method, false);
            Log.println(" because allocation is currently disabled");
            return;
        }

        synchronized (mpo) {
            if (!mpo.triggered) {
                // location count overflowed; call into instrumentation system
                CompilationScheme.Static.instrumentationCounterOverflow(mpo, receiver);
                mpo.triggered = true;
            }
        }
    }

    @INLINE
    private static void findAndIncrement(MethodProfile mpo, int index, int entries, int id) {
        int[] data = mpo.rawData();
        int max = index + entries * 2;
        for (int i = index; i < max; i += 2) {
            if (data[i] == id) {
                // this entry matches
                data[i + 1]++;
                return;
            } else if (data[i] == 0) {
                // this entry is empty
                data[i] = id;
                data[i + 1] = 1;
                return;
            }
        }
        // failed to find matching entry, increment default
        data[index + entries * 2 + 1]++;
    }

    public static Hub computeMostFrequentHub(MethodProfile mpo, int bci, int threshold, float ratio) {
        if (mpo != null) {
            Integer[] hubProfile = mpo.getTypeProfile(bci);
            if (hubProfile != null) {
                int total = 0;
                for (int i = 0; i < hubProfile.length; i++) {
                    // count up the total of all entries
                    Integer hubId = hubProfile[i];
                    Integer count = hubProfile[i + 1];
                    if (hubId != null && count != null) {
                        total += count;
                    }
                }
                if (total >= threshold) {
                    // if there are enough recorded entries
                    int thresh = (int) (ratio * total);
                    for (int i = 0; i < hubProfile.length; i++) {
                        Integer hubId = hubProfile[i];
                        Integer count = hubProfile[i + 1];
                        if (hubId != null && count != null && count >= thresh) {
                            return idToHub(hubId);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Hub idToHub(Integer hubId) {
        if (hubId != null && hubId > 0) {
            // TODO: convert hub id to hub
        }
        return null;
    }

}

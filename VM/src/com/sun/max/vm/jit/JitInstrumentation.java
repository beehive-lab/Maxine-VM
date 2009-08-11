/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.jit;

import com.sun.max.vm.actor.holder.Hub;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.profile.MethodProfile;
import com.sun.max.vm.compiler.CompilationScheme;
import com.sun.max.unsafe.Word;
import com.sun.max.annotate.NEVER_INLINE;
import com.sun.max.annotate.INLINE;

/**
 * This class contains several utility methods for dealing with method instrumentation
 * in JIT code.
 *
 * @author Ben L. Titzer
 */
public class JitInstrumentation {

    public static final int DEFAULT_ENTRY_INITIAL_COUNT = 5000;
    public static final int DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES = 3;

    public static MethodProfile.Builder createMethodProfile(ClassMethodActor classMethodActor) {
        if (false) {
            MethodProfile.Builder builder = new MethodProfile.Builder();
            builder.methodProfileObject().method = classMethodActor;
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
        if (--data[mpoIndex] == 0) {
            if (!mpo.triggered) {
                synchronized (mpo) {
                    if (!mpo.triggered) {
                        // location count overflowed; call into instrumentation system
                        CompilationScheme.Static.instrumentationCounterOverflow(mpo, mpoIndex);
                        mpo.triggered = true;
                    }
                }
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

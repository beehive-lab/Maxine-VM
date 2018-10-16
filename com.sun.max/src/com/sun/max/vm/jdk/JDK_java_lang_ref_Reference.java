/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.lang.ref.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.type.*;

import sun.misc.*;

/**
 * Substitutions for java.lang.ref.Reference.
 */
@METHOD_SUBSTITUTIONS(Reference.class)
public final class JDK_java_lang_ref_Reference {

    @ALIAS(declaringClass = java.lang.ref.Reference.class)
    static          java.lang.ref.Reference      pending;

    @ALIAS(declaringClass = java.lang.ref.Reference.class, descriptor = "Ljava/lang/ref/Reference$Lock;")
    static          Object                       lock;

    @ALIAS(declaringClass = java.lang.ref.Reference.class)
    public          java.lang.ref.Reference      next;

    @ALIAS(declaringClass = java.lang.ref.Reference.class)
    public volatile java.lang.ref.ReferenceQueue queue;

    @INTRINSIC(UNSAFE_CAST)
    public static native JDK_java_lang_ref_Reference asJLRRAlias(Object o);

    @INTRINSIC(UNSAFE_CAST)
    public static native Cleaner asCleaner(Object o);

    /**
     * Substitute of java.lang.ref.Reference.tryHandlePending.
     * <p>
     * It is essentially a copy of the substitutee that instead of traversing the pending list through the discovered
     * field (as in JDK 8), it traverses it through the next field (as prior to JDK 8).
     *
     * @param waitForNotify
     * @return
     */
    @SUBSTITUTE(optional = true) // Not available in JDK 7
    static boolean tryHandlePending(boolean waitForNotify) {
        Reference                   ref;
        JDK_java_lang_ref_Reference refAlias;
        Cleaner                     c;
        try {
            synchronized (lock) {
                if (pending != null) {
                    ref = pending;
                    refAlias = asJLRRAlias(ref);
                    Reference rn = refAlias.next;
                    pending = (rn == ref) ? null : rn;
                    refAlias.next = ref;
                    // 'instanceof' might throw OutOfMemoryError sometimes
                    // so do this before un-linking 'r' from the 'pending' chain...
                    c = ClassRegistry.CLEANER.isInstance(ref) ? asCleaner(ref) : null;
                    // unlink 'r' from 'pending' chain
                } else {
                    // The waiting on the lock may cause an OutOfMemoryError
                    // because it may try to allocate exception objects.
                    if (waitForNotify) {
                        lock.wait();
                    }
                    // retry if waited
                    return waitForNotify;
                }
            }
        } catch (OutOfMemoryError x) {
            // Give other threads CPU time so they hopefully drop some live references
            // and GC reclaims some space.
            // Also prevent CPU intensive spinning in case 'r instanceof Cleaner' above
            // persistently throws OOME for some time...
            Thread.yield();
            // retry
            return true;
        } catch (InterruptedException x) {
            // retry
            return true;
        }

        // Fast path for cleaners
        if (c != null) {
            c.clean();
            return true;
        }

        JDK_java_lang_ref_ReferenceQueue q = JDK_java_lang_ref_ReferenceQueue.asThis(refAlias.queue);
        if (refAlias.queue != JDK_java_lang_ref_ReferenceQueue.NULL) {
            q.enqueue(ref);
        }
        return true;
    }


}

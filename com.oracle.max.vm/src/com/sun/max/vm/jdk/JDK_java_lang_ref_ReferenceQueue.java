/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.heap.SpecialReferenceManager.*;

import java.lang.ref.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.SpecialReferenceManager.JLRRAlias;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Substitutions for java.lang.ref.ReferenceQueue.
 */
@METHOD_SUBSTITUTIONS(ReferenceQueue.class)
public final class JDK_java_lang_ref_ReferenceQueue {

    private JDK_java_lang_ref_ReferenceQueue() {
    }

    @ALIAS(declaringClass = ReferenceQueue.class)
    private volatile java.lang.ref.Reference head;

    @ALIAS(declaringClass = ReferenceQueue.class)
    public static ReferenceQueue NULL;

    @ALIAS(declaringClass = ReferenceQueue.class)
    public static ReferenceQueue ENQUEUED;

    @ALIAS(declaringClass = ReferenceQueue.class)
    private long queueLength;

    @ALIAS(declaringClass = ReferenceQueue.class, descriptor = "Ljava/lang/ref/ReferenceQueue$Lock;")
    private Object lock;

    /**
     * Note: Must be kept in sync with the original JDK source.
     */
    @SUBSTITUTE
    boolean enqueue(java.lang.ref.Reference r) {
        synchronized (r) {
            JLRRAlias rAlias = asJLRRAlias(r);
            if (rAlias.queue == ENQUEUED) {
                return false;
            }
            synchronized (lock) {
                rAlias.queue = ENQUEUED;
                rAlias.next = (head == null) ? r : head;
                head = r;
                queueLength++;
                if (ClassRegistry.JLR_FINAL_REFERENCE.isInstance(r)) {
                    sun.misc.VM.addFinalRefCount(1);
                }
                lock.notifyAll();
                if (SpecialReferenceManager.specialReferenceLogger.enabled()) {
                    SpecialReferenceManager.specialReferenceLogger.logEnqueue(ObjectAccess.readClassActor(r), Reference.fromJava(r).toOrigin(), Reference.fromJava(this).toOrigin());
                }
                return true;
            }
        }
    }

    /**
     * Note: Must be kept in sync with the original JDK source.
     */
    @SUBSTITUTE
    private java.lang.ref.Reference reallyPoll() {
        FatalError.check(Thread.holdsLock(lock), "ReferenceQueue.lock should be held");
        if (head != null) {
            java.lang.ref.Reference r = head;
            JLRRAlias rAlias = asJLRRAlias(r);

            head = (rAlias.next == r) ? null : asJLRR(rAlias.next);

            if (rAlias.queue != ENQUEUED) {
                Log.print(ObjectAccess.readClassActor(r).name.string);
                Log.print(" at ");
                Log.print(Reference.fromJava(r).toOrigin());
                Log.print(" on queue should have queue field equal to ENQUEUED (");
                Log.print(Reference.fromJava(ENQUEUED).toOrigin());
                Log.print(") instead of ");
                Log.println(Reference.fromJava(rAlias.queue).toOrigin());
                FatalError.unexpected("wrong queue");
            }

            rAlias.queue = NULL;
            rAlias.next = r;
            queueLength--;
            if (ClassRegistry.JLR_FINAL_REFERENCE.isInstance(r)) {
                sun.misc.VM.addFinalRefCount(-1);
            }
            if (SpecialReferenceManager.specialReferenceLogger.enabled()) {
                SpecialReferenceManager.specialReferenceLogger.logRemove(
                                ObjectAccess.readClassActor(r),
                                Reference.fromJava(r).toOrigin(),
                                Reference.fromJava(this).toOrigin());
            }
            return r;
        }
        return null;
    }
}

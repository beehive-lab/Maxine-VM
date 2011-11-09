/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.jvmti.JVMTIUtil.ClassActorProxy;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * All the JVMTI functions that relate to the heap.
 */
public class JVMTIHeapFunctions {

    /**
     * * Must be consistent with {@code struct jvmtiHeapCallbacks} in jvmti.c.
     */
    static enum HeapCallbacks {
        HEAP_ITERATION(0),
        HEAP_REFERENCE(8),
        PRIMITIVE_FIELD(16),
        ARRAY_PRIMITIVE_VALUE(24),
        STRING_PRIMITIVE_VALUE(32);

        private final int offset;

        HeapCallbacks(int offset) {
            this.offset = offset;
        }

        Word getCallback(Pointer struct) {
            return struct.readWord(offset);
        }
    }

    /** A {@link VmOperation} that prevents any allocgtion while we walk the heap.
     * We don't care about the threads, just that they are blocked, so we
     * override the {@link VmOperation#doIt} method.
     */
    static class IterateThroughHeapVmOperation extends VmOperation {
        private final JVMTI.Env env;
        private final int heapFilter;
        private final Class klass;
        private final Pointer callbacks;
        private final CBCV cbcv;
        private final Word userData;

        class CBCV extends CallbackCellVisitor {
            @Override
            protected boolean callback(Object object) {
                Pointer tagPtr = Intrinsics.stackAllocate(Word.size());
                ClassActor classActor = ObjectAccess.readClassActor(object);
                ClassActorProxy proxyClassActor = ClassActorProxy.asClassActorProxy(classActor);

                /* To avoid the tricky case where we encounter an object whose Class mirror
                 * has not been set in the ClassActor yet, which would require allocation
                 * we check the field in classActor directly and observe that such
                 * an object cannot have been tagged, otherwise its class mirror would be set.
                 */
                Class objectClass = proxyClassActor.javaClass == null ? null : proxyClassActor.javaClass;

                if (klass != null && objectClass != klass)  {
                    return true;
                }
                if ((heapFilter & JVMTI_HEAP_FILTER_CLASS_TAGGED) != 0) {
                    if (objectClass != null && env.tags.isTagged(objectClass)) {
                        return true;
                    }
                }
                if ((heapFilter & JVMTI_HEAP_FILTER_CLASS_UNTAGGED) != 0) {
                    if (!(objectClass != null && env.tags.isTagged(objectClass))) {
                        return true;
                    }
                }
                if ((heapFilter & JVMTI_HEAP_FILTER_TAGGED) != 0) {
                    if (env.tags.isTagged(object)) {
                        return true;
                    }
                }
                if ((heapFilter & JVMTI_HEAP_FILTER_UNTAGGED) != 0) {
                    if (!env.tags.isTagged(object)) {
                        return true;
                    }
                }
                Reference objectRef = Reference.fromJava(object);
                Word heapIterationCallback = HeapCallbacks.HEAP_ITERATION.getCallback(callbacks);
                if (!heapIterationCallback.isZero()) {
                    long tag = env.tags.getTag(object);
                    tagPtr.setLong(tag);
                    int flags = invokeHeapIterationCallback(
                                    heapIterationCallback.asPointer(),
                                    objectClass == null ? 0 : env.tags.getTag(objectClass),
                                    Layout.size(objectRef).toInt(),
                                    tagPtr,
                                    Layout.isArray(objectRef) ? Layout.readArrayLength(objectRef) : -1,
                                    userData);
                    long newTag = tagPtr.getLong();
                    if (newTag != tag) {
                        env.tags.setTag(object, newTag);
                    }
                    if ((flags & JVMTI_VISIT_ABORT) != 0) {
                        return false;
                    }
                }
                return true;
            }
        }

        IterateThroughHeapVmOperation(JVMTI.Env env, int heapFilter, Class klass, Pointer callbacks, Word userData) {
            super("JVMTI_IterateThroughHeap", null, Mode.Safepoint, false);
            this.heapFilter = heapFilter;
            this.klass = klass;
            this.callbacks = callbacks;
            this.cbcv = new CBCV();
            this.env = env;
            this.userData = userData;
        }

        @Override
        protected void doIt() {
            // There should be no allocation in this path, so we enforce that as a debugging aid.
            try {
                Heap.disableAllocationForCurrentThread();
                vmConfig().heapScheme().walkHeap(cbcv);
            } finally {
                Heap.enableAllocationForCurrentThread();
            }
        }
    }

    static int iterateThroughHeap(JVMTI.Env jvmtiEnv, int heapFilter, Class klass, Pointer callbacks, Pointer userData) {
        IterateThroughHeapVmOperation op = new IterateThroughHeapVmOperation(jvmtiEnv, heapFilter, klass, callbacks, userData);
        op.submit();
        return JVMTI_ERROR_NONE;
    }

}

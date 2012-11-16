/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.ext.jvmti.JVMTICallbacks.*;
import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIUtil.*;

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
        private final CBCV cbcv;

        abstract class CBCV extends CallbackCellVisitor {
            protected final JVMTI.Env env;
            protected final int heapFilter;
            protected final Class klass;

            CBCV(JVMTI.Env env, int heapFilter, Class klass) {
                this.env = env;
                this.heapFilter = heapFilter;
                this.klass = klass;
            }

            @Override
            protected boolean callback(Object object) {
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
                int flags = doCallback(object, objectClass);
                if ((flags & JVMTI_VISIT_ABORT) != 0) {
                    return false;
                }
                return true;
            }

            protected abstract int doCallback(Object object, Class objectClass);
        }

        class CBCVNative extends CBCV {
            private final Pointer callbacks;
            private final Word userData;

            CBCVNative(JVMTI.Env env, int heapFilter, Class klass, Pointer callbacks, Word userData) {
                super(env, heapFilter, klass);
                this.callbacks = callbacks;
                this.userData = userData;

            }

            @Override
            protected int doCallback(Object object, Class objectClass) {
                Pointer tagPtr = Intrinsics.alloca(Word.size(), false);
                Reference objectRef = Reference.fromJava(object);
                Word heapIterationCallback = HeapCallbacks.HEAP_ITERATION.getCallback(callbacks);
                if (!heapIterationCallback.isZero()) {
                    long tag = env.tags.getLongTag(object);
                    tagPtr.setLong(tag);
                    int flags = invokeHeapIterationCallback(
                                    heapIterationCallback.asPointer(),
                                    objectClass == null ? 0 : env.tags.getLongTag(objectClass),
                                    Layout.size(objectRef).toInt(),
                                    tagPtr,
                                    Layout.isArray(objectRef) ? Layout.readArrayLength(objectRef) : -1,
                                    userData);
                    long newTag = tagPtr.getLong();
                    if (newTag != tag) {
                        env.tags.setTag(object, newTag);
                    }
                    return flags;
                }
                return 0;
            }

        }

        class CBCVJava extends CBCV {
            protected final JJVMTI.HeapCallbacks heapCallbacks;
            protected final Object userData;

            CBCVJava(JVMTI.Env env, int heapFilter, Class klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData) {
                super(env, heapFilter, klass);
                this.heapCallbacks = heapCallbacks;
                this.userData = userData;
            }

            @Override
            protected int doCallback(Object object, Class objectClass) {
                Reference objectRef = Reference.fromJava(object);
                return heapCallbacks.heapIteration(objectClass == null ? 0 : env.tags.getObjectTag(objectClass),
                                Layout.size(objectRef).toInt(), env.tags.getObjectTag(object), Layout.isArray(objectRef) ? Layout.readArrayLength(objectRef) : -1, objectClass);
            }
        }

        class CBCVJavaMax extends CBCVJava {
            CBCVJavaMax(JVMTI.Env env, int heapFilter, Class klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData) {
                super(env, heapFilter, klass, heapCallbacks, userData);
            }

            @Override
            protected int doCallback(Object object, Class objectClass) {
                return heapCallbacks.heapIterationMax(object, userData);
            }
        }


        IterateThroughHeapVmOperation(JVMTI.Env env, int heapFilter, Class klass, Pointer callbacks, Word userData) {
            super("JVMTI_IterateThroughHeap", null, Mode.Safepoint, false);
            this.cbcv = new CBCVNative(env, heapFilter, klass, callbacks, userData);
        }

        IterateThroughHeapVmOperation(JVMTI.Env env, int heapFilter, Class klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData) {
            super("JVMTI_IterateThroughHeap", null, Mode.Safepoint, false);
            this.cbcv = new CBCVJava(env, heapFilter, klass, heapCallbacks, userData);
        }

        IterateThroughHeapVmOperation(JVMTI.Env env, int heapFilter, Class klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData, boolean max) {
            super("JVMTI_IterateThroughHeapMax", null, Mode.Safepoint, false);
            this.cbcv = new CBCVJavaMax(env, heapFilter, klass, heapCallbacks, userData);
        }

        @Override
        protected void doIt() {
            // Ideally there should be no allocation in this path, at least in the "application" heap.
            // However, currently, Maxine has a unified heap and, at least for the Java JVMTI agents,
            // it turns out to be essentially impossible to guarantee no allocation owing to hidden
            // allocations in the VM itself. So we use the immortal heap for now.
            // TODO revisit this when Maxine addresses VM and application heap separation
            try {
                Heap.enableImmortalMemoryAllocation();
                vmConfig().heapScheme().walkHeap(cbcv);
            } finally {
                Heap.disableImmortalMemoryAllocation();
            }
        }
    }

    static int iterateThroughHeap(JVMTI.Env jvmtiEnv, int heapFilter, Class klass, Pointer callbacks, Pointer userData) {
        IterateThroughHeapVmOperation op = new IterateThroughHeapVmOperation(jvmtiEnv, heapFilter, klass, callbacks, userData);
        op.submit();
        return JVMTI_ERROR_NONE;
    }

    static void iterateThroughHeap(JVMTI.Env jvmtiEnv, int heapFilter, ClassActor klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData) {
        IterateThroughHeapVmOperation op = new IterateThroughHeapVmOperation(jvmtiEnv, heapFilter, klass == null ? null : klass.toJava(), heapCallbacks, userData);
        op.submit();
    }

    static void iterateThroughHeapMax(JVMTI.Env jvmtiEnv, int heapFilter, ClassActor klass, JJVMTI.HeapCallbacks heapCallbacks, Object userData) {
        IterateThroughHeapVmOperation op = new IterateThroughHeapVmOperation(jvmtiEnv, heapFilter, klass == null ? null : klass.toJava(), heapCallbacks, userData, true);
        op.submit();
    }

}

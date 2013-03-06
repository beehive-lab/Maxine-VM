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
package com.sun.max.vm.jni;

import java.lang.ref.*;

import com.oracle.graal.snippets.Snippet.Fold;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * JNI handles are allocated for one of the following reasons:
 *
 *  1. Passing an object from Java code to native code as a parameter of a native method
 *  2. Returning an object from a {@linkplain JniFunctions JNI function} to native code
 *
 * The first type of handle is implemented as the address of an object on the thread's stack.
 * The second type of handle is allocated from a pool of JNI handles. There is one pool of
 * JNI handles per thread that is used to allocate local JNI references, a
 * global pool per VM (or isolate?) for global references and
 * another global pool for weak global references.
 *
 * This class implements a pool of JNI handles.
 *
 * In the Maxine VM, we need to take into account that objects may be allocated
 * in a hardware object memory where one cannot take the address of an element or field within
 * an object (i.e. there can be no pointers into the middle of such objects).
 *
 * This leaves us with these choices for implementing JNI handle pools:
 *
 * 1. Store object references inside malloc'ed (i.e. non-object) data structures.
 *     Pros: - JNI handles can be treated uniformly as the address of an object reference
 *             which allows for the fastest and simplest implementation of handle dereferencing.
 *     Cons: - Garbage collection needs to be specialized to include the handles as roots. Also
 *           - Any malloc'ed data structures must be manually deallocated.
 *           - Manipulating non-object data structures requires peeking and poking as opposed
 *             to normal Java array accessing.
 *
 * 2. Store object references inside standard Java objects (i.e. arrays).
 *     Pros: - No need to use malloc or specialize the garbage collectors.
 *           - No special code for deallocating apart from clearing references.
 *           - Manipulating objects can be expressed in standard Java code (i.e. array
 *             indexing expressions).
 *     Cons: - JNI handles require different formats to distinguish between an index into a
 *             pool and a double indirected pointer (i.e. a wrapped native method parameter).
 *             The format for an indexed JNI handle must further distinguish between the current
 *             thread's pool of local references, the global reference pool and the global
 *             weak reference pool. The code for dereferencing a JNI handle must take into
 *             account all of these formats.
 *
 * 3. Other choices?
 *
 * Given the above trade offs between 1 and 2 (and the current non-existence of 3!), we
 * have decided to go with choice 2 until it proves too complex and/or inefficient.
 */
public final class JniHandles {

    public static final class Tag {
        public static final int STACK = 0;
        public static final int LOCAL = 1;
        public static final int GLOBAL = 2;
        public static final int WEAK_GLOBAL = 3;

        public static final int MASK = 3;
        public static final int BITS = 2;
    }

    static final class Frame {
        final int start;
        final Frame previous;
        public Frame(int start, Frame previous) {
            this.start = start;
            this.previous = previous;
        }
    }

    public static final int INITIAL_NUMBER_OF_HANDLES = 32;

    private static final JniHandles globalHandles = new JniHandles();
    private static final JniHandles weakGlobalHandles = new JniHandles();

    /**
     * The objects exposed to native code via handles.
     *
     * Note that this representation simplifies handle allocation and dereferencing
     * (in the context of a "JNI handle is an index" implementation)
     * at the cost of expanding an array (allocation plus copy). It will be replaced
     * with a better representation if this expansion cost proves to be too high.
     */
    private Object[] handles = new Object[INITIAL_NUMBER_OF_HANDLES];

    /**
     * The frames pushed by {@link JniFunctions#PushLocalFrame}.
     */
    private Frame frames;

    /**
     * Denotes the indexes of handles that have been {@linkplain #freeHandle(int) freed}.
     * No bit will be set for a handle at an index >= {@link #top}.
     */
    private final CiBitMap freedHandles = new CiBitMap(INITIAL_NUMBER_OF_HANDLES);

    /**
     * The index at which the next search for a free bit in {@link #freedHandles} starts.
     * This optimizes allocation where the expected behavior of a native method
     * that deletes local references is such that these references are deleted in the
     * reverse order in which they were created.
     */
    private int lastFreedIndex;

    /**
     * Number of handles allocated from this pool that are (potentially) still in use.
     * This value also denotes the index of next unused handle in {@link #handles}.
     * The name of this field also gives some indication of how handles can be allocated
     * and freed in a stack like fashion.
     *
     * Invariant: All elements in {@link #handles} at an index greater than or equal to {@link #top} are null.
     */
    private int top;

    /**
     * Return the "top" (i.e. current size) of this handle pool. This value can be given
     * as the parameter to the {@link #resetTop(int)} method to free handles in a stack
     * like fashion.
     */
    @INLINE
    public int top() {
        return top;
    }

    /**
     * Resets the "top" (i.e. current size) of this handle pool to a value
     * equal to or less than its current size.
     */
    @NO_SAFEPOINT_POLLS("cannot stop before pending exception in JNI stub has been processed")
    public void resetTop(int newTop) {
        if (newTop > this.top || newTop < 0) {
            FatalError.unexpected("Cannot reset JNI handle stack to higher stack height");
        }

        if (newTop != this.top) {
            for (int i = newTop; i != this.top; ++i) {
                handles[i] = null;
            }
            int freeLength = freedHandles.length();
            if (freeLength >= newTop) {
                for (int i = freedHandles.nextSetBit(newTop, freeLength); i != -1; i = freedHandles.nextSetBit(i, freeLength)) {
                    freedHandles.clear(i);
                }
            }

            lastFreedIndex = 0;
            this.top = newTop;
        }
    }

    /**
     * Gets the handle at a given index.
     */
    private Object get(int index) {
        return handles[index];
    }

    /**
     * Frees the JNI handle at a given index from this pool.
     *
     * @param index the index of the handle to free
     */
    private void freeHandle(int index) {
        handles[index] = null;
        freedHandles.grow(index + 1);
        freedHandles.set(index);
        lastFreedIndex = index;
    }

    private static Object[] expandHandles(Object[] handles, int newLength) {
        final int currentLength = handles.length;
        final Object[] newHandles = new Object[newLength];
        // Can't use System.arraycopy - it's a native method which may require allocating JNI handles!
        for (int i = 0; i != currentLength; ++i) {
            newHandles[i] = handles[i];
        }
        return newHandles;
    }

    private JniHandle allocateHandle(Object object, int tag) {
        assert object != null;

        // Try to get a handle from the logical end of the array
        if (top < handles.length) {
            assert handles[top] == null;
            handles[top] = object;
            return indexToJniHandle(top++, tag);
        }

        // Now look for a handle in the free set
        int index = freedHandles.nextSetBit(lastFreedIndex);
        if (index == -1 && lastFreedIndex != 0) {
            // Wrap around and search from the beginning of the array
            index = freedHandles.nextSetBit(0);
        }
        if (index != -1) {
            assert handles[index] == null;
            handles[index] = object;
            freedHandles.clear(index);
            return indexToJniHandle(index, tag);
        }

        // No space available, the handle array is expanded
        // to be double its current size.
        handles = expandHandles(handles, handles.length * 2);

        // Retry - guaranteed to succeed
        return allocateHandle(object, tag);
    }

    private static JniHandle indexToJniHandle(int index, int tag) {
        return Address.fromInt(index << Tag.BITS | tag).asJniHandle();
    }

    private static int jniHandleToIndex(JniHandle jniHandle) {
        return jniHandle.asOffset().toInt() >> Tag.BITS;
    }

    public static int tag(JniHandle jniHandle) {
        return jniHandle.asOffset().toInt() & Tag.MASK;
    }

    private void pushFrame(int capacity) {
        ensureCapacity(capacity);
        frames = new Frame(top, frames);
    }

    private JniHandle popFrame(JniHandle result) {
        final Object object = get(result);

        // This test means PopLocalFrame will work even if there was
        // not a corresponding call to PushLocalFrame
        if (frames != null) {
            resetTop(frames.start);
            frames = frames.previous;
        }
        return (object != null) ? allocateHandle(object, Tag.LOCAL) : result;
    }

    /**
     * Ensures that <i>at least</i> a given number of local references can be created in this pool of handles.
     */
    private void ensureCapacity(int capacity) {
        final int available = (handles.length - top) + freedHandles.cardinality();
        final int extraNeeded = capacity - available;
        if (extraNeeded > 0) {
            handles = expandHandles(handles, handles.length + extraNeeded);
        }
    }

    /**
     * Extracts the object from a given JNI handle. This will return null if {@code jniHandle.isZero() == true}.
     */
    public static Object get(JniHandle jniHandle) {
        final int tag = tag(jniHandle);
        if (tag == Tag.STACK) {
            if (jniHandle.isZero()) {
                return null;
            }
            return jniHandle.asPointer().getReference().toJava();
        }
        if (tag == Tag.LOCAL) {
            JniHandles jniHandles = VmThread.current().jniHandles();
            return jniHandles.get(jniHandleToIndex(jniHandle));
        }
        if (tag == Tag.GLOBAL) {
            return globalHandles.get(jniHandleToIndex(jniHandle));
        }
        assert tag == Tag.WEAK_GLOBAL;

        final WeakReference weakReference = (WeakReference) weakGlobalHandles.get(jniHandleToIndex(jniHandle));
        return weakReference == null ? null : weakReference.get();
    }

    public static Address getAddress(JniHandle jniHandle) {
        assert tag(jniHandle) == Tag.STACK;
        return jniHandle.asPointer().getWord().asAddress();
    }

    /**
     * Creates a thread-local JNI handle for a reference. The handle is valid only within the
     * dynamic context of the native method that creates it, and only within that one invocation
     * of the native method. All local references created during the execution of a native method
     * will be freed once the native method returns.
     *
     * @param jniHandles  the local handles for the current thread
     * @param object the object to handlize
     */
    public static JniHandle createLocalHandle(JniHandles jniHandles, Object object) {
        return jniHandles.allocateHandle(object, Tag.LOCAL);
    }

    /**
     * Creates a thread-local JNI handle for a reference. The handle is valid only within the
     * dynamic context of the native method that creates it, and only within that one invocation
     * of the native method. All local references created during the execution of a native method
     * will be freed once the native method returns.
     *
     * @param object the object to handlize
     */
    public static JniHandle createLocalHandle(Object object) {
        if (object == null) {
            return JniHandle.zero();
        }
        return VmThread.current().createLocalHandle(object);
    }

    public static JniHandle createGlobalHandle(Object object) {
        if (object == null) {
            return JniHandle.zero();
        }
        synchronized (globalHandles) {
            return globalHandles.allocateHandle(object, Tag.GLOBAL);
        }
    }

    public static JniHandle createWeakGlobalHandle(Object object) {
        if (object == null) {
            return JniHandle.zero();
        }
        synchronized (weakGlobalHandles) {
            return weakGlobalHandles.allocateHandle(new WeakReference<Object>(object), Tag.WEAK_GLOBAL);
        }
    }

    public static void destroyLocalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            final int tag = tag(jniHandle);
            if (tag != Tag.STACK) {
                assert tag == Tag.LOCAL;
                JniHandles jniHandles = VmThread.current().jniHandles();
                if (jniHandles == null) {
                    throw new IllegalArgumentException("invalid JNI handle: " + jniHandle.to0xHexString());
                }
                jniHandles.freeHandle(jniHandleToIndex(jniHandle));
            } else {
                // Handles to references on a thread's stack are automatically freed
                // as these handles are also on the stack
            }
        }
    }

    public static void destroyGlobalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            assert tag(jniHandle) == Tag.GLOBAL;
            synchronized (globalHandles) {
                globalHandles.freeHandle(jniHandleToIndex(jniHandle));
            }
        }
    }

    public static void destroyWeakGlobalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            assert tag(jniHandle) == Tag.WEAK_GLOBAL;
            synchronized (weakGlobalHandles) {
                weakGlobalHandles.freeHandle(jniHandleToIndex(jniHandle));
            }
        }
    }

    public static void ensureLocalHandleCapacity(int capacity) {
        VmThread.current().makeJniHandles().ensureCapacity(capacity);
    }

    public static void pushLocalFrame(int capacity) {
        VmThread.current().makeJniHandles().pushFrame(capacity);
    }

    public static JniHandle popLocalFrame(JniHandle result) {
        JniHandles jniHandles = VmThread.current().jniHandles();
        if (jniHandles == null) {
            if (!result.isZero()) {
                throw new IllegalArgumentException("invalid JNI handle: " + result.to0xHexString());
            }
            return JniHandle.zero();
        }
        return jniHandles.popFrame(result);
    }


    /**
     * Gets the number of object parameters in a given signature.
     *
     * This method is compile-time evaluated so that the first parameter to
     * {@link Intrinsics#alloca(int, boolean)} is a compile-time constant.
     */
    @Fold
    public static int handlesCount(SignatureDescriptor sig) {
        int res = 0;
        for (int i = 0; i < sig.numberOfParameters(); i++) {
            if (sig.parameterDescriptorAt(i).toKind().isReference) {
                res++;
            }
        }
        return res;
    }

    /**
     * Gets a handle for an object.
     *
     * @param handles the address of a handles block (i.e. a block of memory containing object references)
     * @param offset the offset of {@code value} in the handles block
     * @param value an object value in the handles block
     * @return if {@code value == null} then {@code 0} else {@code stackHandles.plus(offset)}
     */
    @INLINE
    public static Pointer getHandle(Pointer handles, int offset, Object value) {
        return (value == null) ? Pointer.zero() : handles.plus(offset);
    }
}

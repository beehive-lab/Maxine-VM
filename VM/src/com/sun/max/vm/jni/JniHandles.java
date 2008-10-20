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
package com.sun.max.vm.jni;

import java.lang.ref.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

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
 * In the MaxineVM VM, we need to take into account that objects may be allocated
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
 *
 * @author Doug Simon
 */
public final class JniHandles {

    private static final class Tag {
        public static final int STACK = 0;
        public static final int LOCAL = 1;
        public static final int GLOBAL = 2;
        public static final int WEAK_GLOBAL = 3;

        public static final int MASK = 3;
        public static final int BITS = 2;
    }

    static final class Frame {
        final int _start;
        final Frame _previous;
        public Frame(int start, Frame previous) {
            _start = start;
            _previous = previous;
        }
    }

    public static final int INITIAL_NUMBER_OF_HANDLES = 32;

    private static final JniHandles _globalHandles = new JniHandles();
    private static final JniHandles _weakGlobalHandles = new JniHandles();

    /**
     * The objects exposed to native code via handles.
     *
     * Note that this representation simplifies handle allocation and deferencing
     * (in the context of a "JNI handle is an index" implementation)
     * at the cost of expanding an array (allocation plus copy). It will be replaced
     * with a better representation if this expansion cost proves to be too high.
     */
    private Object[] _handles = new Object[INITIAL_NUMBER_OF_HANDLES];

    /**
     * The frames pushed by {@link JniFunctions#_PushLocalFrame}.
     */
    private Frame _frames;

    /**
     * Denotes the indexes of handles that have been {@linkplain #freeHandle(int) freed}.
     * No bit will be set for a handle at an index >= {@link #_top}.
     */
    private final BitSet _freedHandles = new BitSet(INITIAL_NUMBER_OF_HANDLES);

    /**
     * The index at which the next search for a free bit in {@link #_freedHandles} starts.
     * This optimizes allocation where the expected behavior of a native method
     * that deletes local references is such that these references are deleted in the
     * reverse order in which they were created.
     */
    private int _lastFreedIndex;

    /**
     * Number of handles allocated from this pool that are (potentially) still in use.
     * This value also denotes the index of next unused handle in {@link #_handles}.
     * The name of this field also gives some indication of how handles can be allocated
     * and freed in a stack like fashion.
     *
     * Invariant: All elements in _handles at an index greater than or equal to _top are null.
     */
    private int _top;

    /**
     * Return the "top" (i.e. current size) of this handle pool. This value can be given
     * as the parameter to the {@link #resetTop(int)} method to free handles in a stack
     * like fashion.
     */
    public int top() {
        return _top;
    }

    /**
     * Resets the "top" (i.e. current size) of this handle pool to a value
     * equal to or less than its current size.
     */
    public void resetTop(int top) {
        if (top > _top || top < 0) {
            FatalError.unexpected("Cannot reset JNI handle stack to higher stack height");
        }

        if (top != _top) {
            for (int i = top; i != _top; ++i) {
                _handles[i] = null;
            }
            _freedHandles.clear(top, _top);
            _lastFreedIndex = 0;
            _top = top;
        }
    }

    /**
     * Gets the handle at a given index.
     */
    private Object get(int index) {
        return _handles[index];
    }

    /**
     * Frees the JNI handle at a given index from this pool.
     *
     * @param index the index of the handle to free
     */
    private void freeHandle(int index) {
        _handles[index] = null;
        _freedHandles.set(index);
        _lastFreedIndex = index;
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
        if (_top < _handles.length) {
            assert _handles[_top] == null;
            _handles[_top] = object;
            return indexToJniHandle(_top++, tag);
        }

        // Now look for a handle in the free set
        int index = _freedHandles.nextSetBit(_lastFreedIndex);
        if (index == -1 && _lastFreedIndex != 0) {
            // Wrap around and search from the beginning of the array
            index = _freedHandles.nextSetBit(0);
        }
        if (index != -1) {
            assert _handles[index] == null;
            _handles[index] = object;
            _freedHandles.clear(index);
            return indexToJniHandle(index, tag);
        }

        // No space available, the handle array is expanded
        // to be double its current size.
        _handles = expandHandles(_handles, _handles.length * 2);

        // Retry - guaranteed to succeed
        return allocateHandle(object, tag);
    }

    private static JniHandle indexToJniHandle(int index, int tag) {
        return Address.fromInt(index << Tag.BITS | tag).asJniHandle();
    }

    private static int jniHandleToIndex(JniHandle jniHandle) {
        return jniHandle.asOffset().toInt() >> Tag.BITS;
    }

    private static int tag(JniHandle jniHandle) {
        return jniHandle.asOffset().toInt() & Tag.MASK;
    }

    private void pushFrame(int capacity) {
        ensureCapacity(capacity);
        _frames = new Frame(_top, _frames);
    }

    private JniHandle popFrame(JniHandle result) {
        final Object object = get(result);

        // This test means PopLocalFrame will work even if there was
        // not a corresponding call to PushLocalFrame
        if (_frames != null) {
            resetTop(_frames._start);
            _frames = _frames._previous;
        }
        return (object != null) ? allocateHandle(object, Tag.LOCAL) : result;
    }

    /**
     * Ensures that <i>at least</i> a given number of local references can be created in this pool of handles.
     */
    private void ensureCapacity(int capacity) {
        final int available = (_handles.length - _top) + _freedHandles.cardinality();
        final int extraNeeded = capacity - available;
        if (extraNeeded > 0) {
            _handles = expandHandles(_handles, _handles.length + extraNeeded);
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
            return UnsafeLoophole.cast(jniHandle.asPointer().getReference().toJava());
        }
        if (tag == Tag.LOCAL) {
            return VmThread.current().jniHandles().get(jniHandleToIndex(jniHandle));
        }
        if (tag == Tag.GLOBAL) {
            return _globalHandles.get(jniHandleToIndex(jniHandle));
        }
        assert tag == Tag.WEAK_GLOBAL;

        final WeakReference weakReference = (WeakReference) _weakGlobalHandles.get(jniHandleToIndex(jniHandle));
        return weakReference == null ? null : weakReference.get();
    }

    public static Address getAddress(JniHandle jniHandle) {
        assert tag(jniHandle) == Tag.STACK;
        return jniHandle.asPointer().getWord().asAddress();
    }

    /**
     * Creates a JNI handle for a reference on a thread's stack. This is used to pass parameters
     * to a native method from a Java method (i.e. marshalling of parameters in a JNI
     * "down" call).
     *
     * The given reference is guaranteed by the {@linkplain NativeStubGenerator JNI stub generator}
     * (the only place this method should be called) not to be null as it generates bytecode to
     * do the null test. This simplifies the platform specific backend implementation of this
     * builtin.
     *
     * @param object the object to handlize (must not be null)
     *
     * @see MakeStackVariable
     */
    @INLINE
    public static JniHandle createStackHandle(Object object) {
        return MakeStackVariable.makeStackVariable(Reference.fromJava(object)).asJniHandle();
    }

    /**
     * Creates a thread-local JNI handle for a reference. The handle is valid only within the
     * dynamic context of the native method that creates it, and only within that one invocation
     * of the native method. All local references created during the execution of a native method
     * will be freed once the native method returns.
     *
     * @param threadLocalHandles  the local handles for the current thread
     * @param object the object to handlize
     */
    public static JniHandle createLocalHandle(JniHandles threadLocalHandles, Object object) {
        return threadLocalHandles.allocateHandle(object, Tag.LOCAL);
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
        synchronized (_globalHandles) {
            return _globalHandles.allocateHandle(object, Tag.GLOBAL);
        }
    }

    public static JniHandle createWeakGlobalHandle(Object object) {
        if (object == null) {
            return JniHandle.zero();
        }
        synchronized (_weakGlobalHandles) {
            return _weakGlobalHandles.allocateHandle(new WeakReference<Object>(object), Tag.WEAK_GLOBAL);
        }
    }

    public static void destroyLocalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            final int tag = tag(jniHandle);
            if (tag != Tag.STACK) {
                assert tag == Tag.LOCAL;
                VmThread.current().jniHandles().freeHandle(jniHandleToIndex(jniHandle));
            } else {
                // Handles to references on a thread's stack are automatically freed
                // as these handles are also on the stack
            }
        }
    }

    public static void destroyGlobalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            assert tag(jniHandle) == Tag.GLOBAL;
            synchronized (_globalHandles) {
                _globalHandles.freeHandle(jniHandleToIndex(jniHandle));
            }
        }
    }

    public static void destroyWeakGlobalHandle(JniHandle jniHandle) {
        if (!jniHandle.isZero()) {
            assert tag(jniHandle) == Tag.WEAK_GLOBAL;
            synchronized (_weakGlobalHandles) {
                _weakGlobalHandles.freeHandle(jniHandleToIndex(jniHandle));
            }
        }
    }

    public static void ensureLocalHandleCapacity(int capacity) {
        VmThread.current().jniHandles().ensureCapacity(capacity);
    }

    public static void pushLocalFrame(int capacity) {
        VmThread.current().jniHandles().pushFrame(capacity);
    }

    public static JniHandle popLocalFrame(JniHandle result) {
        return VmThread.current().jniHandles().popFrame(result);
    }
}

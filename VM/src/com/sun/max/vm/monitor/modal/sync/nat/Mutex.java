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
package com.sun.max.vm.monitor.modal.sync.nat;

import java.lang.ref.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Java wrapper for a native mutex.
 *
 * TODO: Decide how to keep the NativeReference instances reachable and how to manage disposal.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */
public final class Mutex {

    static class NativeReference extends WeakReference<Mutex> {

        @CONSTANT_WHEN_NOT_ZERO
        private Pointer _mutex = Pointer.zero();

        NativeReference(Mutex m) {
            super(m, _refQueue);
        }

        private void disposeNative() {
            Memory.deallocate(_mutex);
        }
    }

    private static ReferenceQueue<Mutex> _refQueue = new ReferenceQueue<Mutex>();

    private NativeReference _native;

    private static int _size;

    static {
        new CriticalNativeMethod(Mutex.class, "nativeMutexSize");
        new CriticalNativeMethod(Mutex.class, "nativeMutexInitialize");
        new CriticalNativeMethod(Mutex.class, "nativeMutexLock");
        new CriticalNativeMethod(Mutex.class, "nativeMutexUnlock");
    }

    @C_FUNCTION
    private static native int nativeMutexSize();

    @C_FUNCTION
    private static native void nativeMutexInitialize(Pointer mutex);

    @C_FUNCTION
    private static native boolean nativeMutexUnlock(Pointer mutex);

    private static native boolean nativeMutexLock(Pointer mutex);

    public static void initialize() {
        assert MaxineVM.hostOrTarget().phase() == MaxineVM.Phase.PRIMORDIAL;
        _size = nativeMutexSize();
    }

    public Mutex() {
        _native = new NativeReference(this);
    }

    /**
     * Performs native allocation for this <code>Mutex</code>.
     */
    public void alloc() {
        _native._mutex =  Memory.mustAllocate(_size);
        nativeMutexInitialize(_native._mutex);
    }

    /**
     * Causes the current thread to perform a lock on the mutex.
     *
     * This is not intended for recursive locking, even though the native mutex implementation
     * may support it.
     *
     * @return true if an error occured in native code; false otherwise
     */
    @INLINE
    public boolean lock() {
        return nativeMutexLock(_native._mutex);
    }

    /**
     * Causes the current thread to perform an unlock on the mutex.
     *
     * The current thread must own the given mutex when calling this method, otherwise the
     * results are undefined.
     *
     * @return true if an error occured in native code; false otherwise
     * @see Mutex#lock()
     */
    @INLINE
    public boolean unlock() {
        return nativeMutexUnlock(_native._mutex);
    }

    /**
     * Returns a pointer to this <code>Mutex</code>'s native data structure.
     * @return
     */
    @INLINE
    public Pointer asPointer() {
        return _native._mutex;
    }

}

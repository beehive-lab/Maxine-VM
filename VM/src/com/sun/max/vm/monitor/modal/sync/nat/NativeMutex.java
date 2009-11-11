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
import com.sun.max.vm.monitor.modal.sync.*;

/**
 * Java wrapper for a native mutex.
 *
 * TODO: Decide how to keep the NativeReference instances reachable and how to manage disposal.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */
public final class NativeMutex extends Mutex {

    static class NativeReference extends WeakReference<NativeMutex> {

        @CONSTANT_WHEN_NOT_ZERO
        private Pointer mutex = Pointer.zero();

        NativeReference(NativeMutex m) {
            super(m, refQueue);
        }

        private void disposeNative() {
            Memory.deallocate(mutex);
        }
    }

    private static ReferenceQueue<NativeMutex> refQueue = new ReferenceQueue<NativeMutex>();

    private NativeReference nativeRef;

    private static int size;

    static {
        new CriticalNativeMethod(NativeMutex.class, "nativeMutexSize");
        new CriticalNativeMethod(NativeMutex.class, "nativeMutexInitialize");
        new CriticalNativeMethod(NativeMutex.class, "nativeMutexLock");
        new CriticalNativeMethod(NativeMutex.class, "nativeMutexUnlock");
    }

    @C_FUNCTION
    private static native int nativeMutexSize();

    @C_FUNCTION
    private static native void nativeMutexInitialize(Pointer mutex);

    @C_FUNCTION
    private static native boolean nativeMutexUnlock(Pointer mutex);

    private static native boolean nativeMutexLock(Pointer mutex);

    static void initialize() {
        assert MaxineVM.hostOrTarget().phase == MaxineVM.Phase.PRIMORDIAL;
        size = nativeMutexSize();
    }

    NativeMutex() {
        nativeRef = new NativeReference(this);
    }

    /**
     * Performs native allocation for this {@code Mutex}.
     */
    @Override
    public Mutex init() {
        if (nativeRef.mutex.isZero()) {
            nativeRef.mutex =  Memory.mustAllocate(size);
            nativeMutexInitialize(nativeRef.mutex);
        }
        return this;
    }

    @Override
    public void cleanup() {
        nativeRef.disposeNative();
    }

    @Override
    public boolean lock() {
        return nativeMutexLock(nativeRef.mutex);
    }

    /**
     * Causes the current thread to perform an unlock on the mutex.
     *
     * The current thread must own the given mutex when calling this method, otherwise the
     * results are undefined.
     *
     * @return true if an error occurred in native code; false otherwise
     * @see NativeMutex#lock()
     */
    @Override
    public boolean unlock() {
        return nativeMutexUnlock(nativeRef.mutex);
    }

    /**
     * Returns a pointer to this {@code NativeMutex}'s native data structure.
     * @return
     */
    @INLINE
    public Pointer asPointer() {
        return nativeRef.mutex;
    }

    @Override
    public long logId() {
        return nativeRef.mutex.toLong();
    }

}

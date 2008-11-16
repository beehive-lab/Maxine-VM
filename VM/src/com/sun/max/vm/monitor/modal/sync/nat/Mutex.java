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

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

/**
 * Java wrapper for a native mutex.
 *
 * @author Simon Wilkinson
 */
public final class Mutex {

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _mutex = Pointer.zero();

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
    }

    public void alloc() {
        _mutex =  Memory.mustAllocate(_size);
        nativeMutexInitialize(_mutex);
    }

    @INLINE
    public boolean lock() {
        return nativeMutexLock(_mutex);
    }

    @INLINE
    public boolean unlock() {
        return nativeMutexUnlock(_mutex);
    }

    @INLINE
    public Pointer asPointer() {
        return _mutex;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        Memory.deallocate(_mutex);
    }
}

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
 * Java wrapper for a native condition variable.
 *
 * @author Simon Wilkinson
 */
public final class ConditionVariable {

    @CONSTANT_WHEN_NOT_ZERO
    private Pointer _condition = Pointer.zero();

    private static int _size;

    private static CriticalNativeMethod _nativeConditionSize = new CriticalNativeMethod(ConditionVariable.class, "nativeConditionSize");
    private static CriticalNativeMethod _nativeConditionInitialize = new CriticalNativeMethod(ConditionVariable.class, "nativeConditionInitialize");
    private static CriticalNativeMethod _nativeConditionWait = new CriticalNativeMethod(ConditionVariable.class, "nativeConditionWait");
    private static CriticalNativeMethod _nativeConditionNotify = new CriticalNativeMethod(ConditionVariable.class, "nativeConditionSize");

    @C_FUNCTION
    private static native int nativeConditionSize();

    @C_FUNCTION
    private static native void nativeConditionInitialize(Pointer condition);

    @C_FUNCTION
    private static native boolean nativeConditionNotify(Pointer condition, boolean all);

    private static native boolean nativeConditionWait(Pointer mutex, Pointer condition, long timeoutMilliSeconds);

    public static void initialize(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.PRIMORDIAL) {
            _nativeConditionSize.link();
            _nativeConditionInitialize.link();
            _nativeConditionWait.link();
            _nativeConditionNotify.link();
            _size = nativeConditionSize();
        }
    }

    public ConditionVariable() {
    }

    public void alloc() {
        _condition =  Memory.mustAllocate(_size);
        nativeConditionInitialize(_condition);
    }

    public boolean requiresAlloc() {
        return _condition.equals(Pointer.zero());
    }

    @INLINE
    public boolean threadWait(Mutex mutex, long timeoutMilliSeconds) {
        return nativeConditionWait(mutex.asPointer(), _condition, timeoutMilliSeconds);
    }

    @INLINE
    public boolean threadNotify(boolean all) {
        return nativeConditionNotify(_condition, all);
    }

    @INLINE
    public Pointer asPointer() {
        return _condition;
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        Memory.deallocate(_condition);
    }
}

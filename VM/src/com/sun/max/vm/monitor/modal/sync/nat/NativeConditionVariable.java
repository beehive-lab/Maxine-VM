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
 * Java wrapper for a native condition variable.
 *
 * TODO: Decide how to keep the NativeReference instances reachable and how to manage disposal.
 *
 * @author Simon Wilkinson
 * @author Mick Jordan
 */
public final class NativeConditionVariable extends ConditionVariable {

    static class NativeReference extends WeakReference<NativeConditionVariable> {

        @CONSTANT_WHEN_NOT_ZERO
        private Pointer condition = Pointer.zero();

        NativeReference(NativeConditionVariable c) {
            super(c, refQueue);
        }

        private void disposeNative() {
            Memory.deallocate(condition);
        }
    }

    private static ReferenceQueue<NativeConditionVariable> refQueue = new ReferenceQueue<NativeConditionVariable>();
    private NativeReference nativeRef;

    private static int size;

    static {
        new CriticalNativeMethod(NativeConditionVariable.class, "nativeConditionSize");
        new CriticalNativeMethod(NativeConditionVariable.class, "nativeConditionInitialize");
        new CriticalNativeMethod(NativeConditionVariable.class, "nativeConditionWait");
        new CriticalNativeMethod(NativeConditionVariable.class, "nativeConditionSize");
        new CriticalNativeMethod(NativeConditionVariable.class, "nativeConditionNotify");
    }

    @C_FUNCTION
    private static native int nativeConditionSize();

    @C_FUNCTION
    private static native void nativeConditionInitialize(Pointer condition);

    @C_FUNCTION
    private static native boolean nativeConditionNotify(Pointer condition, boolean all);

    /**
     * This must be a fully informative JNI call, not a C_FUNCTION, because it can block and the stack walker needs to know the last Java frame.
     *
     * @return true if no error occurred whilst waiting; false otherwise
     */
    private static native boolean nativeConditionWait(Pointer mutex, Pointer condition, long timeoutMilliSeconds);

    static void initialize() {
        assert MaxineVM.hostOrTarget().phase == MaxineVM.Phase.PRIMORDIAL;
        size = nativeConditionSize();
    }

    NativeConditionVariable() {
        nativeRef = new NativeReference(this);
    }

    /**
     * Performs native allocation for this {@code ConditionVariable}.
     */
    @Override
    public ConditionVariable init() {
        if (nativeRef.condition.isZero()) {
            nativeRef.condition = Memory.mustAllocate(size);
            nativeConditionInitialize(nativeRef.condition);
        }
        return this;
    }

    public boolean requiresAllocation() {
        return nativeRef.condition.isZero();
    }

    @Override
    public boolean threadWait(Mutex mutex, long timeoutMilliSeconds) {
        return nativeConditionWait(((NativeMutex) mutex).asPointer(), nativeRef.condition, timeoutMilliSeconds);
    }

    @Override
    public boolean threadNotify(boolean all) {
        return nativeConditionNotify(nativeRef.condition, all);
    }

    /**
     * Returns a pointer to this {@code ConditionVariable}'s native data structure.
     * @return
     */
    @INLINE
    Pointer asPointer() {
        return nativeRef.condition;
    }

    @Override
    public long logId() {
        return nativeRef.condition.toLong();
    }

}

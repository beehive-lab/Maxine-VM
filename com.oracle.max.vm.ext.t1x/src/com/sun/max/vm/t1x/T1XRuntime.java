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
package com.sun.max.vm.t1x;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Collection of methods called from T1X templates. These methods are typically non-inlined
 * to satisfy invariants enforced for the templates. They can also be used to keep the
 * template code small.
 */
@NEVER_INLINE
public class T1XRuntime {

    // ==========================================================================================================
    // == Resolution routines ===================================================================================
    // ==========================================================================================================

    public static Address resolveAndSelectVirtualMethod(Object receiver, ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethodActor = Snippets.resolveVirtualMethod(guard);
        Address vtableEntryPoint = Snippets.selectNonPrivateVirtualMethod(receiver, virtualMethodActor).asAddress();
        return vtableEntryPoint.plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static Address resolveAndSelectInterfaceMethod(ResolutionGuard.InPool guard, final Object receiver) {
        final InterfaceMethodActor declaredInterfaceMethod = Snippets.resolveInterfaceMethod(guard);
        final Address vtableEntryPoint = Snippets.selectInterfaceMethod(receiver, declaredInterfaceMethod).asAddress();
        return vtableEntryPoint.plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static Address resolveSpecialMethod(ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethod = Snippets.resolveSpecialMethod(guard);
        return Snippets.makeEntrypoint(virtualMethod, BASELINE_ENTRY_POINT);
    }

    public static Address resolveStaticMethod(ResolutionGuard.InPool guard) {
        final StaticMethodActor staticMethod = Snippets.resolveStaticMethod(guard);
        Snippets.makeHolderInitialized(staticMethod);
        return Snippets.makeEntrypoint(staticMethod, BASELINE_ENTRY_POINT);
    }

    public static Object resolveClassForNewAndCreate(ResolutionGuard guard) {
        final ClassActor classActor = Snippets.resolveClassForNew(guard);
        Snippets.makeClassInitialized(classActor);
        final Object tuple = Snippets.createTupleOrHybrid(classActor);
        return tuple;
    }

    public static Object resolveMirror(ResolutionGuard guard) {
        return Snippets.resolveClass(guard).javaClass();
    }

    // ==========================================================================================================
    // == Field access           ================================================================================
    // ==========================================================================================================

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    public static void preVolatileRead() {
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_LOAD | LOAD_STORE) << 8))
    public static native void postVolatileRead();

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((LOAD_STORE | STORE_STORE) << 8))
    public static native void preVolatileWrite();

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INTRINSIC(MEMBAR | ((STORE_LOAD | STORE_STORE) << 8))
    public static native void postVolatileWrite();

    // ==========================================================================================================
    // == Misc routines =========================================================================================
    // ==========================================================================================================

    public static void arrayStore(final int index, final Object array, final Object value) {
        ArrayAccess.checkIndex(array, index);
        ArrayAccess.checkSetObject(array, value);
        ArrayAccess.setObject(array, index, value);
    }

    public static Object getClassMirror(ClassActor classActor) {
        return classActor.javaClass();
    }

    public static Object createTupleOrHybrid(ClassActor classActor) {
        return Snippets.createTupleOrHybrid(classActor);
    }

    public static Object createPrimitiveArray(Kind kind, int length) {
        return Snippets.createArray(kind.arrayClassActor(), length);
    }

    public static Object createReferenceArray(ArrayClassActor arrayClassActor, int length) {
        return Snippets.createArray(arrayClassActor, length);
    }

    public static int[] createMultianewarrayDimensions(Pointer sp, int n) {
        int[] dims = new int[n];
        for (int i = 0; i < n; i++) {
            int len;
            if (T1X.isAMD64()) {
                int offset = (n - i - 1) * JVMS_SLOT_SIZE;
                len = sp.readInt(offset);
            } else {
                throw T1X.unimplISA();
            }
            checkArrayDimension(len);
            dims[i] = len;
        }
        return dims;

    }

    public static Object cloneArray(int[] arr) {
        return arr.clone();
    }

    public static void checkArrayDimension(int length) {
        Snippets.checkArrayDimension(length);
    }

    public static Throwable loadException() {
        return VmThread.current().loadExceptionForHandler();
    }

    public static void rethrowException() {
        Throw.raise(VmThread.current().loadExceptionForHandler());
    }

    public static void monitorenter(Object rcvr) {
        Monitor.enter(rcvr);
    }

    public static void monitorexit(Object rcvr) {
        Monitor.exit(rcvr);
    }

    public static int f2i(float value) {
        return (int) value;
    }

    public static long f2l(float value) {
        return (long) value;
    }

    public static int d2i(double value) {
        return (int) value;
    }

    public static long d2l(double value) {
        return (long) value;
    }


}

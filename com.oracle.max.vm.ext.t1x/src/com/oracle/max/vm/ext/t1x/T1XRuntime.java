/*
 * Copyright (c) 2017, 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.oracle.max.vm.ext.t1x;

import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.methodhandle.*;
import com.sun.max.vm.profilers.tracing.numa.NUMAProfiler;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.VmThread;
import com.sun.max.vm.thread.VmThreadLocal;

/**
 * Collection of methods called from (or inlined by) T1X templates.
 * They may be annotated with {@link NEVER_INLINE} to keep the
 * template code small or to work around these constraints:
 * <ul>
 * <li>T1X templates cannot contain scalar literals</li>
 * <li>T1X templates cannot contain calls to {@link CompilerStub compiler stubs}</li>
 * </ul>
 */
public class T1XRuntime {

    // ==========================================================================================================
    // == Resolution routines ===================================================================================
    // ==========================================================================================================

    public static Address resolveAndSelectVirtualMethod(Object receiver, ResolutionGuard.InPool guard) {
        final VirtualMethodActor virtualMethodActor = Snippets.resolveVirtualMethod(guard);
        Address vtableEntryPoint = Snippets.selectNonPrivateVirtualMethod(receiver, virtualMethodActor);
        return vtableEntryPoint.plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static Address resolveAndSelectInterfaceMethod(ResolutionGuard.InPool guard, final Object receiver) {
        final InterfaceMethodActor declaredInterfaceMethod = Snippets.resolveInterfaceMethod(guard);
        final Address vtableEntryPoint = Snippets.selectInterfaceMethod(receiver, declaredInterfaceMethod);
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

    public static Address resolveAndSelectLinkToVirtual(Object memberName, Object receiver) {
        VMTarget target = VMTarget.fromMemberName(memberName);
        assert target != null;
        assert target.getVMindex() != VirtualMethodActor.NONVIRTUAL_VTABLE_INDEX;
        Trace.line(1, "T1XRuntime.resolveAndSelectLinkToVirtual target=" + target + ", mnameid=" + System.identityHashCode(memberName));
        Address vTableEntrypoint = Snippets.selectNonPrivateVirtualMethod(receiver, target.getVMindex());
        return vTableEntrypoint.plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static Address resolveAndSelectLinkToInterface(Object memberName, Object receiver) {
        VMTarget target = VMTarget.fromMemberName(memberName);
        assert target != null;
        Trace.line(1, "T1XRuntime.resolveAndSelectLinkToInterface target=" + target + ", mnameid=" + System.identityHashCode(memberName));
        Address vTableEntrypoint = Snippets.selectInterfaceMethod(receiver, UnsafeCast.asInterfaceMethodActor(target.getVmTarget()));
        return vTableEntrypoint.plus(BASELINE_ENTRY_POINT.offset() - VTABLE_ENTRY_POINT.offset());
    }

    public static Address resolveAndSelectLinkToSpecial(Object memberName, Object receiver) {
        Trace.begin(1, "T1XRuntime.resolveAndSelectLinkToSpecial: memberName=" + memberName);

        /*
         * TODO At this point we should null check the receiver and make sure that
         * its class accords with the membername.clazz.
         */
        if (receiver != null) {
            Trace.line(1, "receiver clazz=" + receiver.getClass());
        }
        Trace.line(1, "T1XRuntime.resolveAndSelectLinkToSpecial: memberName=" + memberName + "#id=" + System.identityHashCode(memberName));
        VMTarget target = VMTarget.fromMemberName(memberName);
        Trace.line(1, "T1XRuntime.resolveAndSelectLinkToSpecial: target=" + target);
        assert target != null;
        Trace.line(1, "T1XRuntime.resolveAndSelectLinkToSpecial: vmtarget=" + target.getVmTarget());
        Trace.end(1, "T1XRuntime: resolveAndSelectLinkToSpecial");
        return Snippets.makeEntrypoint(UnsafeCast.asClassMethodActor(target.getVmTarget()), BASELINE_ENTRY_POINT);
    }

    public static Address resolveAndSelectLinkToStatic(Object memberName) {
        Trace.begin(1, "T1XRuntime: resolveAndSelectLinkToStatic");
        Trace.line(1, "T1XRuntime.memberName[class=" + memberName.getClass().getName() + "]");
        Trace.line(1, "T1XRuntime.linkToStatic: memberName=" + memberName);
        Trace.line(1, "T1XRuntime.linkToStatic: memberName=" + memberName + "#id=" + System.identityHashCode(memberName));
        VMTarget target = VMTarget.fromMemberName(memberName);
        Trace.line(1, "T1XRuntime.linkToStatic: target=" + target);
        assert target != null;
        Trace.line(1, "T1XRuntime.linkToStatic: vmtarget=" + target.getVmTarget());
        Trace.end(1, "T1XRuntime: resolveAndSelectLinkToStatic");
        return Snippets.makeEntrypoint(UnsafeCast.asClassMethodActor(target.getVmTarget()), BASELINE_ENTRY_POINT);
    }

    // ==========================================================================================================
    // == Field access           ================================================================================
    // ==========================================================================================================

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    public static void preVolatileRead() {
        MemoryBarriers.barrier(MemoryBarriers.JMM_PRE_VOLATILE_READ);
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INLINE
    public static void postVolatileRead() {
        MemoryBarriers.barrier(MemoryBarriers.JMM_POST_VOLATILE_READ);
    }

    /**
     * Inserts any necessary memory barriers before a volatile read as required by the JMM.
     */
    @INLINE
    public static void preVolatileWrite() {
        MemoryBarriers.barrier(MemoryBarriers.JMM_PRE_VOLATILE_WRITE);
    }

    /**
     * Inserts any necessary memory barriers after a volatile read as required by the JMM.
     */
    @INLINE
    public static void postVolatileWrite() {
        MemoryBarriers.barrier(MemoryBarriers.JMM_POST_VOLATILE_WRITE);
    }

    @INLINE
    public static void profileTupleWrite(long address) {
        Pointer tla = VmThread.currentTLA();
        int enabled = NUMAProfiler.PROFILING_STATE.ENABLED.getValue();
        int ongoing = NUMAProfiler.PROFILING_STATE.ONGOING.getValue();
        // if PROFILER_STATE is ENABLED do profile and set PROFILER_STATE to ONGOING
        if (VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, enabled, ongoing) == enabled) {
            NUMAProfiler.profileAccess(NUMAProfiler.ACCESS_COUNTER.LOCAL_TUPLE_WRITE, address);
            // set PROFILER_STATE back to ENABLED
            VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, ongoing, enabled);
        }
    }

    @INLINE
    public static void profileArrayWrite(long address) {
        Pointer tla = VmThread.currentTLA();
        int enabled = NUMAProfiler.PROFILING_STATE.ENABLED.getValue();
        int ongoing = NUMAProfiler.PROFILING_STATE.ONGOING.getValue();
        // if PROFILER_STATE is ENABLED do profile and set PROFILER_STATE to ONGOING
        if (VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, enabled, ongoing) == enabled) {
            NUMAProfiler.profileAccess(NUMAProfiler.ACCESS_COUNTER.LOCAL_ARRAY_WRITE, address);
            // set PROFILER_STATE back to ENABLED
            VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, ongoing, enabled);
        }
    }

    @INLINE
    public static void profileTupleRead(long address) {
        Pointer tla = VmThread.currentTLA();
        int enabled = NUMAProfiler.PROFILING_STATE.ENABLED.getValue();
        int ongoing = NUMAProfiler.PROFILING_STATE.ONGOING.getValue();
        // if PROFILER_STATE is ENABLED do profile and set PROFILER_STATE to ONGOING
        if (VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, enabled, ongoing) == enabled) {
            NUMAProfiler.profileAccess(NUMAProfiler.ACCESS_COUNTER.LOCAL_TUPLE_READ, address);
            // set PROFILER_STATE back to ENABLED
            VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, ongoing, enabled);
        }
    }

    @INLINE
    public static void profileArrayRead(long address) {
        Pointer tla = VmThread.currentTLA();
        int enabled = NUMAProfiler.PROFILING_STATE.ENABLED.getValue();
        int ongoing = NUMAProfiler.PROFILING_STATE.ONGOING.getValue();
        // if PROFILER_STATE is ENABLED do profile and set PROFILER_STATE to ONGOING
        if (VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, enabled, ongoing) == enabled) {
            NUMAProfiler.profileAccess(NUMAProfiler.ACCESS_COUNTER.LOCAL_ARRAY_READ, address);
            // set PROFILER_STATE back to ENABLED
            VmThreadLocal.PROFILER_STATE.compareAndSwap(tla, ongoing, enabled);
        }
    }

    // ==========================================================================================================
    // == Misc routines =========================================================================================
    // ==========================================================================================================

    public static int[] createMultianewarrayDimensions(Pointer sp, int n) {
        int[] dims = new int[n];
        for (int i = 0; i < n; i++) {
            int len;
            if (T1X.isAMD64() || T1X.isARM() || T1X.isAARCH64() || T1X.isRISCV64()) {
                int offset = (n - i - 1) * JVMS_SLOT_SIZE;
                len = sp.readInt(offset);
            } else {
                throw T1X.unimplISA();
            }
            Snippets.checkArrayDimension(len);
            dims[i] = len;
        }
        return dims;

    }

    @NEVER_INLINE("T1X code cannot call compiler stubs")
    public static int f2i(float value) {
        return (int) value;
    }

    @NEVER_INLINE("T1X code cannot call compiler stubs")
    public static long f2l(float value) {
        return (long) value;
    }

    @NEVER_INLINE("T1X code cannot call compiler stubs")
    public static int d2i(double value) {
        return (int) value;
    }

    @NEVER_INLINE("T1X code cannot call compiler stubs")
    public static long d2l(double value) {
        return (long) value;
    }
}

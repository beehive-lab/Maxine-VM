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

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;
import static com.sun.max.vm.ext.jvmti.JVMTIVmThreadLocal.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Implementation of the JVMTI Raw Monitor functionality.
 * This is a hybrid implementation that does some synchronization
 * explicitly and some using native mutex/condition variables.
 *
 * The primary reason for not simply using native mutex/condition variables directly
 * is to finesse a deadlock that was observed in JDWP relating to the way
 * Maxine threads are suspended. For example:
 *
 * <ul>
 * <li>Thread 1 calls RawMonitorEnter(M)
 * <li>Thread 1 calls RawMonitorWait(M)
 * <li>Thread 2 calls SuspendThreadList({M,...})
 * <li>Thread 2 calls RawMonitorEnter(M)
 * <li>Thread 2 calls RawMonitorNotify(M)
 * <li>Thread 2 calls RawMonitorExit(M)
 * <li>Thread 1 wakes up in native thread library, re-acquires M then suspends on return
 * <li>Thread 2 or other thread calls RawMonitorEnter(M) and blocks => deadlock if that thread
 * was the one about to do the ResumeThreadList({M,...});
 * </ul>
 * Evidently the suspend really needs to happen "inside" the native threads library, but this
 * is not possible to achieve in general. Maxine suspends threads that are in native code
 * if and only if they return from the native method while the suspend is in effect, which
 * means that actions (like re-acquiring a mutex) in the native method cannot be prevented.
 *
 * This implementation finesses this by using spinlocks and denoting monitor acquisition by an "owner" field.
 * Native mutex/condition variables are only used internally for blocking/notification purposes.
 * Therefore a RawMonitorNotify to a suspended thread in native will only cause re-acquisition of the internal mutex
 * and not the actual RawMonitor. This internal re-acquisition is harmless. It can only effect another thread
 * trying to enter the monitor; that thread will block on the internal mutex before waiting on the ENTER condition.
 *
 * Note on use nativeConditionNotify. This is always called with {@code notifyAll==true} because although we pick
 * which thread is notified, we would have no actual control on which thread the OS layer would actually choose to release
 * if we just notified one thread. So we have to release all of them, even though only one will end up matching the
 * "while" condition. Since there are only few agent threads, this is not a big performance deal.
 */
public class JVMTIRawMonitor {

    private static final long RM_MAGIC = ('T' << 24) + ('I' << 16) + ('R' << 8) + 'M';

    public static class Monitor {
        long magic = RM_MAGIC;
        @INSPECTED
        Pointer name;
        volatile int spinLock;
        @INSPECTED
        volatile VmThread owner;
        int rcount;
        VmThread[] entryWaiters = new VmThread[8];
        VmThread[] waitWaiters = new VmThread[8];
        Word enterMutex;
        Word enterCondition;
        Word waitMutex;
        Word waitCondition;
    }

    private static int spinLockOffset = ClassActor.fromJava(Monitor.class).findLocalInstanceFieldActor("spinLock").offset();

    private static Monitor[] monitors = new Monitor[32];

    static {
        for (int i = 0; i < monitors.length; i++) {
            monitors[i] = new Monitor();
        }
    }


    static {
        new CriticalMethod(JVMTIRawMonitor.class, "create", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "destroy", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "enter", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "exit", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "wait", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "notify", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        new CriticalMethod(JVMTIRawMonitor.class, "notifyAll", null, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    private static Monitor getFreeMonitor() {
        // TODO sync
        for (int i = 0; i < monitors.length; i++) {
            if (monitors[i].name.isZero()) {
                return monitors[i];
            }
        }
        return null;
    }

    static int create(Pointer namePtr, Pointer rawMonitorPtr) {
        Monitor m = getFreeMonitor();
        if (m == null) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        Pointer namePtrCopy = CString.copy(namePtr);
        if (namePtrCopy.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }

        m.name = namePtrCopy;

        int ms = OSMonitor.nativeMutexSize();
        int cs = OSMonitor.nativeConditionSize();

        Pointer base = Memory.allocate(Size.fromInt(2 * (ms + cs)));
        Pointer enterMutex = base;
        Pointer enterCondition = enterMutex.plus(ms);
        Pointer waitMutex = enterCondition.plus(cs);
        Pointer waitCondition = waitMutex.plus(ms);
        OSMonitor.nativeMutexInitialize(enterMutex);
        OSMonitor.nativeConditionInitialize(enterCondition);
        OSMonitor.nativeMutexInitialize(waitMutex);
        OSMonitor.nativeConditionInitialize(waitCondition);

        m.enterMutex = enterMutex;
        m.enterCondition = enterCondition;
        m.waitMutex = waitMutex;
        m.waitCondition = waitCondition;

        rawMonitorPtr.setWord(Reference.fromJava(m).toOrigin());
        return JVMTI_ERROR_NONE;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native Monitor asMonitor(Object o);

    private static Monitor validate(Word rawMonitor) {
        Monitor m = asMonitor(Reference.fromOrigin(rawMonitor.asPointer()).toJava());
        if (m.magic == RM_MAGIC) {
            return m;
        } else {
            return null;
        }
    }

    static int destroy(Word rawMonitor) {
        // TODO check ownership
        Monitor m = validate(rawMonitor);
        if (m == null) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        Memory.deallocate(m.name);
        m.name = Pointer.zero();
        return JVMTI_ERROR_NONE;
    }

    static int enter(Word rawMonitor) {
        return enter(rawMonitor, true);
    }

    static int enter(Word rawMonitor, boolean unlock) {
        Monitor m = validate(rawMonitor);
        if (m == null) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        VmThread self = VmThread.current();
        spinLock(m);
        if (m.owner == null) {
            // not owned before, now we own it
            m.owner = self;
        } else if (m.owner == self) {
            // we already owned it, inc recursion count
            m.rcount++;
        } else {
            // someone else owns it, block on enter condition
            addWaiter(m.entryWaiters, self);
            spinUnlock(m);
            // having released the spin lock (which is required since we are expecting to block)
            // must use OS mutex protection
            OSMonitor.nativeMutexLock(m.enterMutex);
            while (m.owner != self) {
                OSMonitor.nativeConditionWait(m.enterMutex, m.enterCondition, 0);
            }
            OSMonitor.nativeMutexUnlock(m.enterMutex);
            // now we own it
            unlock = false;
        }
        if (unlock) {
            spinUnlock(m);
        }
        return JVMTI_ERROR_NONE;
    }

    static int exit(Word rawMonitor) {
        Monitor m = validate(rawMonitor);
        if (m == null) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        int result = JVMTI_ERROR_NONE;
        spinLock(m);
        if (notOwner(m)) {
            result = JVMTI_ERROR_NOT_MONITOR_OWNER;
        } else if (m.rcount > 0) {
            m.rcount--;
        } else {
            // final release
            // anyone waiting?
            releaseEnterWaiter(m);
        }
        spinUnlock(m);
        return result;
    }

    private static void releaseEnterWaiter(Monitor m) {
        VmThread newOwner = null;
        OSMonitor.nativeMutexLock(m.enterMutex);
        for (int i = 0; i < m.entryWaiters.length; i++) {
            VmThread r = m.entryWaiters[i];
            if (r != null) {
                for (int j = i; j < m.entryWaiters.length - 1; j++) {
                    m.entryWaiters[j] = m.entryWaiters[j + 1];
                }
                m.entryWaiters[m.entryWaiters.length - 1] =  null;
                newOwner = r;
                break;
            }
        }
        m.owner = newOwner;
        OSMonitor.nativeConditionNotify(m.enterCondition, true);
        OSMonitor.nativeMutexUnlock(m.enterMutex);
    }

    private static void addWaiter(VmThread[] waiters, VmThread vmThread) {
        for (int i = 0; i < waiters.length; i++) {
            if (waiters[i] == null) {
                waiters[i] = vmThread;
                return;
            }
        }
        Log.println("JVMTIRawMonitor too many waiting threads");
        MaxineVM.native_exit(-1);
    }

    static int wait(Word rawMonitor, long millis) {
        Monitor m = validate(rawMonitor);
        if (m == null) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        int result = JVMTI_ERROR_NONE;
        spinLock(m);
        if (notOwner(m)) {
            result = JVMTI_ERROR_NOT_MONITOR_OWNER;
        } else {
            if (millis == -1) {
                // JDWP seems to use -1 when it means 0
                millis = 0;
            }
            addWaiter(m.waitWaiters, VmThread.current());
            // save recursion count
            int rcount = m.rcount;
            releaseEnterWaiter(m);
            spinUnlock(m);
            // wait for notify
            OSMonitor.nativeMutexLock(m.waitMutex);
            Pointer tla = VmThread.currentTLA();
            while (!JVMTIVmThreadLocal.bitIsSet(tla, JVMTI_RAW_NOTIFY)) {
                OSMonitor.nativeConditionWait(m.waitMutex, m.waitCondition, millis);
            }
            JVMTIVmThreadLocal.unsetBit(tla, JVMTI_RAW_NOTIFY);
            OSMonitor.nativeMutexUnlock(m.waitMutex);
            // re-acquire monitor but hold spinlock
            enter(rawMonitor, false);
            // reset our recursion count
            m.rcount = rcount;
        }
        spinUnlock(m);
        return result;
    }

    static int notify(Word rawMonitor) {
        return notify(rawMonitor, false);
    }

    static int notifyAll(Word rawMonitor) {
        return notify(rawMonitor, true);
    }

    static int notify(Word rawMonitor, boolean all) {
        Monitor m = validate(rawMonitor);
        if (m == null) {
            return JVMTI_ERROR_INVALID_MONITOR;
        }
        int result = JVMTI_ERROR_NONE;
        spinLock(m);
        if (notOwner(m)) {
            return JVMTI_ERROR_NOT_MONITOR_OWNER;
        } else {
            boolean listChanged = false;
            OSMonitor.nativeMutexLock(m.waitMutex);
            for (int i = 0; i < m.waitWaiters.length; i++) {
                VmThread r = m.waitWaiters[i];
                if (r != null) {
                    listChanged = true;
                    // mark as notified
                    JVMTIVmThreadLocal.setBit(r.tla(), JVMTI_RAW_NOTIFY);
                    m.waitWaiters[i] = null;
                    if (!all) {
                        break;
                    }
                }
            }
            OSMonitor.nativeConditionNotify(m.waitCondition, true);
            OSMonitor.nativeMutexUnlock(m.waitMutex);
            if (listChanged) {
                // collapse list
                for (int i = 0; i < m.waitWaiters.length - 1; i++) {
                    if (m.waitWaiters[i] == null) {
                        m.waitWaiters[i] = m.waitWaiters[i + 1];
                    }
                }
                m.waitWaiters[m.waitWaiters.length - 1] = null;
            }

        }
        spinUnlock(m);
        return result;

    }

    /**
     * Check that this thread owns the monitor.
     * @param rawMonitor
     */
    private static boolean notOwner(Monitor m) {
        return m.owner != VmThread.current();
    }

    @INLINE
    private static void spinLock(Monitor m) {
        while (Reference.fromJava(m).compareAndSwapInt(spinLockOffset, 0, 1) != 0) {
            Intrinsics.pause();
        }
    }

    @INLINE
    private static void spinUnlock(Monitor m) {
        m.spinLock = 0;
    }

}

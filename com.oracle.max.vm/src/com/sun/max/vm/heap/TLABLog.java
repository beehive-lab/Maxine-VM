/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;

/**
 * Log of TLAB allocations.
 * The log records three information per TLAB allocation: pc of the allocation site, allocated cell, size of the allocated cell.
 * The log is sized to accommodate the maximum number of allocations that can be made with a TLAB, i.e.,
 * the TLAB maximum size / smallest object size.
 * The log is flushed at TLAB refill.
 */
public final class TLABLog {
    public static final String TLAB_LOG_THREAD_LOCAL_NAME = "TLAB_LOG";
    public static final String TLAB_LOG_TAIL_THREAD_LOCAL_NAME = "TLAB_LOG_TAIL";

    public static final VmThreadLocal TLAB_LOG
        = new VmThreadLocal(TLAB_LOG_THREAD_LOCAL_NAME, true, "TLABLog: TLAB allocation log, null if not used", Nature.Single);

    public static final VmThreadLocal TLAB_LOG_TAIL
        = new VmThreadLocal(TLAB_LOG_TAIL_THREAD_LOCAL_NAME, false, "TLABLog: tail of TLAB allocation log, zero if logging disabled", Nature.Single);

    public static boolean LogTLABAllocation = false;

    static HeapScheme heapScheme;
    static Address logBufferAllocator;
    static TLABLog logBufferList;

    private final long [] log;
    private VmThread logger;
    private TLABLog next;

    private TLABLog(Size tlabSize) {
        int maxLogRecords = tlabSize.dividedBy(HeapSchemeAdaptor.MIN_OBJECT_SIZE).toInt();
        log = new long[maxLogRecords * 3];
    }

    public static void initialize(Address customAllocator) {
        heapScheme = VMConfiguration.vmConfig().heapScheme();
        logBufferAllocator = customAllocator;
    }

    private static synchronized void release(TLABLog logBuffer) {
        logBuffer.next = logBufferList;
        logBufferList = logBuffer;
    }

    // FIXME: for now, the tlab size is constant. This makes this logging mechanism to work simply.
    // FIXME: use non-blocking list of log buffer.

    private static synchronized TLABLog acquire(Pointer etla, Size tlabSize) {
        TLABLog logBuffer = logBufferList;
        if (logBuffer == null) {
            heapScheme.enableCustomAllocation(logBufferAllocator);
            logBuffer = new TLABLog(tlabSize);
            heapScheme.disableCustomAllocation();
        } else {
            logBufferList.next = logBuffer.next;
            logBuffer.next = null;
        }
        logBuffer.logger =  UnsafeCast.asVmThread(VmThreadLocal.VM_THREAD.loadRef(etla).toJava());
        return logBuffer;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native TLABLog asTLABLog(Object object);

    private static TLABLog tlabLog(Pointer etla) {
        return asTLABLog(TLAB_LOG.loadRef(etla).toJava());
    }

    public static void doOnRetireTLAB(Pointer etla) {
        Pointer logTail = TLAB_LOG_TAIL.load(etla);
        if (!logTail.isZero()) {
            // were logging TLAB allocation. Flush them out.
            TLABLog tlabLog = tlabLog(etla);
            FatalError.check(tlabLog != null, "Cannot have non-null tlab log tail without TLABLog");
            tlabLog.flush(logTail);
            TLAB_LOG_TAIL.store(etla, Pointer.zero());
            if (!LogTLABAllocation) {
                TLAB_LOG.store(etla, Pointer.zero());
                release(tlabLog);
            }
        }
    }

    public static void doOnRefillTLAB(Pointer etla, Size tlabSize) {
        if (!LogTLABAllocation) {
            return;
        }
        TLABLog tlabLog = tlabLog(etla);
        if (tlabLog == null) {
            tlabLog = acquire(etla, tlabSize);
            TLAB_LOG.store(etla, Reference.fromJava(tlabLog));
        }
        TLAB_LOG_TAIL.store(etla, tlabLog.logStart());
    }

    @INLINE
    public static void record(Pointer etla, Pointer allocationSite, Pointer allocatedCell, Size cellSize) {
        final Pointer logTail = TLAB_LOG_TAIL.load(etla);
        if (!logTail.isZero()) {
            logTail.setWord(0, allocationSite);
            logTail.setWord(1, allocatedCell);
            logTail.setWord(2, cellSize);
            TLAB_LOG_TAIL.store(etla, logTail.plusWords(3));
        }
    }

    private Pointer logStart() {
        return Reference.fromJava(log).toOrigin().plus(Layout.longArrayLayout().getElementOffsetFromOrigin(0));
    }

    @NO_SAFEPOINT_POLLS("GC debugging")
    private void flush(Pointer logTail) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("TLAB allocation log for ");
        Log.printThread(logger, true);
        Pointer p = logStart();
        while (p.lessThan(logTail)) {
            Log.print(p.getWord(0).asAddress());
            Log.print(", ");
            Log.print(p.getWord(1).asAddress());
            Log.print(", ");
            Log.println(p.getWord(2).asSize().toLong());
            p = p.plusWords(3);
        }
        Log.unlock(lockDisabledSafepoints);
    }
}

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

import static com.sun.max.platform.Platform.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.thread.VmThreadLocal.Nature;

/**
 * Log of TLAB allocations. Used for GC debugging at the moment.
 *
 * The log records three information per TLAB allocation: pc of the allocation site, allocated cell, size of the allocated cell.
 * Information is recorded on per-thread log buffers which are flushed when they are full, or at refill / reset time.
 * Log is enabled / disabled using a global flag which may be set by other GC debugging tools.
 *
 * Thread local are allocated directly off virtual memory to avoid complication and disturbance to the heap being debugged.
 * The log buffer are allocated on demand.
 */
public final class TLABLog {
    public static final String TLAB_LOG_TAIL_THREAD_LOCAL_NAME = "TLAB_LOG_TAIL";

    public static boolean TraceTLABAllocation = false;

    static {
        VMOptions.addFieldOption("-XX:", "TraceTLABAllocation", TLABLog.class, "Trace every allocation from TLABs when in DEBUG mode", Phase.STARTING);
    }

    /**
     * Tail of the thread-local log buffer of a thread. If zero, logging is disabled,or the thread wasn't allocated a log yet.
     */
    public static final VmThreadLocal TLAB_LOG_TAIL
        = new VmThreadLocal(TLAB_LOG_TAIL_THREAD_LOCAL_NAME, false, "TLABLog: tail of TLAB allocation log, zero if logging disabled", Nature.Single);

    static HeapScheme heapScheme;
    static Address logBufferAllocator;

    public static final int LOG_BUFFER_SIZE = platform().pageSize;
    public static final int LOG_BUFFER_HEADER_SIZE = Word.size();
    public static final int LOG_RECORD_SIZE = Word.size() * 3;
    public static final long LOG_BUFFER_TAIL_MASK = ~((long) LOG_BUFFER_SIZE - 1);

    private static void release(Pointer logHead) {
        VirtualMemory.deallocate(logHead, Size.fromInt(LOG_BUFFER_SIZE),  VirtualMemory.Type.DATA);
    }

    private static Pointer allocate(Pointer etla) {
        Pointer logBuffer = VirtualMemory.allocate(Size.fromInt(LOG_BUFFER_SIZE), VirtualMemory.Type.DATA);
        logBuffer.setWord(etla);
        int numUnusableWords =  ((LOG_BUFFER_SIZE % LOG_RECORD_SIZE) - LOG_BUFFER_HEADER_SIZE) >> Word.widthValue().log2numberOfBytes;
        Pointer logBufferEnd = logBuffer.plus(LOG_BUFFER_SIZE);
        // Set up the unusable part of the log buffer with self pointers, so that
        // one can quickly test if there's enough space for a new record by testing if log.getWord(wordIndex) == log.plusWords(wordIndex)
        while (numUnusableWords > 0) {
            final Pointer p = logBufferEnd.minusWords(numUnusableWords--);
            p.setWord(p);
        }
        return logBuffer.plus(LOG_BUFFER_HEADER_SIZE);
    }

    public static void doOnRetireTLAB(Pointer etla) {
        Pointer logTail = TLAB_LOG_TAIL.load(etla);
        if (!logTail.isZero()) {
            // were logging TLAB allocation. Flush them out.
            flush(logTail);
            if (!TraceTLABAllocation) {
                TLAB_LOG_TAIL.store(etla, Pointer.zero());
                release(logHead(logTail));
            } else {
                TLAB_LOG_TAIL.store(etla,  logStart(logTail));
            }
        }
    }

    public static void doOnRefillTLAB(Pointer etla, Size tlabSize, boolean force) {
        if (!(TraceTLABAllocation || force)) {
            return;
        }
        Pointer logTail = TLAB_LOG_TAIL.load(etla);
        TLAB_LOG_TAIL.store(etla, logTail.isZero() ?  allocate(etla) : logStart(logTail));
    }

    @INLINE
    public static void record(Pointer etla, Pointer allocationSite, Pointer allocatedCell, Size cellSize) {
        Pointer logTail = TLAB_LOG_TAIL.load(etla);
        if (!logTail.isZero()) {
            if (logTail.getWord().asPointer().equals(logTail)) {
                flush(logTail);
                logTail = logStart(logTail);
            }
            logTail.setWord(0, allocationSite);
            logTail.setWord(1, allocatedCell);
            logTail.setWord(2, cellSize);
            TLAB_LOG_TAIL.store(etla, logTail.plus(LOG_RECORD_SIZE));
        }
    }

    private static Pointer logHead(Pointer logTail) {
        return logTail.and(LOG_BUFFER_TAIL_MASK);
    }

    private static Pointer logStart(Pointer logTail) {
        return logHead(logTail).plus(LOG_BUFFER_HEADER_SIZE);
    }

    private static Pointer logEnd(Pointer logTail) {
        return logTail.and(LOG_BUFFER_TAIL_MASK).plus(LOG_BUFFER_SIZE);
    }

    private static VmThread logger(Pointer logTail) {
        return VmThread.fromTLA(logHead(logTail).getWord().asPointer());
    }

    public static Pointer flushAndGetStart(Pointer logTail) {
        flush(logTail);
        return  logStart(logTail);
    }

    @NO_SAFEPOINT_POLLS("GC debugging")
    private static void flush(Pointer logTail) {
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("TLAB allocation log for ");
        Log.printThread(logger(logTail), false);
        Pointer p = logStart(logTail);
        Log.print(" #records = ");
        Log.println(logTail.minus(p).dividedBy(LOG_RECORD_SIZE).toInt());
        int logRecordId = 1;
        while (p.lessThan(logTail)) {
            Log.print(logRecordId++);
            Log.print(" ");
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

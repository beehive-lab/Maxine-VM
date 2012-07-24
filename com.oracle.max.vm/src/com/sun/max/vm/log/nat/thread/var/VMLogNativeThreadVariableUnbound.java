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
package com.sun.max.vm.log.nat.thread.var;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.log.nat.thread.*;
import com.sun.max.vm.thread.*;

/**
 * Per-thread log buffer with variable sized records. Most space efficient but most complicated! Once the circular
 * buffer has cycled, new records can partially overlap old ones, so we have to record the logical "first" record offset
 * and keep it updated. Traversing records also requires using the arg count to correctly locate the
 * next record. An additional complication is that a record might not fit in the slot at the end of the buffer. We do
 * not split records in this case but leave a "hole". We mark this as "FREE" so that scanning will skip it, and
 * maintain the invariant that a full buffer is always completely populated. (For simplicity we actually leave a
 * sequence of zero-arg holes.)
 *
 * On entry/exit to/from {@link #getRecord}, the values of {@code firstOffset/nextOffset} are always {@code >= 0} and
 * {@code < logSize}. However, during the method they may exceed it, which simplifies calculating the new value of
 * firstOffset.
 *
 * Note, as a consequence of the potential overlap of old records, a GC/flush scan cannot just start at the
 * beginning of the buffer; it must start at {@code firstOffset} to avoid encountering a partially overwritten record.
 *
 * A log with a {@link com.sun.max.vm.log.VMLog.Flusher} will only overwrite records after they have been passed to the flusher.
 * The records are all flushed at once and then the log is then reset to empty.
 *
 * This class is abstract because it does not define the specific thread locals that are used to control
 * the buffer. That is left to a concrete subclass, thereby allowing multiple instances of this log to co-exist
 * for a thread at runtime.
 *
 */
public abstract class VMLogNativeThreadVariableUnbound extends VMLogNativeThread {

    @Override
    public void threadStart() {
        // we want to allocate the NativeRecord early;
        // crucial for the VMOperation thread, otherwise GC logging will fail
        if (!MaxineVM.isPrimordialOrPristine()) {
            getNativeRecord();
        }
    }

    @Override
    @NO_SAFEPOINT_POLLS("atomic")
    protected Record getRecord(int argCount) {
        Pointer holeAddress = Pointer.zero();
        int uuid = getUniqueId();
        Pointer tla = VmThread.currentTLA();
        Pointer buffer = getBuffer(tla);
        Address offsets = vmLogBufferOffsetsTL.load(tla);
        final int nextOffset = (int) (offsets.toLong() & NEXT_OFFSET_MASK);
        long firstOffsetAndWrap = offsets.toLong() & FIRST_OFFSET_WRAP_MASK;
        long wrap = firstOffsetAndWrap & WRAPPED;
        Pointer recordAddress = buffer.plus(nextOffset);
        int recordSize = ARGS_OFFSET + argCount * Word.size();
        int newNextOffset = nextOffset + recordSize;

        if (newNextOffset >= logSize) {
            if (newNextOffset > logSize) {
                // record would straddle buffer end; remember hole address
                holeAddress = recordAddress;
                recordAddress = buffer;
                newNextOffset = logSize + recordSize;
            } // else exact fit, but next wraps
            wrap = WRAPPED;
        } // else fits with no wrap

        if (wrap == WRAPPED) {
            int firstOffset = (int) ((firstOffsetAndWrap >> FIRST_OFFSET_SHIFT) & SHIFTED_FIRST_OFFSET_MASK);
            if (firstOffset < newNextOffset) {
                // may need to flush the log, as are just about to step on a live record
                if (flusher != null) {
                    flusher.start();
                    scanOrFlushLog(tla, null, false);
                    flusher.end();
                    // reset
                    wrap = 0;
                    firstOffset = 0;
                    newNextOffset = 0;
                    recordAddress = buffer;
                    holeAddress = Pointer.zero();
                }

                // skip over records until we are >= newNextOffset
                while (firstOffset < newNextOffset) {
                    firstOffset = nextRecordOffset(buffer, firstOffset);
                }
            }
            // firstOffset needs to wrap now, unless we flushed
            firstOffsetAndWrap = ((long) modLogSize(firstOffset)) << FIRST_OFFSET_SHIFT | wrap;
            if (holeAddress.isNotZero()) {
                Pointer bufferEnd = buffer.plus(logSize);
                while (holeAddress.lessThan(bufferEnd)) {
                    holeAddress.setInt(Record.FREE);
                    holeAddress = holeAddress.plus(Word.size());
                }
            }
        }
        vmLogBufferOffsetsTL.store3(Address.fromLong(firstOffsetAndWrap | modLogSize(newNextOffset)));

        recordAddress.writeInt(ID_OFFSET, uuid);
        NativeRecord record = getNativeRecord();
        record.address = recordAddress;

        return record;
    }

    /**
     * Returns the offset of the next record after the one at {@code offset}.
     * N.B. This does not wrap to ensure that the calling loop terminates.
     * @param buffer
     * @param offset
     */
    private int nextRecordOffset(Pointer buffer, int offset) {
        int argCount = Record.getArgCount(buffer.plus(modLogSize(offset)).getInt());
        return offset + ARGS_OFFSET + argCount * Word.size();
    }

    @Override
    protected final int getLogSize() {
        // assume average arg size is midpoint
        return super.getLogSize() / 2;
    }

    @Override
    public void scanLog(Pointer tla, PointerIndexVisitor visitor) {
        scanOrFlushLog(tla, visitor, true);
    }

    private void flushLog(Pointer tla) {
        try {
            flusher.start();
            scanOrFlushLog(tla, null, false);
        } finally {
            flusher.end();
        }
    }

    // temporary -- to track VMLog bug
    private int debug_next_offset = -1;
    private int debug_offset = -1;
    private int debug_last_offset = -1;
    private int debug_first_offset = -1;

    private void scanOrFlushLog(Pointer tla, PointerIndexVisitor visitor, boolean scanning) {
        long offsets = vmLogBufferOffsetsTL.load(tla).toLong();
        int nextOffset = nextOffset(offsets);
        if (nextOffset == 0 && !isWrapped(offsets)) {
            // nothing to scan (and therefore no buffer or NativeRecord yet)
            return;
        }

        Pointer buffer = getBuffer(tla);
        NativeRecord r = getNativeRecord();
        int offset = firstOffset(offsets);

        if (MaxineVM.isDebug()) {
            debug_next_offset = nextOffset;
            debug_first_offset = offset;
            debug_offset = offset;
        }
        while (offset != nextOffset) {
            r.address = buffer.plus(offset);
            int header = r.getHeader();
            // variable length records can cause holes
            if (!Record.isFree(header)) {
                if (scanning) {
                    scanArgs(r, r.address.plus(ARGS_OFFSET), visitor);
                } else {
                    flusher.flushRecord(r);
                }
            }
            offset = modLogSize(offset + ARGS_OFFSET + r.getArgCount() * Word.size());
            if (MaxineVM.isDebug()) {
                debug_last_offset = debug_offset;
                debug_offset = offset;
            }
        }
    }

    @Override
    public void flushLog() {
        flushLog(VmThread.currentTLA());
        // reset the log
        vmLogBufferOffsetsTL.store3(Address.zero());
    }

}

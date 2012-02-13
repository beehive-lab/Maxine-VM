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
import com.sun.max.vm.log.nat.thread.*;
import com.sun.max.vm.thread.*;

/**
 * Per-thread log buffer with variable sized records.
 * Most space efficient but most complicated!
 * Once the circular buffer has cycled, new records can
 * partially overlap old ones, so we have to record the
 * logical "first" record id and keep it updated (for the Inspector).
 * Traversing records also requires using the arg count
 * to correctly locate the next record. An additional complication
 * is that a record might not fit in the slot at the end of the
 * buffer. We do not split records in this case but leave
 * a "hole". We mark this as "FREE" so that the Inspector
 * will skip it, and maintain the invariant that a full
 * buffer is always completely populated. (For simplicity
 * we actually leave a sequence of zero-arg holes.)
 *
 * On entry/exit, the values of firstOffset/nextOffset
 * are always {@code >= 0} and {@code < logSize}.
 * However, during the method they may exceed it, which
 * simplifies calculating the new value of firstOffset.
 *
 */
public class VMLogNativeThreadVariable extends VMLogNativeThread {

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
    }

    @Override
    @NEVER_INLINE
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
        Address offsets = VMLOG_BUFFER_OFFSETS.load(tla);
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

        if (wrap != 0) {
            int firstOffset = (int) ((firstOffsetAndWrap >> FIRST_OFFSET_SHIFT) & SHIFTED_FIRST_OFFSET_MASK);
            // skip over records until we are >= newNextOffset
            while (firstOffset < newNextOffset) {
                firstOffset = nextRecordOffset(buffer, firstOffset);
            }
            // firstOffset may need to wrap now
            firstOffsetAndWrap = ((long) modLogSize(firstOffset)) << FIRST_OFFSET_SHIFT | WRAPPED;
            if (holeAddress.isNotZero()) {
                Pointer bufferEnd = buffer.plus(logSize);
                while (holeAddress.lessThan(bufferEnd)) {
                    holeAddress.setInt(Record.FREE);
                    holeAddress = holeAddress.plus(Word.size());
                }
            }
        }
        VMLOG_BUFFER_OFFSETS.store3(Address.fromLong(firstOffsetAndWrap | modLogSize(newNextOffset)));

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
     * @return
     */
    private int nextRecordOffset(Pointer buffer, int offset) {
        int argCount = Record.getArgCount(buffer.plus(modLogSize(offset)).getInt());
        return offset + ARGS_OFFSET + argCount * Word.size();
    }

    @Override
    protected int getLogSize() {
        // assume average arg size is midpoint
        return super.getLogSize() / 2;
    }

}

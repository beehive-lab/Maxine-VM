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
package com.sun.max.vm.log.nat.thread;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.nat.*;
import com.sun.max.vm.thread.*;

/**
 * Common superclass for per-thread log buffers.
 *
 * Information on the state of the buffer is stored in a {@link VmThreadLocal},
 * {@link VMLogNativeThread#vmLogBufferOffsetsTL}. The actual pair of
 * thread local slots used for this buffer must be set by calling
 * {@link #setBufferThreadLocals(VmThreadLocal, VmThreadLocal)}.
 *
 * The thread local state comprises following:
 *
 * <ul>
 * <li>the offset of the first valid record, {@link #firstOffset}</li>
 * <li>the offset where the next record should be written, {@link #nextOffset}</li>
 * <li>a (sticky) bit to indicate that the buffer has wrapped {@link #WRAPPED}.
 * </ul>
 * Offsets are in bytes.
 * <p>
 * Note that unless the buffer is empty, {@link #firstOffset} is never equal
 * to {@link #nextOffset}.
 *
 * The exact layout of the native buffer, e.g., whether records are fixed size or
 * variable size is left to the concrete subclass.
 *
 * Note that in order for the Inspector to be able to recreate a globally ordered
 * set of records (by id), we must store the id in the record itself.
 *
 */
public abstract class VMLogNativeThread extends VMLogNative {


    public static final int ID_OFFSET = Ints.SIZE;
    public static final int ARGS_OFFSET = 2 * Ints.SIZE;
    public static final int NEXT_OFFSET_MASK = 0x7FFFFFFE;
    public static final int FIRST_OFFSET_SHIFT = 32;
    public static final int SHIFTED_FIRST_OFFSET_MASK = 0x7FFFFFE;
    public static final long WRAPPED = 0x100000000L; // set in FIRST_OFFSET to indicate buffer has wrapped (sticky)
    public static final long FIRST_OFFSET_WRAP_MASK = 0x7FFFFFF00000000L;
    /**
     * We use bit 0 set to 1 to denote that logging is disabled for this thread.
     * Since this will suppress all calls to {@link #getRecord(int)} we
     * do not need to worry about masking it out in {@link #getRecord(int)}.
     */
    public static final int DISABLED = 0x1;
    public static final long DISABLED_MASK = 0x7FFFFFFFFFFFFFFEL;

    @CONSTANT
    protected VmThreadLocal vmLogBufferTL;
    @CONSTANT
    protected VmThreadLocal vmLogBufferOffsetsTL;

    /**
     * Sets the specific thread locals used to control this log.
     * @param vmLogBufferTL
     * @param vmLogBufferOffsetsTL
     */
    public void setBufferThreadLocals(VmThreadLocal vmLogBufferTL, VmThreadLocal vmLogBufferOffsetsTL) {
        this.vmLogBufferTL = vmLogBufferTL;
        this.vmLogBufferOffsetsTL = vmLogBufferOffsetsTL;
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.PRIMORDIAL) {
            vmLogBufferTL.store3(logBuffer);
        }
    }

    @Override
    /**
     * Space for header and the id.
     */
    protected final int getArgsOffset() {
        return ARGS_OFFSET;
    }

    @Override
    protected int getLogSize() {
        return logEntries * defaultNativeRecordSize;
    }

    @NEVER_INLINE
    private Pointer allocateBuffer() {
        Pointer buffer = Memory.allocate(Size.fromInt(logSize));
        vmLogBufferTL.store3(buffer);
        return buffer;
    }

    @INLINE
    protected final Pointer getBuffer(Pointer tla) {
        Pointer buffer = vmLogBufferTL.load(tla);
        if (buffer.isZero()) {
            buffer = allocateBuffer();
        }
        return buffer;
    }

    @INLINE
    protected final int modLogSize(int offset) {
        // offset is either < logSize or < 2 * logSize
        return offset < logSize ? offset : offset - logSize;
    }

    @Override
    public boolean setThreadState(boolean value) {
        int bit = value ? 0 : DISABLED;
        Pointer tla = VmThread.currentTLA();
        Address offsets = vmLogBufferOffsetsTL.load(tla);
        vmLogBufferOffsetsTL.store3(Address.fromLong((offsets.toLong() & DISABLED_MASK) | bit));
        return (offsets.toLong() & DISABLED) == 0;
    }

    @Override
    public boolean threadIsEnabled() {
        return (vmLogBufferOffsetsTL.load(VmThread.currentTLA()).toLong() & DISABLED) == 0;
    }

    // Convenience methods for accessing the data in VMLOG_BUFFER_OFFSETS

    @INLINE
    protected final boolean isWrapped(long offsets) {
        return (offsets & WRAPPED) != 0;
    }

    @INLINE
    protected final int nextOffset(long offsets) {
        return (int) (offsets & NEXT_OFFSET_MASK);
    }

    @INLINE
    protected final int firstOffset(long offsets) {
        return (int) ((offsets >> FIRST_OFFSET_SHIFT) & SHIFTED_FIRST_OFFSET_MASK);
    }

}

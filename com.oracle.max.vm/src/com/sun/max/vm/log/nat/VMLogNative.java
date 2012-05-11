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
package com.sun.max.vm.log.nat;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * Subclasses of this implementation uses native buffers and the "records" are stored as a flattened array. The records
 * are, however, not real objects, which requires some subterfuge in the case where a client, e.g, {@VMLogger} wants
 * to see a real object. We handle this with a {@link VmThreadLocal} that holds a {@link NativeRecord
 * subclass} of {@link VMLog.Record} which records the native record address, and overrides all the methods that access
 * the state of the record. Evidently this record must be "used" promptly and not cached, as the value of the underlying
 * state will change every time the {@link NativeRecord#address} is set, which will happen at least every time
 * {@link VMLog#getRecord} is called.
 */
public abstract class VMLogNative extends VMLog {

    /**
     * Where the per-thread instance of {@link NativeRecord} is stored.
     */
    private static final VmThreadLocal VMLOG_RECORD = new VmThreadLocal("VMLOG_RECORD", true, "VMLog.Record");

    @INTRINSIC(UNSAFE_CAST) private static native NativeRecord asNativeRecord(Object object);

    public static class NativeRecord extends VMLog.Record {
        public Pointer address;
        public final int argsOffset;

        protected NativeRecord(int argsOffset) {
            this.argsOffset = argsOffset;
        }

        @Override
        public int getHeader() {
            return address.getInt();
        }

        @Override
        public void setHeader(int header) {
            address.setInt(header);
        }

        @Override
        public Word getArg(int n) {
            return address.getWord(argsOffset, n - 1);
        }

        @Override
        public void setArgs(Word arg1) {
            address.setWord(argsOffset, 0, arg1);
        }
        @Override
        public void setArgs(Word arg1, Word arg2) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
            address.setWord(argsOffset, 3, arg4);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
            address.setWord(argsOffset, 3, arg4);
            address.setWord(argsOffset, 4, arg5);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
            address.setWord(argsOffset, 3, arg4);
            address.setWord(argsOffset, 4, arg5);
            address.setWord(argsOffset, 5, arg6);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
            address.setWord(argsOffset, 3, arg4);
            address.setWord(argsOffset, 4, arg5);
            address.setWord(argsOffset, 5, arg6);
            address.setWord(argsOffset, 6, arg7);
        }
        @Override
        public void setArgs(Word arg1, Word arg2, Word arg3, Word arg4, Word arg5, Word arg6, Word arg7, Word arg8) {
            address.setWord(argsOffset, 0, arg1);
            address.setWord(argsOffset, 1, arg2);
            address.setWord(argsOffset, 2, arg3);
            address.setWord(argsOffset, 3, arg4);
            address.setWord(argsOffset, 4, arg5);
            address.setWord(argsOffset, 5, arg6);
            address.setWord(argsOffset, 6, arg7);
            address.setWord(argsOffset, 7, arg8);
        }

        /**
         * Default size of a log record in the native buffer, assuming fixed length, max args.
         * @return
         */
        public final int defaultSize() {
            return argsOffset + VMLog.Record.MAX_ARGS * Word.size();
        }

    }

    /**
     * Used to communicate with {@link VMLogger} in PRIMORDIAL phase, so allocated at BOOTSTRAPPING.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected static NativeRecord primordialNativeRecord;

    /**
     * Default size of a native log record in bytes.
     */
    @CONSTANT_WHEN_NOT_ZERO
    @INSPECTED
    protected int defaultNativeRecordSize;

    /**
     * Offset to start of arguments area.
     */
    @CONSTANT_WHEN_NOT_ZERO
    @INSPECTED
    private int nativeRecordArgsOffset;

    /**
     * Since we must log before it is even possible to call any native functions,
     * this byte array provides s pre-allocated, non-moving area.
     */
    @CONSTANT_WHEN_NOT_ZERO
    private byte[] primordialLogBufferArray;

    @CONSTANT_WHEN_NOT_ZERO
    private static Offset byteDataOffset;

    /**
     * Size of primordial logBuffer and subsequently allocated log buffers.
     */
    @INSPECTED
    protected int logSize;

    /**
     * Address of the actual data area in {@link #primordialLogBufferArray}.
     */
    @INSPECTED
    protected Address logBuffer;

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            byteDataOffset = VMConfiguration.vmConfig().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0);
            nativeRecordArgsOffset = getArgsOffset();
            primordialNativeRecord = new NativeRecord(nativeRecordArgsOffset);
            defaultNativeRecordSize = primordialNativeRecord.defaultSize();
            logSize = getLogSize();
            primordialLogBufferArray = new byte[logSize];
        } else if (phase == MaxineVM.Phase.PRIMORDIAL) {
            logBuffer = Reference.fromJava(primordialLogBufferArray).toOrigin().plus(byteDataOffset);
            VMLOG_RECORD.store3(Reference.fromJava(primordialNativeRecord));
        }
    }

    @NEVER_INLINE
    private Reference allocateNativeRecord() {
        // We disable logging for this allocation because we cannot log until
        // it is set and we could be logging heap allocation
        boolean oldState = setThreadState(false);
        Reference nativeRecordRef = Reference.fromJava(new NativeRecord(getArgsOffset()));
        setThreadState(oldState);
        VMLOG_RECORD.store3(nativeRecordRef);
        return nativeRecordRef;
    }

    @INLINE
    protected final NativeRecord getNativeRecord() {
        Reference nativeRecordRef = VMLOG_RECORD.loadRef(VmThread.currentTLA());
        if (nativeRecordRef.isZero()) {
            nativeRecordRef = allocateNativeRecord();
        }
        NativeRecord record = asNativeRecord(nativeRecordRef.toJava());
        return record;
    }

    /**
     * The offset (in bytes) to the start of the arguments in the native record.
     * @return
     */
    protected abstract int getArgsOffset();

    /**
     * The total size in bytes of a log buffer.
     * @return
     */
    protected abstract int getLogSize();


}

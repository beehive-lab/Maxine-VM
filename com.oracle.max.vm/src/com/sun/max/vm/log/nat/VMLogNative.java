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
import com.sun.max.vm.log.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * This implementation uses a native buffer and the "records" are stored
 * as a flattened array. As per {@link VMLogArray} the records are all fixed
 * size and capable of storing the maximum number of arguments. They are, however,
 * not real objects, which requires some subterfuge in the case where a client,
 * e.g, {@VMLogger} wants to see a real object. We handle this with a
 * {@link VmThreadLocal} that holds a {@link NativeRecord subclass} of {@link VMLog.Record}
 * containing the native record address which overrides all the methods
 * that access the state of the record. Evidently this record must be "used"
 * promptly and not cached as the value of the underlying state will change
 * every time {@link VMLog#getRecord} is called.
 */
public abstract class VMLogNative extends VMLog {

    private static final VmThreadLocal VMLOG_RECORD = new VmThreadLocal("VMLOG_RECORD", true, "VMLog.Record");

    @INTRINSIC(UNSAFE_CAST) private static native NativeRecord asNativeRecord(Object object);

    public static class NativeRecord extends VMLog.Record {
        public Pointer address;
        public int argsOffset;

        protected NativeRecord() {
        }

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

        protected void setArgsOffset(int argsOffset) {
            this.argsOffset = argsOffset;
        }

        public int size() {
            return argsOffset + VMLog.Record.MAX_ARGS * Word.size();
        }

    }

    protected static final NativeRecord primordialNativeRecord = new NativeRecord();

    @INSPECTED
    protected int nativeRecordSize;

    @INSPECTED
    protected int nativeRecordArgsOffset;

    @Override
    public void initialize() {
        super.initialize();
        nativeRecordArgsOffset = getArgsOffset();
        primordialNativeRecord.setArgsOffset(nativeRecordArgsOffset);
        nativeRecordSize = primordialNativeRecord.size();
        // make sure we have a NativeRecord for the primordial phase
        VMLOG_RECORD.store3(Reference.fromJava(primordialNativeRecord));
    }

    @NEVER_INLINE
    private Reference allocateNativeRecord() {
        Reference nativeRecordRef = Reference.fromJava(new NativeRecord(getArgsOffset()));
        VMLOG_RECORD.store3(nativeRecordRef);
        return nativeRecordRef;
    }

    @INLINE
    protected final NativeRecord getNativeRecord() {
        Reference nativeRecordRef = VMLOG_RECORD.loadRef(VmThread.currentTLA());
        if (nativeRecordRef.isZero()) {
            nativeRecordRef = allocateNativeRecord();
            VMLOG_RECORD.store3(nativeRecordRef);
        }
        NativeRecord record = asNativeRecord(nativeRecordRef.toJava());
        return record;
    }

    protected abstract int getArgsOffset();

}

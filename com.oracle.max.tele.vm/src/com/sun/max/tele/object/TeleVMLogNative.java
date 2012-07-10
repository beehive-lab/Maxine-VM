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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.nat.*;
import com.sun.max.vm.log.nat.thread.*;
import com.sun.max.vm.reference.*;

/**
 * Access to native implementations the VM's {@linkplain VMLog log}.
 *
 * @see VMLogNative
 */
public final class TeleVMLogNative extends TeleVMLog {

    private int defaultNativeRecordSize = 0;
    private int nativeRecordArgsOffset = 0;

    protected TeleVMLogNative(TeleVM vm, Reference vmLogNativeReference) {
        super(vm, vmLogNativeReference);
    }

    public int defaultNativeRecordSize() {
        if (defaultNativeRecordSize == 0) {
            defaultNativeRecordSize = fields().VMLogNative_defaultNativeRecordSize.readInt(reference());
        }
        return defaultNativeRecordSize;
    }

    public int nativeRecordSize(Address address) {
        final int argCount = Record.getArgCount(memory().readInt(address));
        return VMLogNativeThread.ARGS_OFFSET + argCount * vm().platform().nBytesInWord();
    }

    public int logSize() {
        return fields().VMLogNative_logSize.readInt(reference());
    }

    public TeleHostedLogRecord getLogRecord(Address recordAddress, int id) {
        if (recordAddress.isZero()) {
            return new TeleHostedLogRecord();
        }
        int header = memory().readInt(recordAddress);
        int argCount = Record.getArgCount(header);
        Word[] args = new Word[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = memory().getWord(recordAddress, nativeRecordArgsOffset(), i);
        }
        return new TeleHostedLogRecord(id, header, args);
    }

    /**
     * Offset to start of arguments area.
     */
    private int nativeRecordArgsOffset() {
        if (nativeRecordArgsOffset == 0) {
            nativeRecordArgsOffset = fields().VMLogNative_nativeRecordArgsOffset.readInt(reference());
        }
        return nativeRecordArgsOffset;
    }
}

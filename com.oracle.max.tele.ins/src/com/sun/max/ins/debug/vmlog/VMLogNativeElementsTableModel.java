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
package com.sun.max.ins.debug.vmlog;

import com.sun.max.ins.*;
import com.sun.max.tele.data.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.VMLog.Record;


abstract class VMLogNativeElementsTableModel extends VMLogElementsTableModel {
    protected VmMemoryIO vmIO;

    protected VMLogNativeElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection, vmLogView);
        vmIO = vm.memoryIO();
    }

    private int nativeRecordSize;
    /**
     * Get the size of the record at {@code r}.
     * Default implementation is fixed length.
     * @return
     */
    protected int nativeRecordSize(Pointer r) {
        if (nativeRecordSize == 0) {
            nativeRecordSize = vm.fields().VMLogNative_maxNativeRecordSize.readInt(vmLogView.vmLogRef);
        }
        return nativeRecordSize;
    }

    protected int nativeRecordSize() {
        return nativeRecordSize(Pointer.zero());
    }

    private int nativeRecordArgsOffset;
    protected int nativeRecordArgsOffset() {
        if (nativeRecordArgsOffset == 0) {
            nativeRecordArgsOffset = vm.fields().VMLogNative_nativeRecordArgsOffset.readInt(vmLogView.vmLogRef);
        }
        return nativeRecordArgsOffset;
    }

    @Override
    protected HostedLogRecord getRecordFromVM(int id) {
        // native buffer, access directly
        Pointer recordAddress = getRecordAddress(id);
        if (recordAddress.isZero()) {
            return new HostedLogRecord();
        } else {
            int header = vmIO.readInt(recordAddress);
            int argCount = Record.getArgCount(header);
            Word[] args = new Word[argCount];
            for (int i = 0; i < argCount; i++) {
                args[i] = vmIO.getWord(recordAddress, nativeRecordArgsOffset(), i);
            }
            return new HostedLogRecord(id, header, args);
        }
    }

    protected abstract Pointer getRecordAddress(long id);

}


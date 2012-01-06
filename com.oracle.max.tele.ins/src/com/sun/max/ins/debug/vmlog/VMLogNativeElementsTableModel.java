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
import com.sun.max.ins.debug.vmlog.VMLogView.*;
import com.sun.max.tele.data.*;
import com.sun.max.unsafe.*;


abstract class VMLogNativeElementsTableModel extends VMLogElementsTableModel {
    protected VmMemoryAccess vma;

    protected VMLogNativeElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection, vmLogView);
        vma = vm.memory();
    }

    private int nativeRecordSize;
    /**
     * Get the size of the record at {@code r}.
     * Default implementation is fixed length.
     * @return
     */
    protected int nativeRecordSize(Pointer r) {
        if (nativeRecordSize == 0) {
            nativeRecordSize = vm.fields().VMLogNative_nativeRecordSize.readInt(vmLogView.vmLogRef);
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
            return new HostedLogRecord(
                            id,
                            vma.readInt(recordAddress),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 0),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 1),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 2),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 3),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 4),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 5),
                            vma.getWord(recordAddress, nativeRecordArgsOffset(), 6));
        }
    }

    protected abstract Pointer getRecordAddress(long id);

}


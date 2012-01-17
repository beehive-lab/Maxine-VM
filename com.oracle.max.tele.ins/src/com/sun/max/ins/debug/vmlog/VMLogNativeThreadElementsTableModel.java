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

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.debug.vmlog.VMLogView.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.log.nat.thread.*;


class VMLogNativeThreadElementsTableModel extends VMLogNativeElementsTableModel {

    private static class NativeThreadRecordAccess implements Comparable<NativeThreadRecordAccess> {
        MaxThreadVMLog vmLog;
        Pointer recordAddress = Pointer.zero();
        int id;

        NativeThreadRecordAccess(MaxThreadVMLog vmLog, Pointer recordAddress, int id) {
            this.vmLog = vmLog;
            this.recordAddress = recordAddress;
            this.id = id;
        }

        public int compareTo(NativeThreadRecordAccess other) {
            if (id < other.id) {
                return -1;
            } else if (id > other.id) {
                return 1;
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return vmLog.thread().entityName() + ":" + id + ":" + Long.toHexString(recordAddress.toLong());
        }
    }


    protected NativeThreadRecordAccess[] sharedModel;
    protected int vmLogBufferNextIdIndex;

    protected VMLogNativeThreadElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection, vmLogView);
    }

    @Override
    protected void modelSpecificRefresh() {
        // we count the actual number of available records, which
        // could considerably exceed logEntries, depending on the
        // number of active threads.
        // N.B. At the end there is no guarantee that the gathered ids are contiguous.
        // TODO reimplement to avoid such prodigious storage allocation by sharing between refreshes
        ArrayList<NativeThreadRecordAccess> sharedModelList = new ArrayList<NativeThreadRecordAccess>();

        // look at every thread's buffer
        for (MaxThread thread : vm.state().threads()) {
            MaxThreadVMLog vmLog = thread.vmLog();
            if (vmLog.memoryRegion() == null) {
                continue;
            }
            Pointer logBuffer = vmLog.start().asPointer();
            int size = vmLog.size();
            Pointer logBufferEnd = logBuffer.plus(size);

            int offset = vmLog.firstOffset();
            int nextOffset = vmLog.nextOffset();
            do {
                Pointer recordAddress = logBuffer.plus(offset);
                assert recordAddress.lessThan(logBufferEnd);
                int header = vmIO.readInt(recordAddress);
                // variable length records can cause holes
                if (!Record.isFree(header)) {
                    int sharedId = vmIO.readInt(recordAddress, VMLogNativeThread.ID_OFFSET);
                    NativeThreadRecordAccess nativeThreadRecordAccess = new NativeThreadRecordAccess(vmLog, recordAddress, sharedId);
                    sharedModelList.add(nativeThreadRecordAccess);
                }
                offset = (offset + nativeRecordSize(recordAddress)) % size;
            } while (offset != nextOffset);
        }

        sharedModel = new NativeThreadRecordAccess[sharedModelList.size()];
        sharedModelList.toArray(sharedModel);
        Arrays.sort(sharedModel);
        if (sharedModel.length > logRecordCache.length) {
            logRecordCache = new HostedLogRecord[sharedModel.length];
        }
        actualLogBufferEntries = sharedModel.length;
    }

    /**
     * Poor man's iterator; depends on the implementation of {@link #refresh}.
     */
    private int stepIndex = 0;

    @Override
    protected int firstId() {
        stepIndex = 0;
        return sharedModel[0].id;
    }

    @Override
    protected int stepId(int id) {
        assert sharedModel[stepIndex].id == id;
        // if we ever had unused slots at the end of sharedModel this would need to be altered.
        if (++stepIndex >= sharedModel.length) {
            return nextId;
        }
        return sharedModel[stepIndex].id;
    }

    @Override
    protected Pointer getRecordAddress(long id) {
        assert sharedModel[stepIndex].id == id;
        return sharedModel[stepIndex].recordAddress;
    }

}


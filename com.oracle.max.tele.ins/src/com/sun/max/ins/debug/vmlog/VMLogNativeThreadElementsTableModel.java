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
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.VMLog.*;
import com.sun.max.vm.log.nat.thread.*;


/**
 * Model corresponds to subclasses of {@link VMLogNativeThread}.
 * Fixed/Variable length records stored in a per thread native circular buffer.
 * The main task in this code is to recreate the illusion of a shared, global,
 * log buffer, with monotonically increasing ids.
 */
class VMLogNativeThreadElementsTableModel extends VMLogNativeElementsTableModel {

    protected int vmLogBufferNextIdIndex;
    /**
     * This records the log records for all threads, including those that have died.
     */
    protected final HashMap<MaxThreadVMLog, ArrayList<HostedLogRecord>> threadVMLogs = new HashMap<MaxThreadVMLog, ArrayList<HostedLogRecord>>();

    private HostedLogRecord[] sortedCache;

    private Pointer recordAddress;

    protected VMLogNativeThreadElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection, vmLogView);
    }

    @Override
    protected void modelSpecificRefresh() {
        // look at every active thread's buffer for new records
        for (MaxThread thread : vm.state().threads()) {
            MaxThreadVMLog vmLog = thread.vmLog();
            if (vmLog.memoryRegion() == null) {
                continue;
            }
            ArrayList<HostedLogRecord> threadLogRecordCache = threadVMLogs.get(vmLog);
            if (threadLogRecordCache ==  null) {
                threadLogRecordCache = new ArrayList<HostedLogRecord>(vmLogView.logBufferEntries);
                threadVMLogs.put(vmLog, threadLogRecordCache);
            }

            int threadLastId = threadLogRecordCache.size() == 0 ? -1 : threadLogRecordCache.get(threadLogRecordCache.size() - 1).getId();

            Pointer logBuffer = vmLog.start().asPointer();
            int size = vmLog.size();
            Pointer logBufferEnd = logBuffer.plus(size);

            int offset = vmLog.firstOffset();
            int nextOffset = vmLog.nextOffset();
            do {
                recordAddress = logBuffer.plus(offset);
                assert recordAddress.lessThan(logBufferEnd);
                int header = vmIO.readInt(recordAddress);
                // variable length records can cause holes
                if (!Record.isFree(header)) {
                    int sharedId = vmIO.readInt(recordAddress, VMLogNativeThread.ID_OFFSET);
                    if (sharedId > threadLastId) {
                        threadLogRecordCache.add(getRecordFromVM(sharedId));
                    }
                }
                offset = (offset + nativeRecordSize(recordAddress)) % size;
            } while (offset != nextOffset);
        }

        int sortedCacheSize = 0;
        for (ArrayList<HostedLogRecord> threadLogRecordCache : threadVMLogs.values()) {
            sortedCacheSize += threadLogRecordCache.size();
        }
        sortedCache = new HostedLogRecord[sortedCacheSize];
        sortedCacheSize = 0;
        for (ArrayList<HostedLogRecord> threadLogRecordCache : threadVMLogs.values()) {
            for (HostedLogRecord r : threadLogRecordCache) {
                sortedCache[sortedCacheSize++] = r;
            }
        }
        Arrays.sort(sortedCache);
        recordAddress = Pointer.zero();
        logRecordCache = Arrays.asList(sortedCache);
    }

    @Override
    protected Pointer getRecordAddress(long id) {
        return recordAddress;
    }

}


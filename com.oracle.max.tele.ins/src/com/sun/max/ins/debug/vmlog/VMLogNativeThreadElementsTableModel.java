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
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
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

    /**
     * This records the log records for all threads, including those that have died.
     */
    protected final HashMap<MaxThreadVMLog, ArrayList<TeleHostedLogRecord>> threadVMLogs = new HashMap<MaxThreadVMLog, ArrayList<TeleHostedLogRecord>>();

    private TeleHostedLogRecord[] sortedCache;

    private Pointer recordAddress;

    protected VMLogNativeThreadElementsTableModel(Inspection inspection, TeleVMLog teleVMLog) {
        super(inspection, teleVMLog);
    }

    @Override
    protected void modelSpecificRefresh() {
        // look at every active thread's buffer for new records
        for (MaxThread thread : vm().state().threads()) {
            MaxThreadVMLog threadVmLog = thread.vmLog();
            if (threadVmLog.memoryRegion() == null) {
                continue;
            }
            ArrayList<TeleHostedLogRecord> threadLogRecordCache = threadVMLogs.get(threadVmLog);
            if (threadLogRecordCache ==  null) {
                threadLogRecordCache = new ArrayList<TeleHostedLogRecord>(teleVMLogNative.logEntries());
                threadVMLogs.put(threadVmLog, threadLogRecordCache);
            }

            int threadLastId = threadLogRecordCache.size() == 0 ? -1 : threadLogRecordCache.get(threadLogRecordCache.size() - 1).getId();

            Pointer logBuffer = threadVmLog.start().asPointer();
            int size = threadVmLog.size();
            Pointer logBufferEnd = logBuffer.plus(size);

            int offset = threadVmLog.firstOffset();
            int nextOffset = threadVmLog.nextOffset();
            do {
                recordAddress = logBuffer.plus(offset);
                assert recordAddress.lessThan(logBufferEnd);
                int header = vm().memoryIO().readInt(recordAddress);
                // variable length records can cause holes
                if (!Record.isFree(header)) {
                    int sharedId = vm().memoryIO().readInt(recordAddress, VMLogNativeThread.ID_OFFSET);
                    if (sharedId > threadLastId) {
                        threadLogRecordCache.add(getRecordFromVM(sharedId));
                    }
                }
                offset = (offset + nativeRecordSize(recordAddress)) % size;
            } while (offset != nextOffset);
        }

        int sortedCacheSize = 0;
        for (ArrayList<TeleHostedLogRecord> threadLogRecordCache : threadVMLogs.values()) {
            sortedCacheSize += threadLogRecordCache.size();
        }
        sortedCache = new TeleHostedLogRecord[sortedCacheSize];
        sortedCacheSize = 0;
        for (ArrayList<TeleHostedLogRecord> threadLogRecordCache : threadVMLogs.values()) {
            for (TeleHostedLogRecord r : threadLogRecordCache) {
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


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
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.log.*;


abstract class VMLogElementsTableModel extends InspectorTableModel {

    protected TeleVM vm;

    /**
     * Cache of the (logical) circular buffer of log records in the target VM.
     * Logical in the sense that per-thread log buffers are reconstituted into
     * a single, ordered, log in the Inspector.
     *
     * The length may change in the case of per-thread buffers
     * with variable length records, {@link VMLogNativeThreadVariableElementsTableModel},
     * and will always be at least {@link #actualLogBufferEntries}.
     *
     * Only the elements in the range{@code 0 .. actualLogBufferEntries - 1} are valid.
     *
     */
    HostedLogRecord[] logRecordCache;

    /**
     * Usually equal to {@link #logBufferEntries} but, in the case of per-thread buffers
     * and/or variable length records, {@link VMLogNativeThreadVariableElementsTableModel},
     * the values may differ.
     */
    protected int actualLogBufferEntries;
    /**
     * During {@link #refresh}, this holds the new value of the {@link VMLog#nextId} field,
     * which is the id of the next record that will be written.
     */
    protected int nextId;

    /**
     * After {@link #refresh}, this is set equal to {@link #nextId}, allowing
     * optimization on a subsequent refresh where nothing changed.
     */
    protected int lastNextId;

    protected VMLogView vmLogView;

    protected VMLogElementsTableModel(Inspection inspection, VMLogView vmLogView) {
        super(inspection);
        vm = (TeleVM) vm();
        this.vmLogView = vmLogView;
        logRecordCache = new HostedLogRecord[vmLogView.logBufferEntries];
    }

    public int getColumnCount() {
        return VMLogColumnKind.values().length;
    }

    /**
     * The number of log records in the view.
     * @return
     */
    public int getRowCount() {
        return actualLogBufferEntries;
    }

    /**
     * Get the value of the slot in the log buffer at the given logical row and column.
     */
    public Object getValueAt(int row, int col) {
        HostedLogRecord record = logRecordCache[row];
        if (record == null) {
            TeleError.unexpected("null log record in LogElementsTableModel.getValueAt");
        }
        Object result = null;
        int argCount = VMLog.Record.getArgCount(record.header);

        switch (VMLogColumnKind.values()[col]) {
            case ID:
                result = record.id;
                break;
            case THREAD:
                result = VMLog.Record.getThreadId(record.header);
                break;
            case OPERATION:
                result = VMLog.Record.getOperation(record.header);
                break;
            case ARG1:
                if (argCount > 0) {
                    result = record.args[0];
                }
                break;
            case ARG2:
                if (argCount > 1) {
                    result = record.args[1];
                }
                break;
            case ARG3:
                if (argCount > 2) {
                    result = record.args[2];
                }
                break;
            case ARG4:
                if (argCount > 3) {
                    result = record.args[3];
                }
                break;
            case ARG5:
                if (argCount > 4) {
                    result = record.args[4];
                }
                break;
            case ARG6:
                if (argCount > 5) {
                    result = record.args[5];
                }
                break;
            case ARG7:
                if (argCount > 6) {
                    result = record.args[6];
                }
                break;
            default:
                TeleError.unexpected("illegal column value kind");
                result = null;
        }
        return result;
    }

    @Override
    public void refresh() {
        nextId = vmLogView.nextIdFieldAccess.readInt(vmLogView.vmLogRef);
        if (nextId != lastNextId) {
            // Some new records; we could try to be clever and just figure out what changed
            // but for now we just read everything into the record cache (underlying page caching helps).
            // This also makes it easy to keep the record cache logical in the sense that index 0 is the first
            // slot in the circular buffer and not index 0 in (any) target VM log array.
            // The maximum possible number of records is nextId - lastNextId,
            // as that is the total number allocated since the last refresh.
            // However, depending on the target implementation, it is entirely possible
            // that enough change has occurred that some records were overwritten in the target.

            modelSpecificRefresh();

            int id = firstId();
            int cacheIndex = 0;

            while (id < nextId) {
                logRecordCache[cacheIndex++] = getRecordFromVM(id);
                id = stepId(id);
            }
            lastNextId = nextId;
        } else {
            // one very special case - the last refresh was after nextId was bumped
            // but before the record was filled in.
            if (lastNextId != 0 && logRecordCache[actualLogBufferEntries - 1].header == 0) {
                int id = lastNextId - 1;
                // re-read the record
                HostedLogRecord record = getRecordFromVM(id);
                logRecordCache[actualLogBufferEntries - 1] = record;
            }
        }
        super.refresh();
    }

    /**
     * Is this header value well-formed?
     * @param header
     * @return
     */
    boolean wellFormedHeader(int header) {
        // there are brief periods when a record may not be well formed,
        // e.g., we have stopped in the Inspector after the log buffer id has been bumped
        // but before the data has been filled in.
        // specifically, the logger id may be bogus, which will cause a crash.
        if (VMLog.Record.isFree(header)) {
            return false;
        }
        int loggerId = VMLog.Record.getLoggerId(header);
        if (vmLogView.loggers.get(loggerId) == null) {
            return false;
        }
        return true;
    }


    /**
     * Responsible for setting the value of {@link #actualLogBufferEntries}.
     * Plus do any model-specific refresh before the main refresh happens.
     * E.g., collecting together the thread-specific records in {@link NativeThreadFixedLogElementsTableModel per-thread buffer model}.
     * Default assume shared buffer with fixed size records.
     */
    protected void modelSpecificRefresh() {
        actualLogBufferEntries = nextId > vmLogView.logBufferEntries ? vmLogView.logBufferEntries : nextId;
    }

    /**
     * Return the first id available in this model.
     * Default assumes contiguous id range, i.e. shared buffer.
     * @return
     */
    protected int firstId() {
        return nextId > actualLogBufferEntries ? nextId - actualLogBufferEntries : 0;
    }

    /**
     * Step to the next id in this model.
     * Default assumes contiguous id range, i.e. shared buffer.
     * @param id
     * @return
     */
    protected int stepId(int id) {
        return id + 1;
    }

    /**
     * Create a {@link HostedLogRecord} from the target VM record.
     * @param id the id of the record (B.B. may not be stored in target). {@code nextId - logRecordCache.length <= id < nextId}.
     * @return
     */
    protected abstract HostedLogRecord getRecordFromVM(int id);

}


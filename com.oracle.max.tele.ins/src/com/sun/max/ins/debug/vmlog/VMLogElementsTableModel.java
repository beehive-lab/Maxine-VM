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

import java.awt.*;
import java.util.*;
import java.util.List;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.hosted.VMLogHosted.HostedLogRecord;


abstract class VMLogElementsTableModel extends InspectorTableModel {

    private class ColumnRenderers {
        Component[] renderers = new Component[VMLogColumnKind.values().length];
    }

    /**
     * Cache of the (logical) buffer of log records in the target VM.
     * Logical in the sense that per-thread log buffers are reconstituted into
     * a single, ordered, log in the Inspector.
     *
     * Although most VM implementations use a circular buffer and eventually
     * overwrite records we keep them all. (At some point may add a capability
     * to flush old records).
     *
     * Note that overwritten records may not be seen if the time between entries to the
     * Inspector is sufficiently long that the circular buffer wraps.
     * This could be addressed by a hidden breakpoint that was triggered
     * appropriately.
     *
     */
    protected List<TeleHostedLogRecord> logRecordCache;

    /**
     * During {@link #refresh}, this holds the new value of the {@link VMLog#nextId} field,
     * which is the id of the next record that will be written.
     */
    private int nextId;

    /**
     * After {@link #refresh}, this is set equal to {@link #nextId}, allowing
     * optimization on a subsequent refresh where nothing changed.
     */
    private int lastNextId;

    private final TeleVMLog teleVMLog;

    private int[] displayedRows;

    private ArrayList<ColumnRenderers> tableRenderers;

    protected VMLogElementsTableModel(Inspection inspection, TeleVMLog teleVMLog) {
        super(inspection);
        this.teleVMLog = teleVMLog;
        logRecordCache = new ArrayList<TeleHostedLogRecord>(teleVMLog.logEntries());
        tableRenderers = new ArrayList<ColumnRenderers>(teleVMLog.logEntries());
    }

    public int getColumnCount() {
        return VMLogColumnKind.values().length;
    }

    /**
     * The number of log records in the view.
     */
    public int getRowCount() {
        return displayedRows == null ? logRecordCache.size() : displayedRows.length;
    }

    public void setDisplayedRows(int[] displayedRows) {
        this.displayedRows = displayedRows;
        this.fireTableDataChanged();
    }

    private int displayed2ModelRow(int displayedRow) {
        return displayedRows == null ? displayedRow : displayedRows[displayedRow];
    }

    TeleHostedLogRecord getRecord(int row) {
        return logRecordCache.get(displayed2ModelRow(row));
    }

    private ColumnRenderers getColumnRenderers(int row) {
        row = displayed2ModelRow(row);
        int size = tableRenderers.size();
        if (row >= size) {
            for (int r = size; r <= row; r++) {
                tableRenderers.add(new ColumnRenderers());
            }
        }
        return tableRenderers.get(row);
    }

    Component getRenderer(int row, int column) {
        ColumnRenderers cr = getColumnRenderers(row);
        return cr.renderers[column];
    }

    void setRenderer(int row, int column, Component renderer) {
        ColumnRenderers cr = getColumnRenderers(row);
        cr.renderers[column] = renderer;
    }

    /**
     * Get the value of the slot in the log buffer at the given logical row and column.
     */
    public Object getValueAt(int row, int col) {
        TeleHostedLogRecord record = getRecord(row);
        if (record == null) {
            TeleError.unexpected("null log record in LogElementsTableModel.getValueAt");
        }
        Object result = null;
        int argCount = VMLog.Record.getArgCount(record.getHeader());

        switch (VMLogColumnKind.values()[col]) {
            case ID:
                result = record.getId();
                break;
            case THREAD:
                result = VMLog.Record.getThreadId(record.getHeader());
                break;
            case OPERATION:
                result = VMLog.Record.getOperation(record.getHeader());
                break;
            default:
                // arguments
                final int argNum = col2argNum(col);
                if (argNum <= argCount) {
                    result = record.getArg(argNum);
                }
        }
        return result;
    }

    private int col2argNum(int col) {
        return col - VMLogColumnKind.ARG1.ordinal() + 1;
    }

    private int argNum2col(int argNum) {
        return argNum + VMLogColumnKind.ARG1.ordinal() - 1;
    }

    @Override
    public void refresh() {
        nextId = teleVMLog.nextID();
        if (nextId != lastNextId) {
            // Some new records.
            modelSpecificRefresh();

            lastNextId = nextId;
        }
        super.refresh();
    }

    /**
     * Is this header value well-formed?
     * @param header
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
        if (teleVMLog.getLogger(loggerId) == null) {
            return false;
        }
        return true;
    }


    /**
     * Responsible for any model-specific refresh before the main refresh happens.
     * E.g., collecting together the thread-specific records in {@link NativeThreadFixedLogElementsTableModel per-thread buffer model}.
     */
    protected void modelSpecificRefresh() {
        int id = firstId();

        while (id < nextId) {
            logRecordCache.add(getRecordFromVM(id));
            id++;
        }
    }

    /**
     * Return the first id to start gathering new records from.
     * Default assumes contiguous id range, i.e. global, shared, buffer, handling case
     * where some records have been overwritten.
     */
    protected int firstId() {
        if (nextId - lastNextId > teleVMLog.logEntries()) {
            // missed some records
            return nextId - teleVMLog.logEntries();
        } else {
            // pick up where we left off
            return lastNextId;
        }
    }

    /**
     * Create a {@link HostedLogRecord} from the target VM record.
     * @param id the id of the record (N.B. may not be stored in target).
     */
    protected abstract TeleHostedLogRecord getRecordFromVM(int id);

    /**
     * Refresh (create) the state from a list of records gathered offline.
     * @param records
     */
    protected abstract void offLineRefresh(ArrayList<String> records);

    /**
     * Refreshes the display of every renderer in a column displaying a specified
     * log argument number.
     *
     * @param argNum the log argument number whose column is to be refreshed
     * @param force whether the refresh should override any caching.
     */
    public void refreshColumnRenderers(int argNum, boolean force) {
        final int rowCount = getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final ColumnRenderers columnRenderers = getColumnRenderers(row);
            final Component component = columnRenderers.renderers[argNum2col(argNum)];
            if (component instanceof Prober) {
                final Prober prober = (Prober) component;
                prober.refresh(force);
            }
        }
    }

    /**
     * Forces a redisplay of every render in a column displaying a specified log argument number.
     *
     * @param argNum the log argument number whose column is to be redisplayed.
     */
    public void redisplayColumnRenderers(int argNum) {
        final int rowCount = getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final ColumnRenderers columnRenderers = getColumnRenderers(row);
            final Component component = columnRenderers.renderers[argNum2col(argNum)];
            if (component instanceof Prober) {
                final Prober prober = (Prober) component;
                prober.redisplay();
            }
        }
    }

    // Support for offline (file) log processing

    protected static Map<Integer, String> offLineThreadMap = new HashMap<Integer, String>();

    /**
     * Gets the name of a thread gathered by {@link #offLineRefresh(ArrayList)} from the id.
     */
    protected String offLineThreadName(int threadId) {
        return offLineThreadMap.get(threadId);
    }

    /**
     * Process embedded records defining thread name/ids.
     * @param records
     * @return array of {@code TeleHostedLogRecord}
     */
    protected TeleHostedLogRecord[] processThreadIds(ArrayList<String> records) {
        int count = 0;
        for (String record : records) {
            if (record.startsWith(VMLog.RawDumpFlusher.THREAD_MARKER)) {
                continue;
            }
            count++;
        }
        TeleHostedLogRecord[] result = new TeleHostedLogRecord[count];

        count = 0;
        for (String record : records) {
            if (record.startsWith(VMLog.RawDumpFlusher.THREAD_MARKER)) {
                // ...: name[id=N]
                int colonX = record.indexOf(':');
                int bracketX = record.indexOf('[');
                int eqX = record.lastIndexOf('=');
                String name = record.substring(colonX + 1, bracketX);
                int id = Integer.parseInt(record.substring(eqX + 1, record.length() - 1));
                offLineThreadMap.put(id, name);
                continue;
            }
            result[count++] = parseRecord(record);
        }
        return result;
    }

    protected TeleHostedLogRecord parseRecord(String record) {
        String[] parts = record.split(" ");
        int header = Integer.parseInt(parts[0]);
        int uuid = Integer.parseInt(parts[1]);
        int argc = Integer.parseInt(parts[2]);
        Word[] args = new Word[argc];
        for (int i = 0; i < argc; i++) {
            args[i] = parseWord(parts[i + 3]);
        }
        return new TeleHostedLogRecord(uuid, header, args);
    }


    /**
     * Parse a hex word value in a log records file.
     * @param s
     * @return
     */
    protected static Word parseWord(String s) {
        int ix = 0;
        if (s.startsWith("0x")) {
            ix = 2;
        }
        long value = 0;
        for (int i = ix; i < s.length(); i++) {
            char ch = s.charAt(i);
            int chv = 0;
            if ('a' <= ch && ch <= 'f') {
                chv = (ch - 'a') + 10;
            } else if ('0' <= ch && ch <= '9') {
                chv = ch - '0';
            } else {
                ProgramError.check(false, "malformed value in log entry");
            }
            value = (value << 4) | (chv & 0xf);
        }
        return Address.fromLong(value);
    }

}


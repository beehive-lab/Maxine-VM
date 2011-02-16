/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug.linux;

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.dump.*;

/**
 * Linux implementation of the channel protocol for accessing core dump files.
 *
 * @author Mick Jordan
 *
 */
public class LinuxDumpTeleChannelProtocol extends ELFDumpTeleChannelProtocolAdaptor implements LinuxTeleChannelProtocol {
    private List<TaskData> taskDataList = new ArrayList<TaskData>();
    private ByteBuffer taskPsInfo;
    private LinuxDumpThreadAccess linuxDumpThreadAccess;

    static class TaskData {
        ByteBuffer status;
        ByteBuffer fpreg;
        ByteBuffer psinfo; // copy of taskPsInfo
    }

    public LinuxDumpTeleChannelProtocol(TeleVM teleVM, File vm, File dump) {
        super(teleVM, vm, dump);
        LinuxNoteEntryHandler noteEntryHandler = new LinuxNoteEntryHandler();
        processNoteSection(noteEntryHandler);
    }

    class LinuxNoteEntryHandler extends NoteEntryHandler {
        private TaskData taskData;  // NT_PRSTATUS, NT_PRFPREG come in pairs, this holds the data across the callbacks

        @Override
        protected void processNoteEntry(int type, String name, byte[] desc) {
            final NoteType noteType = NoteType.get(type);
            switch (noteType) {
                case NT_PRSTATUS:
                    checkCreate();
                    final ByteBuffer status = ByteBuffer.allocateDirect(desc.length);
                    status.put(desc);
                    taskData.status = status;
                    if (taskData.fpreg != null) {
                        taskData = null;
                    }
                    break;

                case NT_PRFPREG:
                    checkCreate();
                    final ByteBuffer fpreg = ByteBuffer.allocateDirect(desc.length);
                    fpreg.put(desc);
                    taskData.fpreg = fpreg;
                    if (taskData.status != null) {
                        taskData = null;
                    }
                    break;

                case NT_PRPSINFO:
                    taskPsInfo = ByteBuffer.allocateDirect(desc.length);
                    taskPsInfo.put(desc);
            }
        }

        private void checkCreate() {
            if (taskData == null) {
                taskData = new TaskData();
                taskData.psinfo = taskPsInfo;
                taskDataList.add(taskData);
            }
        }



    }

    private static enum NoteType {
        NT_PRSTATUS(1),
        NT_PRFPREG(2),
        NT_PRPSINFO(3),
        NT_AUXV(6);

        int value;

        static NoteType get(int type) {
            for (NoteType noteType : values()) {
                if (noteType.value == type) {
                    return noteType;
                }
            }
            return null;
        }

        NoteType(int value) {
            this.value = value;
        }
    }

    @Override
    public boolean initialize(int tlaSize, boolean bigEndian) {
        super.initialize(tlaSize, bigEndian);
        linuxDumpThreadAccess = new LinuxDumpThreadAccess(this, tlaSize, taskDataList);
        return true;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        LinuxDumpThreadAccess.LinuxThreadInfo threadInfo = (LinuxDumpThreadAccess.LinuxThreadInfo) linuxDumpThreadAccess.getThreadInfo((int) threadId);
        System.arraycopy(threadInfo.integerRegisters, 0, integerRegisters, 0, integerRegisters.length);
        System.arraycopy(threadInfo.floatingPointRegisters, 0, floatingPointRegisters, 0, floatingPointRegisters.length);
        System.arraycopy(threadInfo.stateRegisters, 0, stateRegisters, 0, stateRegisters.length);
        return true;
    }

    @Override
    public boolean gatherThreads(Object teleDomainObject, Object threadList, long tlaList) {
        return linuxDumpThreadAccess.gatherThreads(teleDomainObject, threadList, tlaList);
    }
}

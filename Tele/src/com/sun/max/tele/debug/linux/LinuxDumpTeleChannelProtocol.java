/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
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

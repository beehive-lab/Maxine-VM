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

import java.nio.*;
import java.util.*;

import com.sun.max.tele.debug.dump.*;
import com.sun.max.tele.debug.linux.LinuxDumpTeleChannelProtocol.TaskData;

/**
 * @author Mick Jordan
 *
 */
public class LinuxDumpThreadAccess extends ThreadAccess {

    class LinuxThreadInfo extends ThreadAccess.ThreadInfoRegisterAdaptor {

        private TaskData taskData;

        LinuxThreadInfo(TaskData taskData) {
            this.taskData = taskData;
            taskRegisters(taskData.status, taskData.fpreg, integerRegisters, integerRegisters.length, floatingPointRegisters, floatingPointRegisters.length, stateRegisters, stateRegisters.length);
        }

        @Override
        public int getId() {
            return taskId(taskData.status);
        }

        @Override
        public int getThreadState() {
            // It seems that state is not available per thread
            return taskStatusToThreadState(taskData.psinfo);
        }

    }

    private List<TaskData> taskDataList;

    LinuxDumpThreadAccess(LinuxDumpTeleChannelProtocol protocol, int tlaSize, List<TaskData> taskDataList) {
        super(protocol, tlaSize);
        this.taskDataList = taskDataList;
    }

    @Override
    protected void gatherOSThreads(List<ThreadInfo> threadList) {
        for (TaskData taskData : taskDataList) {
            threadList.add(new LinuxThreadInfo(taskData));
        }
    }

    private static native int taskStatusToThreadState(ByteBuffer lwpstatus);

    private static native int taskId(ByteBuffer lwpstatus);

    private static native boolean taskRegisters(ByteBuffer status, ByteBuffer fpreg, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

}

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

import java.nio.*;
import java.util.*;

import com.sun.max.tele.debug.dump.*;
import com.sun.max.tele.debug.linux.LinuxDumpTeleChannelProtocol.TaskData;

/**
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
            assert taskData != null;
            assert taskData.psinfo != null;
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

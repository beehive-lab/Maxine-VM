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
package com.sun.max.tele.debug.darwin;

import java.nio.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.darwin.DarwinMachO.ThreadLoadCommand;
import com.sun.max.tele.debug.dump.*;


/**
 *
 */
public class DarwinDumpThreadAccess extends ThreadAccess {

    private static int idNext;

    class DarwinThreadInfo extends ThreadAccess.ThreadInfoRegisterAdaptor {
        private ThreadLoadCommand threadData;
        private int id;

        DarwinThreadInfo(ThreadLoadCommand threadData) {
            this.threadData = threadData;
            this.id = idNext++;
            threadRegisters(threadData.regstate.regbytes, threadData.fpregstate.regbytes, integerRegisters, integerRegisters.length, floatingPointRegisters, floatingPointRegisters.length, stateRegisters, stateRegisters.length);

        }

        @Override
        public int getId() {
            // there is no id field or equivalent in the LC_THREAD command info
            // so we just use a simple unique value
            return id;
        }

        @Override
        public int getThreadState() {
            // there also does not seem to be any info on the state of the thread
            return MaxThreadState.SUSPENDED.ordinal();
        }
    }

    private List<ThreadLoadCommand> threadDataList;

    DarwinDumpThreadAccess(DarwinDumpTeleChannelProtocol protocol, int tlaSize, List<ThreadLoadCommand> threadDataList) {
        super(protocol, tlaSize);
        this.threadDataList = threadDataList;
    }

    @Override
    protected void gatherOSThreads(List<ThreadInfo> threadList) {
        for (ThreadLoadCommand threadData : threadDataList) {
            threadList.add(new DarwinThreadInfo(threadData));
        }

    }

    private static native boolean threadRegisters(ByteBuffer gregs, ByteBuffer fpregs, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

}

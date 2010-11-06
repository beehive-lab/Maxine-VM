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
package com.sun.max.tele.debug.darwin;

import java.nio.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.darwin.DarwinMachO.ThreadLoadCommand;
import com.sun.max.tele.debug.dump.*;


/**
 * @author Mick Jordan
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

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
package com.sun.max.tele.debug.solaris;

import java.nio.*;
import java.util.*;

import com.sun.max.tele.debug.dump.*;
import com.sun.max.tele.debug.solaris.SolarisDumpTeleChannelProtocol.LwpData;

/**
 * Solaris-specific implementation of {@link ThreadAccess}.
 * N.B. Since the relevant C data structures are so complex, we use native
 * code to access the critical fields, which means we need to load
 * the tele library (it isn't loaded by default for dump mode).
 *
 * @author Mick Jordan
 *
 */
public class SolarisDumpThreadAccess extends ThreadAccess {

    class SolarisThreadInfo extends ThreadAccess.ThreadInfoRegisterAdaptor {

        private LwpData lwpData;

        SolarisThreadInfo(LwpData lwpData) {
            this.lwpData = lwpData;
            lwpRegisters(lwpData.lwpStatus, integerRegisters, integerRegisters.length, floatingPointRegisters, floatingPointRegisters.length, stateRegisters, stateRegisters.length);
        }

        @Override
        public int getId() {
            return lwpId(lwpData.lwpStatus);
        }

        @Override
        public int getThreadState() {
            return lwpStatusToThreadState(lwpData.lwpStatus);
        }

    }

    private List<LwpData> lwpDataList;

    SolarisDumpThreadAccess(SolarisDumpTeleChannelProtocol protocol, int tlaSize, List<LwpData> lwpDataList) {
        super(protocol, tlaSize);
        this.lwpDataList = lwpDataList;
    }

    @Override
    protected void gatherOSThreads(List<ThreadInfo> threadList) {
        for (LwpData lwpData : lwpDataList) {
            threadList.add(new SolarisThreadInfo(lwpData));
        }
    }

    private static native int lwpStatusToThreadState(ByteBuffer lwpstatus);
    private static native int lwpId(ByteBuffer lwpstatus);
    private static native boolean lwpRegisters(ByteBuffer lwpstatus, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters,
                    int floatingPointRegistersSize, byte[] stateRegisters, int stateRegistersSize);
}

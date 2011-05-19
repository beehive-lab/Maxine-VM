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

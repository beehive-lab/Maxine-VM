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

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.dump.*;

/**
 * Solaris implementation of the channel protocol for accessing core dump files.
 *
 */
public class SolarisDumpTeleChannelProtocol extends ELFDumpTeleChannelProtocolAdaptor implements SolarisTeleChannelProtocol {

    private List<LwpData> lwpDataList = new ArrayList<LwpData>();
    private SolarisDumpThreadAccess solarisDumpThreadAccess;
    private ByteBuffer pStatus;

    static class LwpData {
        // direct buffers
        ByteBuffer lwpStatus;
        ByteBuffer lwpInfo;

        LwpData(ByteBuffer lwpInfo) {
            this.lwpInfo = lwpInfo;
        }
    }

    public SolarisDumpTeleChannelProtocol(MaxVM teleVM, File vm, File dumpFile) {
        super(teleVM, vm, dumpFile);
        SolarisNoteEntryHandler noteEntryHandler = new SolarisNoteEntryHandler();
        processNoteSection(noteEntryHandler);
    }

    @Override
    protected long getBootHeapStartSymbolAddress() {
        // On Solaris a randomly generated prefix is added to static symbols so,
        // if this code were invoked, it would need to do a search that coped with that.
        unimplemented("getBootHeapStartSymbolAddress");
        return 0;
    }

    class SolarisNoteEntryHandler extends NoteEntryHandler {
        LwpData lwpData;

        @Override
        protected void processNoteEntry(int type, String name, byte[] desc) {
            final NoteType noteType = NoteType.get(type);
            switch (noteType) {
                case NT_PSTATUS:
                    pStatus = ByteBuffer.allocateDirect(desc.length);
                    pStatus.put(desc);
                    @SuppressWarnings("unused")
                    final int numActiveLwps = numActiveLwps(pStatus);
                    @SuppressWarnings("unused")
                    final int numZombieLwps = numZombieLwps(pStatus);
                    break;

                case NT_LWPSINFO: {
                    // this comes before NT_LWPSTATUS
                    ByteBuffer lwpInfo = ByteBuffer.allocateDirect(desc.length);
                    lwpInfo.put(desc);
                    if (!isZombieLwp(lwpInfo)) {
                        lwpData = new LwpData(lwpInfo);
                        lwpDataList.add(lwpData);
                    }
                    break;
                }

                case NT_LWPSTATUS: {
                    ByteBuffer lwpStatus = ByteBuffer.allocateDirect(desc.length);
                    lwpStatus.put(desc);
                    lwpData.lwpStatus = lwpStatus;
                    lwpData = null;
                    break;
                }
            }
        }
    }

    private static native int numActiveLwps(ByteBuffer pStatus);
    private static native int numZombieLwps(ByteBuffer pStatus);
    private static native boolean isZombieLwp(ByteBuffer lwpInfo);

    private int readInt(byte[] data, int index) {
        final ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getInt(index);
    }

    private static enum NoteType {
        NT_PRSTATUS(1),
        NT_PRFPREG(2),
        NT_PRPSINFO(3),
        NT_PRXREG(4),
        NT_PLATFORM(5),
        NT_AUXV(6),
        NT_GWINDOWS(7),
        NT_ASRS(8),
        NT_LDT(9),
        NT_PSTATUS(10),
        NT_PSINFO(13),
        NT_PRCRED(14),
        NT_UTSNAME(15),
        NT_LWPSTATUS(16),
        NT_LWPSINFO(17),
        NT_PRPRIV(18),
        NT_PRPRIVINFO(19),
        NT_CONTENT(20),
        NT_ZONENAME(21);

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
        solarisDumpThreadAccess = new SolarisDumpThreadAccess(this, tlaSize, lwpDataList);
        return true;
    }

    @Override
    public boolean readRegisters(long threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        SolarisDumpThreadAccess.SolarisThreadInfo threadInfo = (SolarisDumpThreadAccess.SolarisThreadInfo) solarisDumpThreadAccess.getThreadInfo((int) threadId);
        System.arraycopy(threadInfo.integerRegisters, 0, integerRegisters, 0, integerRegisters.length);
        System.arraycopy(threadInfo.floatingPointRegisters, 0, floatingPointRegisters, 0, floatingPointRegisters.length);
        System.arraycopy(threadInfo.stateRegisters, 0, stateRegisters, 0, stateRegisters.length);
        return true;
    }

    @Override
    public boolean gatherThreads(Object teleProcessObject, Object threadList, long tlaList) {
        return solarisDumpThreadAccess.gatherThreads(teleProcessObject, threadList, tlaList);
    }

}


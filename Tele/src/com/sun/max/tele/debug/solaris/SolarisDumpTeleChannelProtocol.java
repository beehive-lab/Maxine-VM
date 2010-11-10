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

import java.io.*;
import java.nio.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.debug.dump.*;

/**
 * Solaris implementation of the channel protocol for accessing core dump files.
 *
 * @author Mick Jordan
 *
 */
public class SolarisDumpTeleChannelProtocol extends ELFDumpTeleChannelProtocolAdaptor implements SolarisTeleChannelProtocol {

    private List<LwpData> lwpDataList;
    private SolarisDumpThreadAccess solarisDumpThreadAccess;

    static class LwpData {
        // direct buffers
        ByteBuffer lwpStatus;
        ByteBuffer lwpInfo;

        LwpData(ByteBuffer lwpInfo) {
            this.lwpInfo = lwpInfo;
        }
    }

    public SolarisDumpTeleChannelProtocol(TeleVM teleVM, File vm, File dumpFile) {
        super(teleVM, vm, dumpFile);
        SolarisNoteEntryHandler noteEntryHandler = new SolarisNoteEntryHandler();
        processNoteSection(noteEntryHandler);
        lwpDataList = Arrays.asList(noteEntryHandler.lwpDataArray);
    }

    @Override
    protected long getBootHeapStartSymbolAddress() {
        // On Solaris a randomly generated prefix is added to static symbols so,
        // if this code were invoked, it would need to do a search that coped with that.
        unimplemented("getBootHeapStartSymbolAddress");
        return 0;
    }

    class SolarisNoteEntryHandler extends NoteEntryHandler {
        LwpData[] lwpDataArray = null;
        int lwpDataArrayIndex = 0;

        @Override
        protected void processNoteEntry(int type, String name, byte[] desc) {
            final NoteType noteType = NoteType.get(type);
            switch (noteType) {
                case NT_PSTATUS:
                    final int numLwps = readInt(desc, 4);
                    lwpDataArray = new LwpData[numLwps];
                    break;

                case NT_LWPSINFO: {
                    // this comes before NT_LWPSTATUS
                    ByteBuffer lwpInfo = ByteBuffer.allocateDirect(desc.length);
                    lwpInfo.put(desc);
                    lwpDataArray[lwpDataArrayIndex] = new LwpData(lwpInfo);
                    break;
                }

                case NT_LWPSTATUS: {
                    ByteBuffer lwpStatus = ByteBuffer.allocateDirect(desc.length);
                    lwpStatus.put(desc);
                    lwpDataArray[lwpDataArrayIndex].lwpStatus = lwpStatus;
                    lwpDataArrayIndex++;
                    break;
                }
            }
        }
    }

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
    public boolean gatherThreads(Object teleProcessObject, Object threadSequence, long tlaList, long primordialETLA) {
        return solarisDumpThreadAccess.gatherThreads(teleProcessObject, threadSequence, tlaList, primordialETLA);
    }

}


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
package com.sun.max.tele.debug.guestvm.xen.dbchannel.dump;

import java.io.*;

import com.sun.max.elf.xen.*;
import com.sun.max.elf.xen.section.prstatus.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;

public class DumpProtocol extends CompleteProtocolAdaptor implements Protocol {

    private ImageFileHandler imageFileHandler;
    private XenCoreDumpELFReader xenReader = null;
    /**
     * Creates an instance of {@link Protocol} that can read from Xen core dumps.
     *
     * @param dumpImageFileStr designates the dump file and image file separated by a comma (",")
     */
    private File imageFile = null;
    private File dumpFile = null;
    private RandomAccessFile dumpRaf = null;

    public DumpProtocol(ImageFileHandler imageFileHandler, String dumpFileStr) {
        this.imageFileHandler = imageFileHandler;
        dumpFile = new File(dumpFileStr);
        if (!dumpFile.exists()) {
            throw new IllegalArgumentException("Dump or Image file does not exist or is not accessible");
        }
    }

    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        inappropriate("activateWatchpoint");
        return false;
    }

    @Override
    public boolean attach(int domId, int threadLocalsAreaSize) {
        try {
            xenReader = new XenCoreDumpELFReader(new RandomAccessFile(dumpFile, "r"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public boolean detach() {
        // nothing to do
        return false;
    }

    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        inappropriate("deactivateWatchpoint");
        return false;
    }

    @Override
    public boolean gatherThreads(Object teleDomain, Object threadSequence, long threadLocalsList, long primordialThreadLocals) {
        unimplemented("gatherThreads");
        return false;
    }

    @Override
    public long getBootHeapStart() {
        long address = imageFileHandler.getBootHeapStartSymbolAddress();
        try {
            //This essentially assumes 64 bitness of the address and the target.
            return xenReader.getPagesSection().getDataInputStream(address).read_Elf64_XWord();
        } catch (Exception e) {
            ProgramError.unexpected("Couldnt get Boot Heap start from the dump File");
        }
        return 0;
    }

    @Override
    public int maxByteBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int readBytes(long src, byte[] dst, int dstOffset, int length) {
        unimplemented("readBytes");
        return 0;
    }

    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        try {
            //FIXME: Thhe right context for the given threadId
            GuestContext context = xenReader.getGuestContext(0);
            context.getCpuUserRegs().canonicalizeTeleIntegerRegisters(integerRegisters);
            context.getCpuUserRegs().canonicalizeTeleStateRegisters(stateRegisters);
            System.arraycopy(context.getfpuRegisters(), 0, floatingPointRegisters, 0, floatingPointRegistersSize);
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ImproperDumpFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int readWatchpointAccessCode() {
        inappropriate("readWatchpointAccessCode");
        return 0;
    }

    @Override
    public long readWatchpointAddress() {
        inappropriate("readWatchpointAddress");
        return 0;
    }

    @Override
    public int resume() {
        inappropriate("resume");
        return 0;
    }

    @Override
    public int setInstructionPointer(int threadId, long ip) {
        inappropriate("setInstructionPointer");
        return 0;
    }

    @Override
    public int setTransportDebugLevel(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean singleStep(int threadId) {
        inappropriate("singleStep");
        return false;
    }

    @Override
    public boolean suspend(int threadId) {
        inappropriate("suspend");
        return false;
    }

    @Override
    public boolean suspendAll() {
        inappropriate("suspendAll");
        return false;
    }

    @Override
    public int writeBytes(long dst, byte[] src, int srcOffset, int length) {
        unimplemented("writeBytes");
        return 0;
    }

    private void inappropriate(String name) {
        ProgramError.unexpected("DumpProtocol: inappropriate method: " + name + " invoked");
    }

    private void unimplemented(String name) {
        ProgramError.unexpected("DumpProtocol: unimplemented method: " + name + " invoked");
    }
}

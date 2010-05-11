/**
 *
 */
package com.sun.max.tele.debug.guestvm.xen.coredump;

import java.io.*;
import java.util.logging.*;

import com.sun.max.collect.*;
import com.sun.max.elf.*;
import com.sun.max.elf.xen.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.guestvm.xen.*;

/**
 * @author Puneeet Lakhina
 *
 */
public class GuestVMXenCoreDumpChannelAdaptor implements GuestVMXenDBChannelProtocol {

    File imageFile;
    File dumpFile;

    public GuestVMXenCoreDumpChannelAdaptor(File imageFile, File dumpFile) {
        this.imageFile = imageFile;
        this.dumpFile = dumpFile;
    }

    private static final Logger logger = LogManager.getLogManager().getLogger(GuestVMXenCoreDumpChannelAdaptor.class.toString());

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        File dumpFile = null;
        File imageFile = null;

        for (int i = 0; i < args.length; i++) {
            if ((args[i].equals("-d") || args[i].equals("--dump-file")) && i < (args.length - 1)) {
                dumpFile = new File(args[++i]);
                if (!dumpFile.exists()) {
                    logger.severe("Dump File does not exist");
                    return;
                }
            }
            if ((args[i].equals("-i") || args[i].equals("--image-file")) && i < (args.length - 1)) {
                imageFile = new File(args[++i]);
                if (!imageFile.exists()) {
                    logger.severe("Image File does not exist");
                    return;
                }
            }
        }

        if (dumpFile == null || imageFile == null) {
            System.err.println("Usage: [-d|--dump-file] [coredumpfile] [-i|--image-file] [guestvmimage] ");
            return;
        }
        RandomAccessFile dumpraf = new RandomAccessFile(dumpFile, "r");
        ELFHeader elfHeader = ELFLoader.readELFHeader(dumpraf);
        XenCoreDumpELFReader reader = new XenCoreDumpELFReader(dumpraf, elfHeader);
        reader.readNotesSection();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#activateWatchpoint(long, long, boolean,
     * boolean, boolean, boolean)
     */
    @Override
    public boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#attach(int)
     */
    @Override
    public boolean attach(int domId) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#deactivateWatchpoint(long, long)
     */
    @Override
    public boolean deactivateWatchpoint(long start, long size) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#gatherThreads(com.sun.max.tele.debug.guestvm.xen
     * .GuestVMXenTeleDomain, com.sun.max.collect.AppendableSequence, long, long)
     */
    @Override
    public boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#getBootHeapStart()
     */
    @Override
    public long getBootHeapStart() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#maxByteBufferSize()
     */
    @Override
    public int maxByteBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readByte(long)
     */
    @Override
    public int readByte(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readBytes(long, java.lang.Object, boolean,
     * int, int)
     */
    @Override
    public int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readInt(long)
     */
    @Override
    public long readInt(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readRegisters(int, byte[], int, byte[], int,
     * byte[], int)
     */
    @Override
    public boolean readRegisters(int threadId, byte[] integerRegisters, int integerRegistersSize, byte[] floatingPointRegisters, int floatingPointRegistersSize, byte[] stateRegisters,
                    int stateRegistersSize) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readShort(long)
     */
    @Override
    public int readShort(long address) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readWatchpointAccessCode()
     */
    @Override
    public int readWatchpointAccessCode() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#readWatchpointAddress()
     */
    @Override
    public long readWatchpointAddress() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#resume()
     */
    @Override
    public int resume() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#setInstructionPointer(int, long)
     */
    @Override
    public int setInstructionPointer(int threadId, long ip) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#setTransportDebugLevel(int)
     */
    @Override
    public int setTransportDebugLevel(int level) {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#singleStep(int)
     */
    @Override
    public boolean singleStep(int threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#suspend(int)
     */
    @Override
    public boolean suspend(int threadId) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#suspendAll()
     */
    @Override
    public boolean suspendAll() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#writeByte(long, byte)
     */
    @Override
    public boolean writeByte(long address, byte value) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.max.tele.debug.guestvm.xen.GuestVMXenDBChannelProtocol#writeBytes(long, java.lang.Object, boolean,
     * int, int)
     */
    @Override
    public int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length) {
        // TODO Auto-generated method stub
        return 0;
    }

}

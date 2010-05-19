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
package com.sun.max.tele.debug.guestvm.xen.dbchannel;

import java.io.Serializable;

import com.sun.max.program.*;

/**
 * The interface used by the Maxine Inspector Virtual Edition (aka Guest VM) to access information from a target Maxine VM.
 * It is defined in terms of simple data types that allow an implementation with {@link DataInputStream} and {@link DataOutputStream}
 *
 *
 * @author Mick Jordan
 *
 */

public interface SimpleProtocol {
    /**
     * Establish a connection to a given target VM.
     * @param domId the Xen domain id of the target VM; only meaningful for an active target VM
     * @param threadLocalsAreaSize size of thread locals area frm boot image header
     * @return {@code true} if the attach succeeded, {@code false} otherwise
     */
    boolean attach(int domId, int threadLocalsAreaSize);
    /**
     * Break connection with target VM.
     * @return {@code true} if the detach succeeded, {@code false} otherwise
     */
    boolean detach();
    /**
     * Gets the address of the base of the boot image heap.
     * @return base address of the boot heap
     */
    long getBootHeapStart();

    // Data access methods

    /**
     * Gets the largest byte buffer supported by the protocol.
     * If this value is small it may result in multiple calls to {@link #readBytes} or {@link #writeBytes}
     * to satisfy a single request at a higher level. Therefore it should be as large as practicable.
     * @return size of largest byte buffer that can be used for bulk data transfer
     */
    int maxByteBufferSize();
    /**
     * Reads bytes from the target VM into a byte array.
     * @param src virtual address to read from
     * @param dst byte array to write to
     * @param dstOffset offset to start writing bytes
     * @param length number of bytes to read
     * @return the number of bytes actually read
     */
    int readBytes(long src, byte[] dst, int dstOffset, int length);
    /**
     * Writes bytes from a byte array to the target VM.
     * @param dst virtual address to write to
     * @param src byte array containing data to write
     * @param srcOffset offset to start reading bytes
     * @param length number of bytes to write
     * @return number of bytes actually written
     */
    int writeBytes(long dst, byte[] src, int srcOffset, int length);
    /**
     * Gets the registers of the given thread from the target VM.
     * @param threadId id of the thread for which the registers are requested
     * @param integerRegisters byte array to place the integer registers
     * @param integerRegistersSize size of {@code integerRegisters} array
     * @param floatingPointRegisters byte array to place the floating point registers
     * @param floatingPointRegistersSize size of {@code floatRegisters} array
     * @param stateRegisters byte array to place the state registers
     * @param stateRegistersSize size of {@code stateRegisters} array
     * @return
     */
    boolean readRegisters(int threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    /**
     * Exactly the data passed as arguments to {@link TeleProcess.jniGatherThread}.
     * It is manually serialized and passed as a byte array.
     */
    public static class GatherThreadData implements Serializable {
        int id;
        long localHandle;
        long handle;
        int state;
        long instructionPointer;
        long stackBase;
        long stackSize;
        long tlb;
        long tlbSize;
        int tlaSize;

        public GatherThreadData(int id, long localHandle, long handle, int state, long instructionPointer, long stackBase, long stackSize, long tlb, long tlbSize, int tlaSize) {
            this.id = id;
            this.localHandle = localHandle;
            this.handle = handle;
            this.state = state;
            this.instructionPointer = instructionPointer;
            this.stackBase = stackBase;
            this.stackSize = stackSize;
            this.tlb = tlb;
            this.tlbSize = tlbSize;
            this.tlaSize = tlaSize;
            //Trace.line(1, "GatherThreadData " + id + ", " + localHandle + ", 0x" + Long.toHexString(handle) + ", 0x" + Long.toHexString(state) + ", 0x" + Long.toHexString(instructionPointer) + ", 0x" +
            //               Long.toHexString(stackBase) + ", " + stackSize + ", 0x" + Long.toHexString(tlb) + ", " + tlbSize + ", " + tlaSize);
        }
    }

    /**
     * A dumbed down version of {@link Protocol#gatherThreads} that can communicate using the
     * limitations of this interface. Operates in tandem with {@link #readThreads}.
     * @param threadLocalsList forwarded from {@link Protocol#gatherThreads}
     * @param primordialThreadLocals forwarded from {@link Protocol#gatherThreads}
     * @return number of gathered threads (length of serialized array)
     */
    int gatherThreads(long threadLocalsList, long primordialThreadLocals);

    /**
     * Read the gathered thgeads data into byte array.
     * @param size size needed for byte array (result of {@link #gatherThreads}
     * @param gatherThreadsData byte array for serialized data, {@link ArrayMode#OUT} parameter
     * @return number of gathered threads (length of serialized array)
     */
    int readThreads(int size, byte[] gatherThreadsData);

    // Control methods, only relevant for an active target VM

    int resume();
    int setInstructionPointer(int threadId, long ip);
    boolean singleStep(int threadId);
    boolean suspendAll();
    boolean suspend(int threadId);
    boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec);
    boolean deactivateWatchpoint(long start, long size);
    long readWatchpointAddress();
    int readWatchpointAccessCode();

    /**
     * Mechanism to enable option debugging information frm the protocol implementation.
     * @param level value to indicate the level of debugging information. Zero implies no debugging.
     * @return the previous value
     */
    int setTransportDebugLevel(int level);


}

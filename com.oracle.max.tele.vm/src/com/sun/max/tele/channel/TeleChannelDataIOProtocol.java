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
package com.sun.max.tele.channel;

import java.io.Serializable;

/**
 * Subset of the interface used by the Maxine Inspector to access information from a target Maxine VM, that is defined
 * in terms of simple data types that allows an implementation using {@link DataInputStream} and
 * {@link DataOutputStream}. Specifically, method argument and results types must be capable of being transmitted using
 * the methods of those classes; in particular no object types other than {@link String}, are allowed.
 *
 */

public interface TeleChannelDataIOProtocol {
    /** Initialize the tele layer, informing it of the size of the thread locals area and the endianness of the target.
    * @param tlaSize size of thread locals area from boot image header
    * @param bigEndian {@code true} if the target VM is big-endian, {@code false} otherwise.
    * @return {@code true} is the set succeeded, {@code false} otherwise.
    */
    boolean initialize(int tlaSize, boolean bigEndian);
    /**
     * Create a new target VM.
     * @param pathName file system pathname of target VM image
     * @param commandLineArguments arguments to VM
     * @return handle for target VM or < zero if creation failed
     */
    long create(String pathName, String[] commandLineArguments);
    /**
     * Establish a connection to an existing given target VM.
     * @param id the OS-specific id of the target VM; only meaningful for an active target VM
     * @return {@code true} if the attach succeeded, {@code false} otherwise
     */
    boolean attach(int id);
    /**
     * Break connection with target VM.
     * The should be called whether the connection was initiated by {@link #create} or {@link #attach}.
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
     * @param length number of bytes to read, {@code length <= maxByteBufferSize()}
     * @return the number of bytes actually read
     */
    int readBytes(long src, byte[] dst, int dstOffset, int length);
    /**
     * Writes bytes from a byte array to the target VM.
     * @param dst virtual address to write to
     * @param src byte array containing data to write
     * @param srcOffset offset to start reading bytes
     * @param length number of bytes to write, {@code length <= maxByteBufferSize()}
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
    boolean readRegisters(long threadId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    /**
     * The data passed as arguments to {@link TeleProcess.jniGatherThread}.
     * It is manually serialized and passed as a byte array.
     */
    public static class GatherThreadData implements Serializable {
        public int id;
        public long localHandle;
        public long handle;
        public int state;
        public long instructionPointer;
        public long stackBase;
        public long stackSize;
        public long tlb;
        public long tlbSize;
        public int tlaSize;

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
     * A dumbed down version of {@link TeleChannelProtocol#gatherThreads} that can communicate using the
     * limitations of this interface. Operates in tandem with {@link #readThreads}.
     * @param tlaList forwarded from {@link TeleChannelProtocol#gatherThreads}
     * @return number of gathered threads (length of serialized array)
     */
    int gatherThreads(long tlaList);

    /**
     * Read the gathered threads data into byte array.
     * @param size size needed for byte array (result of {@link #gatherThreads}
     * @param gatherThreadsData byte array for serialized data, {@link ArrayMode#OUT} parameter
     * @return number of gathered threads (length of serialized array)
     */
    int readThreads(int size, byte[] gatherThreadsData);

    // Control methods, only relevant for an active target VM
    // The boolean return values are: true if the operation is successful, false otherwise.

    boolean setInstructionPointer(long threadId, long ip);
    boolean singleStep(long threadId);
    boolean resumeAll();
    boolean suspendAll();
    boolean resume(long threadId);
    boolean suspend(long threadId);
    int waitUntilStoppedAsInt();
    boolean kill();
    boolean activateWatchpoint(long start, long size, boolean after, boolean read, boolean write, boolean exec);
    boolean deactivateWatchpoint(long start, long size);
    long readWatchpointAddress();
    int readWatchpointAccessCode();

    /**
     * Mechanism to enable option debugging information from the protocol implementation.
     * @param level value to indicate the level of debugging information. Zero implies no debugging.
     * @return the previous value
     */
    int setTransportDebugLevel(int level);


}

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
package com.sun.max.tele.debug.guestvm.xen;

import com.sun.max.collect.*;
import com.sun.max.tele.debug.*;

/**
 * The interface used by the Maxine Inspector Virtual Edition (aka Guest VM) to access information from a target Maxine VM.
 *
 * @author Mick Jordan
 *
 */

public interface GuestVMXenDBChannelProtocol {
    /**
     * Establish a connection to a given target VM.
     * @param domId the Xen domain id of the target VM; only meaningful for an active target VM
     * @return {@code true} if the connection succeeded, {@code false} otherwise
     */
    boolean attach(int domId);
    /**
     * Gets the address of the base of the boot image heap.
     * @return base address of the boot heap
     */
    long getBootHeapStart();

    // Data access methods

    /**
     * Gets the largest byte buffer supported by the protocol.
     * @return size of largest byte buffer that can be used for bulk data transfer
     */
    int maxByteBufferSize();
    /**
     * Reads bytes from the target VM into either an array or a direct {@link java.nio.ByteBuffer}.
     * @param src virtual address to read from
     * @param dst either byte array or a  {@link java.nio.ByteBuffer byte buffer} to write to
     * @param isDirectByteBuffer {@code true} if {@code dst} is a {@link java.nio.ByteBuffer byte buffer}, {@code false} if it is a byte array.
     * @param dstOffset offset in the byte buffer where writing should begin
     * @param length number of bytes to read
     * @return the number of bytes actually read
     */
    int readBytes(long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);
    /**
     * Writes bytes from either an array or a direct {@link java.nio.ByteBuffer} to the target VM.
     * @param dst virtual address to write to
     * @param src either byte array or a {@link java.nio.ByteBuffer byte buffer} to read from
     * @param isDirectByteBuffer {@code true} if {@code src} is a {@link java.nio.ByteBuffer byte buffer}, {@code false} if it is a byte array.
     * @param srcOffset offset in the byte buffer where readinh should begin
     * @param length number of bytes to write
     * @return number of bytes actually written
     */
    int writeBytes(long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);
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
     * Gathers the set of active threads in the target VM.
     * This is not really a data access method because it assumes an active agent in the VM that can get this information by interpreting the state of the VM.
     * It needs re-thinking in the context of inactive domains and targets with no active agent.
     * @param teleDomain
     * @param threads
     * @param domainId
     * @param threadLocalsList
     * @param primordialThreadLocals
     * @return
     */
    boolean gatherThreads(GuestVMXenTeleDomain teleDomain, AppendableSequence<TeleNativeThread> threads, long threadLocalsList, long primordialThreadLocals);

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

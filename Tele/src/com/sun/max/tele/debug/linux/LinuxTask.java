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
package com.sun.max.tele.debug.linux;

import java.io.*;
import java.nio.*;

import com.sun.max.lang.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.runtime.*;

/**
 * A native Linux task (process or thread; threads are implemented as processes on Linux) that is controlled and
 * accessed via the Linux {@code ptrace} facility. The methods that interact with the traced task always execute on a
 * {@linkplain SingleThread single dedicated thread} fulfilling a requirement of ptrace.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class LinuxTask {

    /**
     * The thread group identifier (TGID) of this task. This is the process identifier shared by all tasks in a
     * process and is the value returned by getpid(2) since Linux 2.4.
     */
    private final int tgid;

    /**
     * The (system-wide) unique task identifier (TID) of this task. The first task in a thread group is
     * the <i>leader</i> of the new thread group and its {@link #tgid TGID} is the same as its {@link #tid TID}.
     *
     * Note that this is <b>not<b> the identifier returned by pthread_create(3p).
     */
    private final int tid;

    /**
     * Gets the thread group identifier (TGID) of this task. This is the process identifier shared by all
     * tasks in a process and is the value returned by getpid(2) since Linux 2.4.
     */
    public int tgid() {
        return tgid;
    }

    private final LinuxTask leader;

    /**
     * Gets the (system-wide) unique task identifier (TID) of this task. The first task in a thread group is
     * the <i>leader</i> of the new thread group and its {@link #tgid TGID} is the same as its {@link #tid TID}.
     *
     * Note that this is <b>not<b> the identifier returned by pthread_create(3p).
     */
    public int tid() {
        return tid;
    }

    /**
     * Gets the leader task from the thread group this task is a member of. The leader of a thread group is
     * the first task created by clone(2) in a thread group.
     * @return
     */
    public LinuxTask leader() {
        return leader;
    }

    /**
     * Determines if this task is the {@linkplain #leader() leader} in its thread group.
     */
    public boolean isLeader() {
        return leader == this;
    }

    /**
     * Creates the <i>leader</i> task for a Linux process.
     *
     * @param tgid
     * @param tid
     */
    LinuxTask(int tgid, int tid) {
        assert tgid == tid : "TGID must match TID for leader task";
        this.tgid = tgid;
        this.tid = tid;
        this.leader = this;
    }

    /**
     * Creates a <i>non-leader</i> task for a Linux process.
     *
     * @param leader the leader task for the process
     * @param tid
     */
    LinuxTask(LinuxTask leader, int tid) {
        this.tgid = leader.tgid;
        this.tid = tid;
        this.leader = leader;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LinuxTask) {
            final LinuxTask other = (LinuxTask) obj;
            return tid() == other.tid();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return tid();
    }

    private static native int nativeCreateChildProcess(long argv, int vmAgentSocketPort);

    /**
     * Creates a new traced task via {@code fork()} and {@code execve()}.
     *
     * @param argv the arguments with which to start the new process
     * @param vmAgentSocketPort the port on which the debugger agent is listening for a connection from the VM.
     * @return an instance of {@link LinuxTask} representing the new process or {@code null} if there was an error
     *         creating the new process. If a new process was created, it is paused at the call to {@code execve}
     */
    public static LinuxTask createChild(final long argv, final int vmAgentSocketPort) {
        return SingleThread.execute(new Function<LinuxTask>() {
            public LinuxTask call() {
                final int tgid = nativeCreateChildProcess(argv, vmAgentSocketPort);
                if (tgid < 0) {
                    return null;
                }
                return new LinuxTask(tgid, tgid);
            }
        });
    }

    private static native boolean nativeDetach(int tgid, int tid);

    public synchronized void detach() throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeDetach(tgid, tid)) {
                        throw new IOException("Ptrace.detach");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    private static native boolean nativeSingleStep(int tgid, int tid);

    public synchronized boolean singleStep() {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeSingleStep(tgid, tid);
            }
        });
    }

    private static native boolean nativeResume(int tgid, int tid, boolean allTasks);

    public synchronized void resume(final boolean allTasks) throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeResume(tgid, tid, allTasks)) {
                        throw new OSExecutionRequestException("Ptrace.resume");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native boolean nativeSuspend(int tgid, int tid, boolean allTasks);

    /**
     * Suspends one or more tasks.
     *
     * Note: This operation is not synchronized otherwise there would be no way to interrupt
     * {@link #waitUntilStopped(boolean)}
     *
     * @param allTasks {@code true} if all tasks should be suspended, {@code false} if only this task should be suspended
     * @throws OSExecutionRequestException
     */
    public void suspend(boolean allTasks) throws OSExecutionRequestException {
        if (!nativeSuspend(tgid, tid, allTasks)) {
            throw new OSExecutionRequestException("Ptrace.suspend");
        }
    }

    private static native boolean nativeWait(int tgid, int tid, boolean allTasks);

    public synchronized boolean waitUntilStopped(final boolean allTasks) {
        if (!allTasks) {
            FatalError.unimplemented();
        }
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeWait(tgid, tid, allTasks);
            }
        });
    }

    private static native boolean nativeKill(int tgid, int tid);

    /**
     * Kills all tasks.
     *
     * Note: This operation is not synchronized otherwise there would be no way to interrupt
     * {@link #waitUntilStopped(boolean)}
     *
     * @throws OSExecutionRequestException
     */
    public void kill() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeKill(tgid, tid)) {
                        throw new OSExecutionRequestException("Ptrace.kill");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    /**
     * The file in /proc through which the memory of this task can be read. As Linux does not
     * support writing to the memory of a process via /proc, it's still necessary to use
     * ptrace for {@linkplain #writeBytes(Address, ByteBuffer, int, int) writing}.
     */
    private RandomAccessFile memory;

    /**
     * Copies bytes from the tele process into a given {@linkplain ByteBuffer#isDirect() direct ByteBuffer} or byte
     * array.
     *
     * @param src the address in the tele process to copy from
     * @param dst the destination of the copy operation. This is a direct {@link ByteBuffer} or {@code byte[]}
     *            depending on the value of {@code isDirectByteBuffer}
     * @param isDirectByteBuffer
     * @param dstOffset the offset in {@code dst} at which to start writing
     * @param length the number of bytes to copy
     * @return the number of bytes copied or -1 if there was an error
     */
    private static native int nativeReadBytes(int tgid, int tid, long src, Object dst, boolean isDirectByteBuffer, int dstOffset, int length);

    public synchronized int readBytes(final Address src, final ByteBuffer dst, final int offset, final int length) {
        if (!isLeader()) {
            return leader().readBytes(src, dst, offset, length);
        }
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                assert !src.isZero();
                final long addr = src.toLong();
                if (addr < 0) {
                    // RandomAccessFile.see() can't handle unsigned long offsets: have to resort to a JNI call
                    if (dst.isDirect()) {
                        return nativeReadBytes(tgid, tid, addr, dst, true, offset, length);
                    }
                    assert dst.array() != null;
                    return nativeReadBytes(tgid, tid, addr, dst.array(), false, offset, length);
                } else {
                    if (memory == null) {
                        memory = new RandomAccessFile("/proc/" + tgid() + "/mem", "r");
                    }
                    try {
                        final ByteBuffer dstView = (ByteBuffer) dst.duplicate().position(offset).limit(offset + length);
                        dstView.position(offset);
                        return memory.getChannel().read(dstView, addr);
                    } catch (IOException ioException) {
                        throw new DataIOError(src, ioException.toString());
                    }
                }
            }
        });
    }

    /**
     * Copies bytes from a given {@linkplain ByteBuffer#isDirect() direct ByteBuffer} or byte array into the tele process.
     *
     * @param dst the address in the tele process to copy to
     * @param src the source of the copy operation. This is a direct {@link ByteBuffer} or {@code byte[]}
     *            depending on the value of {@code isDirectByteBuffer}
     * @param isDirectByteBuffer
     * @param srcOffset the offset in {@code src} at which to start reading
     * @param length the number of bytes to copy
     * @return the number of bytes copied or -1 if there was an error
     */
    private static native int nativeWriteBytes(int tgid, int tid, long dst, Object src, boolean isDirectByteBuffer, int srcOffset, int length);

    public synchronized int writeBytes(final Address dst, final ByteBuffer src, final int offset, final int length) {
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                assert src.limit() - offset >= length;
                if (src.isDirect()) {
                    return nativeWriteBytes(tgid, tid, dst.toLong(), src, true, offset, length);
                }
                assert src.array() != null;
                return nativeWriteBytes(tgid, tid, dst.toLong(), src.array(), false, src.arrayOffset() + offset, length);
            }
        });
    }

    private static native boolean nativeSetInstructionPointer(int tid, long instructionPointer);

    public synchronized boolean setInstructionPointer(final Address instructionPointer) {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeSetInstructionPointer(tid, instructionPointer.toLong());
            }
        });
    }

    private static native boolean nativeReadRegisters(int tid,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    public synchronized boolean readRegisters(
                    final byte[] integerRegisters,
                    final byte[] floatingPointRegisters,
                    final byte[] stateRegisters) {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeReadRegisters(tid, integerRegisters, integerRegisters.length,
                                floatingPointRegisters, floatingPointRegisters.length,
                                stateRegisters, stateRegisters.length);
            }
        });
    }

    public void close() {
        if (memory != null) {
            try {
                memory.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}

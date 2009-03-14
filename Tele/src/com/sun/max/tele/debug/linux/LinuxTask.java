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

import com.sun.max.lang.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

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
     * The task group identifier (TGID) of this task. This is the process identifier shared by all tasks in a
     * process and is the value returned by getpid(2) since Linux 2.4.
     */
    private final int _tgid;

    /**
     * The (system-wide) unique task identifier (TID( of this task. The first task in a task group is
     * the <i>leader</i> of the new task group and its {@link #_tgid TGID} is the same as its {@link #_tid TID}.
     *
     * Note that this is <b>not<b> the identifier returned by pthread_create(3p).
     */
    private final int _tid;

    /**
     * Gets the task group identifier (TGID) of this task. This is the process identifier shared by all
     * tasks in a process and is the value returned by getpid(2) since Linux 2.4.
     */
    public int tgid() {
        return _tgid;
    }

    /**
     * Gets the (system-wide) unique task identifier (TID( of this task. The first task in a task group is
     * the <i>leader</i> of the new task group and its {@link #_tgid TGID} is the same as its {@link #_tid TID}.
     *
     * Note that this is <b>not<b> the identifier returned by pthread_create(3p).
     */
    public int tid() {
        return _tid;
    }

    LinuxTask(int tgid, int tid) {
        _tgid = tgid;
        _tid = tid;
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
                    if (!nativeDetach(_tgid, _tid)) {
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
                return nativeSingleStep(_tgid, _tid);
            }
        });
    }

    private static native boolean nativeResume(int tgid, int tid);

    public synchronized void resume() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeResume(_tgid, _tid)) {
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

    public synchronized void suspend(final boolean allTasks) throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeSuspend(_tgid, _tid, allTasks)) {
                        throw new OSExecutionRequestException("Ptrace.suspend");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native boolean nativeWait(int tgid, int tid);

    public synchronized boolean waitUntilStopped() {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeWait(_tgid, _tid);
            }
        });
    }

    private static native boolean nativeKill(int tgid, int tid);

    public synchronized void kill() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeKill(_tgid, _tid)) {
                        throw new OSExecutionRequestException("Ptrace.kill");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native int nativeReadBytes(int tgid, int tid, long address, byte[] buffer, int offset, int length);

    public synchronized int readBytes(final Address address, final byte[] buffer, final int offset, final int length) {
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                assert !address.isZero();
                return nativeReadBytes(_tgid, _tid, address.toLong(), buffer, offset, length);
            }
        });
    }

    private static native int nativeWriteBytes(int tgid, int tid, long address, byte[] buffer, int offset, int length);

    public synchronized int writeBytes(final Address address, final byte[] buffer, final int offset, final int length) {
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                return nativeWriteBytes(_tgid, _tid, address.toLong(), buffer, offset, length);
            }
        });
    }

    private static native boolean nativeSetInstructionPointer(int tid, long instructionPointer);

    public synchronized boolean setInstructionPointer(final Address instructionPointer) {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeSetInstructionPointer(_tid, instructionPointer.toLong());
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
                return nativeReadRegisters(_tid, integerRegisters, integerRegisters.length,
                                floatingPointRegisters, floatingPointRegisters.length,
                                stateRegisters, stateRegisters.length);
            }
        });
    }
}

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
 * A native process that is controlled and accessed via the Linux {@code ptrace} facility.
 * This is separated out from {@link LinuxTeleProcess} as threads in Linux are implemented
 * as processes and thus need to be attached to {@code ptrace} individually.
 * The methods that interact with the traced process (or thread) always execute on a
 * {@linkplain SingleThread single dedicated thread}.
 *
 * @author Bernd Mathiske
 */
public final class PTracedProcess {

    private final int _processID;

    public int processID() {
        return _processID;
    }

    private PTracedProcess(int processID) {
        _processID = processID;
    }

    private static native int nativeCreateChildProcess(long argv, int vmAgentSocketPort);

    /**
     * Creates a new traced process via {@code fork()} and {@code execve()}.
     *
     * @param argv the arguments with which to start the new process
     * @param vmAgentSocketPort the port on which the debugger agent is listening for a connection from the VM.
     * @return an instance of {@link PTracedProcess} representing the new process or {@code null} if there was an error
     *         creating the new process. If a new process was created, it is paused at the call to {@code execve}
     */
    public static PTracedProcess createChild(final long argv, final int vmAgentSocketPort) {
        return SingleThread.execute(new Function<PTracedProcess>() {
            public PTracedProcess call() {
                final int processID = nativeCreateChildProcess(argv, vmAgentSocketPort);
                if (processID < 0) {
                    return null;
                }
                return new PTracedProcess(processID);
            }
        });
    }

    private static native boolean nativeAttach(int processID);

    public static PTracedProcess attach(final int processID) throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeAttach(processID)) {
                        throw new IOException("Ptrace.attach");
                    }
                    return null;
                }
            });
            return new PTracedProcess(processID);
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    private static native boolean nativeDetach(int processID);

    public synchronized void detach() throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeDetach(_processID)) {
                        throw new IOException("Ptrace.detach");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    private static native boolean nativeSingleStep(int processID);

    public synchronized boolean singleStep() {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeSingleStep(_processID);
            }
        });
    }

    private static native boolean nativeResume(int processID);

    public synchronized void resume() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeResume(_processID)) {
                        throw new OSExecutionRequestException("Ptrace.resume");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native boolean nativeSuspend(int processID);

    public synchronized void suspend() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeSuspend(_processID)) {
                        throw new OSExecutionRequestException("Ptrace.suspend");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native boolean nativeWait(int processID);

    public synchronized boolean waitUntilStopped() {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeWait(_processID);
            }
        });
    }

    private static native boolean nativeKill(int processID);

    public synchronized void kill() throws OSExecutionRequestException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeKill(_processID)) {
                        throw new OSExecutionRequestException("Ptrace.kill");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(OSExecutionRequestException.class, exception);
        }
    }

    private static native int nativeReadBytes(int processID, long address, byte[] buffer, int offset, int length);

    public synchronized int readBytes(final Address address, final byte[] buffer, final int offset, final int length) {
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                return nativeReadBytes(_processID, address.toLong(), buffer, offset, length);
            }
        });
    }

    private static native int nativeWriteBytes(int processID, long address, byte[] buffer, int offset, int length);

    public synchronized int writeBytes(final Address address, final byte[] buffer, final int offset, final int length) {
        return SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                return nativeWriteBytes(_processID, address.toLong(), buffer, offset, length);
            }
        });
    }

    private static native boolean nativeSetInstructionPointer(int processID, long instructionPointer);

    public synchronized boolean setInstructionPointer(final Address instructionPointer) {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeSetInstructionPointer(_processID, instructionPointer.toLong());
            }
        });
    }

    private static native boolean nativeReadRegisters(int processID,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    public synchronized boolean readRegisters(
                    final byte[] integerRegisters,
                    final byte[] floatingPointRegisters,
                    final byte[] stateRegisters) {
        return SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeReadRegisters(_processID, integerRegisters, integerRegisters.length,
                                floatingPointRegisters, floatingPointRegisters.length,
                                stateRegisters, stateRegisters.length);
            }
        });
    }
}

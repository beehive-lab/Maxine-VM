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
import com.sun.max.unsafe.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public final class Ptrace {

    private final int _processID;

    public int processID() {
        return _processID;
    }

    private Ptrace(int processID) {
        _processID = processID;
    }

    private static native int nativeCreateChildProcess(String filename);

    public static Ptrace createChild(final String filename) {
        return SingleThread.execute(new Function<Ptrace>() {
            public Ptrace call() {
                final int processID = nativeCreateChildProcess(filename);
                if (processID < 0) {
                    return null;
                }
                return new Ptrace(processID);
            }
        });
    }

    private static native boolean nativeAttach(int processID);

    public static Ptrace attach(final int processID) throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeAttach(processID)) {
                        throw new IOException("Ptrace.attach");
                    }
                    return null;
                }
            });
            return new Ptrace(processID);
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

    private static native boolean nativeSyscall(int processID);

    public synchronized void syscall() throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeSyscall(_processID)) {
                        throw new IOException("Ptrace.syscall");
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

    public synchronized void resume() throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeResume(_processID)) {
                        throw new IOException("Ptrace.resume");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    private static native boolean nativeKill(int processID);

    public synchronized void kill() throws IOException {
        try {
            SingleThread.executeWithException(new Function<Void>() {
                public Void call() throws Exception {
                    if (!nativeKill(_processID)) {
                        throw new IOException("Ptrace.kill");
                    }
                    return null;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    private static native int nativeReadDataByte(int processID, long address);

    public synchronized byte readDataByte(final Address address) throws IOException {
        final int result = SingleThread.execute(new Function<Integer>() {
            public Integer call() throws Exception {
                return nativeReadDataByte(_processID, address.toLong());
            }
        });
        if (result < 0) {
            throw new DataIOError(address, "Ptrace.readDataByte");
        }
        return (byte) result;
    }

    private static native int nativeReadTextByte(int processID, long address);

    public synchronized byte readTextByte(final Address address) throws IOException {
        try {
            return SingleThread.executeWithException(new Function<Byte>() {
                public Byte call() throws IOException {
                    final int result = nativeReadTextByte(_processID, address.toLong());
                    if (result < 0) {
                        throw new IOException("Ptrace.readTextByte");
                    }
                    return (byte) result;
                }
            });
        } catch (Exception exception) {
            throw Exceptions.cast(IOException.class, exception);
        }
    }

    /**
     * Not thread-safe: Reads a word, changes a byte in it and writes the word back.
     */
    private static native boolean nativeWriteDataByte(int processID, long address, byte value);

    public synchronized void writeDataByte(final Address address, final byte value) throws IOException {
        if (!SingleThread.execute(new Function<Boolean>() {
            public Boolean call() throws Exception {
                return nativeWriteDataByte(_processID, address.toLong(), value);
            }
        })) {
            throw new DataIOError(address, "Ptrace.writeDataByte");
        }
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

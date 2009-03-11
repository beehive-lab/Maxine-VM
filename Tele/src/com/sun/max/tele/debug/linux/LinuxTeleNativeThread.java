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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class LinuxTeleNativeThread extends TeleNativeThread {

    @Override
    public LinuxTeleProcess teleProcess() {
        return (LinuxTeleProcess) super.teleProcess();
    }

    private int _lwpID;

    private int _type;

    public int type() {
        return _type;
    }

    private short _flags;

    public int flags() {
        return _flags;
    }

    private int _priority;

    public int priority() {
        return _priority;
    }

    private PTracedProcess _ptrace;

    private PTracedProcess ptrace() {
        if (_ptrace == null) {
            if (this == teleProcess().primordialThread()) {
                _ptrace = teleProcess().ptrace();
            } else {
                try {
                    _ptrace = PTracedProcess.attach(_lwpID);
                } catch (IOException ioException) {
                    throw new TeleError("could not attach to lwp", ioException);
                }
            }
        }
        return _ptrace;
    }

    LinuxTeleNativeThread(LinuxTeleProcess teleProcess, long threadID, int lwpID, long stackStart, long stackSize) {
        super(teleProcess, stackStart, stackSize, threadID);
        _lwpID = lwpID;
    }

    @Override
    public boolean updateInstructionPointer(Address address) {
        return ptrace().setInstructionPointer(address);
    }

    @Override
    protected boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters) {
        return ptrace().readRegisters(integerRegisters, floatingPointRegisters, stateRegisters);
    }

    @Override
    public boolean singleStep() {
        return ptrace().singleStep();
    }

    @Override
    public boolean threadResume() {
        throw Problem.unimplemented();
    }

    @Override
    public boolean threadSuspend() {
        throw Problem.unimplemented();
    }
}

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

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public class LinuxTeleNativeThread extends TeleNativeThread {

    @Override
    public LinuxTeleProcess teleProcess() {
        return (LinuxTeleProcess) super.teleProcess();
    }

    private int type;

    public int type() {
        return type;
    }

    private short flags;

    public int flags() {
        return flags;
    }

    private int priority;

    public int priority() {
        return priority;
    }

    private final LinuxTask task;

    LinuxTask task() {
        return task;
    }

    LinuxTeleNativeThread(LinuxTeleProcess teleProcess, int id, long tid, long stackBase, long stackSize) {
        super(teleProcess, id, tid, stackBase, stackSize);
        task = new LinuxTask(teleProcess.task(), (int) tid);
    }

    @Override
    public boolean updateInstructionPointer(Address address) {
        return task().setInstructionPointer(address);
    }

    @Override
    protected boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters) {
        return task().readRegisters(integerRegisters, floatingPointRegisters, stateRegisters);
    }

    @Override
    protected boolean singleStep() {
        return task().singleStep();
    }

    @Override
    protected boolean threadResume() {
        throw FatalError.unimplemented();
    }

    @Override
    public boolean threadSuspend() {
        throw FatalError.unimplemented();
    }
}

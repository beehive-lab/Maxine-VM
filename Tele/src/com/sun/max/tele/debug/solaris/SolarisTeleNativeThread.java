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
package com.sun.max.tele.debug.solaris;

import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 */
public class SolarisTeleNativeThread extends TeleNativeThread {

    @Override
    public SolarisTeleProcess teleProcess() {
        return (SolarisTeleProcess) super.teleProcess();
    }

    public SolarisTeleNativeThread(SolarisTeleProcess teleProcess, int id, long lwpId, long stackBase, long stackSize) {
        super(teleProcess, id, lwpId, stackBase, stackSize);
    }

    private static native boolean nativeSetInstructionPointer(long processHandle, long lwpId, long address);

    private long lwpId() {
        return handle();
    }

    @Override
    public boolean updateInstructionPointer(Address address) {
        return nativeSetInstructionPointer(teleProcess().processHandle(), lwpId(), address.toLong());
    }

    private static native boolean nativeReadRegisters(long processHandle, long lwpId,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    @Override
    protected boolean readRegisters(
                    byte[] integerRegisters,
                    byte[] floatingPointRegisters,
                    byte[] stateRegisters) {
        return nativeReadRegisters(teleProcess().processHandle(), lwpId(),
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    protected boolean singleStep() {
        return nativeSingleStep(teleProcess().processHandle(), lwpId());
    }

    /**
     * Initiates a single step on this thread.
     *
     * @return false if there was a problem initiating the single step, true otherwise
     */
    private static native boolean nativeSingleStep(long processHandle, long lwpId);

    /**
     * Resumes running this thread.
     *
     * @return false if there was a problem resuming this thread, true otherwise
     */
    private static native boolean nativeResume(long processHandle, long lwpId);

    @Override
    protected boolean threadResume() {
        return nativeResume(teleProcess().processHandle(), lwpId());
    }

    private static native boolean nativeSuspend(long processHandle, long lwpId);

    @Override
    public boolean threadSuspend() {
        return nativeSuspend(teleProcess().processHandle(), lwpId());
    }
}

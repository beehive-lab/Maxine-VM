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
package com.sun.max.tele.debug.darwin;

import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class DarwinTeleNativeThread extends TeleNativeThread {

    @Override
    public DarwinTeleProcess teleProcess() {
        return (DarwinTeleProcess) super.teleProcess();
    }

    public DarwinTeleNativeThread(DarwinTeleProcess teleProcess, int id, long machThread, long stackBase, long stackSize) {
        super(teleProcess, id, machThread, stackBase, stackSize);
    }

    @Override
    public boolean updateInstructionPointer(Address address) {
        return nativeSetInstructionPointer(teleProcess().task(), machThread(), address.toLong());
    }

    private long machThread() {
        return handle();
    }

    private static native boolean nativeReadRegisters(long machThread,
                    byte[] integerRegisters, int integerRegistersSize,
                    byte[] floatingPointRegisters, int floatingPointRegistersSize,
                    byte[] stateRegisters, int stateRegistersSize);

    @Override
    protected boolean readRegisters(
                    byte[] integerRegisters,
                    byte[] floatingPointRegisters,
                    byte[] stateRegisters) {
        return nativeReadRegisters(machThread(),
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    protected boolean singleStep() {
        return nativeSingleStep(teleProcess().task(), machThread());
    }

    @Override
    protected boolean threadResume() {
        throw Problem.unimplemented();
    }

    @Override
    public boolean threadSuspend() {
        throw Problem.unimplemented();
    }

    private static native boolean nativeSingleStep(long task, long threadID);
    private static native boolean nativeSetInstructionPointer(long task, long threadID, long address);
}

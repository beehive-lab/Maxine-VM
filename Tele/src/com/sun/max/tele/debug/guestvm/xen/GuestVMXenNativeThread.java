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

import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

public class GuestVMXenNativeThread extends TeleNativeThread {

    private final String _name;
    private boolean _mark;

    public String name() {
        return _name;
    }

    @Override
    public GuestVMXenTeleDomain teleProcess() {
        return (GuestVMXenTeleDomain) super.teleProcess();
    }

    protected GuestVMXenNativeThread(GuestVMXenTeleDomain teleDomain, int id, String name, long stackBase, long stackSize) {
        super(teleDomain, stackBase, stackSize, id);
        _name = name;
    }

    @Override
    protected boolean readRegisters(byte[] integerRegisters, byte[] floatingPointRegisters, byte[] stateRegisters) {
        return GuestVMXenDBChannel.readRegisters((int) id(),
                        integerRegisters, integerRegisters.length,
                        floatingPointRegisters, floatingPointRegisters.length,
                        stateRegisters, stateRegisters.length);
    }

    @Override
    protected boolean updateInstructionPointer(Address address) {
        return GuestVMXenDBChannel.setInstructionPointer((int) id(), address.toLong()) == 0;
    }

    @Override
    public boolean singleStep() {
        return GuestVMXenDBChannel.singleStep((int) id());
    }

    // In the current synchronous connection with the target domain, we only ever stop at a breakpoint
    // and control does not return to the inspector until that happens (see GuestVMDBChannel.nativeResume)

    @Override
    public boolean threadSuspend() {
        return GuestVMXenDBChannel.suspend((int) id());
    }

    public boolean marked() {
        return _mark;
    }

    public void setMarked(boolean mark) {
        _mark = mark;
    }

    @Override
    public boolean threadResume() {
        throw Problem.unimplemented();
    }
}

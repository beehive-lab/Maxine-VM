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
package com.sun.max.tele.debug;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.tele.*;

/**
 * Facility that can send commands to the tele VM
 * and to receive dedicated runtime information from the tele VM.
 *
 * @author Bernd Mathiske
 */
public final class VMTeleMessenger extends AbstractTeleVMHolder implements TeleMessenger {

    private final MaxineMessenger _maxineMessenger;

    public VMTeleMessenger(TeleVM teleVM) {
        super(teleVM);
        _maxineMessenger = new MaxineMessenger();
    }

    private Pointer _infoPointer;

    public void enable() {
        _infoPointer = teleVM().bootImageStart().plus(teleVM().bootImage().header()._messengerInfoOffset);
        teleVM().dataAccess().writeWord(_infoPointer, Address.fromInt(1)); // setting to non-zero indicates enabling
    }

    public boolean activate() {
        if (!_maxineMessenger.isActivated()) {
            final Pointer info = teleVM().dataAccess().readWord(_infoPointer).asPointer();
            final Size dataSize = teleVM().dataAccess().getWord(info, 0, 0).asSize();

            // Note that in/out are crossed over.
            final Pointer inData = teleVM().dataAccess().getWord(info, 0, 2).asPointer();
            if (inData.isZero()) {
                return false;
            }

            // Note that in/out are crossed over.
            final Pointer outData = teleVM().dataAccess().getWord(info, 0, 1).asPointer();
            if (outData.isZero()) {
                return false;
            }
            _maxineMessenger.activate(teleVM().dataAccess(), inData, outData, dataSize.toInt());
        }
        return true;
    }

    public void requestBytecodeBreakpoint(MethodKey methodKey, int bytecodePosition) {
        activate();
        _maxineMessenger.send(new BytecodeBreakpointMessage(BytecodeBreakpointMessage.Action.MAKE, methodKey, bytecodePosition));
    }

    public void cancelBytecodeBreakpoint(MethodKey methodKey, int bytecodePosition) {
        activate();
        _maxineMessenger.send(new BytecodeBreakpointMessage(BytecodeBreakpointMessage.Action.DELETE, methodKey, bytecodePosition));
    }

}

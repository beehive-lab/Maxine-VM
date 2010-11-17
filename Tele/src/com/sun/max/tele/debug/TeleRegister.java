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
import com.sun.max.util.*;


/**
 * Access to the description and current state (within a particular thread) of a register
 * in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleRegister implements MaxRegister {

    private final TeleVM teleVM;
    private final TeleRegisters teleRegisters;
    private final Symbol register;
    private final String entityName;
    private final String entityDescription;

    protected TeleRegister(TeleRegisters teleRegisters, Symbol register, TeleNativeThread teleNativeThread) {
        this.teleVM = teleNativeThread.vm();
        this.teleRegisters = teleRegisters;
        this.register = register;
        this.entityName = "Thread-" + teleNativeThread.localHandle() + " register " + register.name();
        this.entityDescription = "A machine register, together with current value in the " + vm().entityName() + " for " + teleNativeThread.entityName();
    }

    public TeleVM vm() {
        return teleVM;
    }

    public String entityName() {
        return entityName;
    }

    public String entityDescription() {
        return entityDescription;
    }

    public MaxEntityMemoryRegion<MaxRegister> memoryRegion() {
        // A register doesn't occupy a memory region
        return null;
    }

    public boolean contains(Address address) {
        return false;
    }

    public String name() {
        return register.name();
    }

    public Address value() {
        return teleRegisters.getValue(register);
    }

    public boolean isFlagsRegister() {
        return teleRegisters.isFlagsRegister(register);
    }

    public boolean isInstructionPointerRegister() {
        return teleRegisters.isInstructionPointerRegister(register);
    }

}

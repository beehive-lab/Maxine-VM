/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.tele.debug;

import com.sun.cri.ci.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;


/**
 * Access to the description and current state (within a particular thread) of a register
 * in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleRegister implements MaxRegister {

    private final TeleVM teleVM;
    private final TeleRegisters teleRegisters;
    private final CiRegister register;
    private final String entityName;
    private final String entityDescription;

    protected TeleRegister(TeleRegisters teleRegisters, CiRegister register, TeleNativeThread teleNativeThread) {
        this.teleVM = teleNativeThread.vm();
        this.teleRegisters = teleRegisters;
        this.register = register;
        this.entityName = "Thread-" + teleNativeThread.localHandle() + " register " + register.name;
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
        return register.name;
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

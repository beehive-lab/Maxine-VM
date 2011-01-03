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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.method.CodeLocation.BytecodeLocation;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;

/**
 * Factory and manager for descriptions of locations in code in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class CodeManager extends AbstractTeleVMHolder implements MaxCodeManager {

    public CodeManager(TeleVM teleVM) {
        super(teleVM);
    }

    public BytecodeLocation createBytecodeLocation(MethodKey methodKey, String description) throws TeleError {
        return CodeLocation.createBytecodeLocation(vm(), methodKey, description);
    }

    public  BytecodeLocation createBytecodeLocation(TeleClassMethodActor teleClassMethodActor, int bytecodePosition, String description) {
        return CodeLocation.createBytecodeLocation(vm(), teleClassMethodActor, bytecodePosition, description);
    }

    public MachineCodeLocation createMachineCodeLocation(Address address, String description) throws TeleError {
        return CodeLocation.createMachineCodeLocation(vm(), address, description);
    }

    public MachineCodeLocation createMachineCodeLocation(Address address, TeleClassMethodActor teleClassMethodActor, int position, String description) throws TeleError {
        return CodeLocation.createMachineCodeLocation(vm(), address, teleClassMethodActor, position, description);
    }

}


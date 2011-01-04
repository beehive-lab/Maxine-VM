/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.amd64;

import com.sun.max.asm.amd64.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.runtime.*;

/**
 * @author Bernd Mathiske
 */
public abstract class AMD64EirABI extends EirABI<AMD64EirRegister> {

    protected AMD64EirABI(TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI) {
        super(AMD64EirRegister.class);
        this.targetABI = targetABI;
    }

    @Override
    public int stackSlotSize() {
        return Longs.SIZE;
    }

    @Override
    public Pool<AMD64EirRegister> registerPool() {
        return AMD64EirRegister.pool();
    }

    @Override
    public final AMD64EirRegister.General integerRegisterActingAs(VMRegister.Role role) {
        final AMD64GeneralRegister64 r = targetABI.registerRoleAssignment.integerRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return AMD64EirRegister.General.from(r);
    }

    @Override
    public final AMD64EirRegister.XMM floatingPointRegisterActingAs(VMRegister.Role role) {
        final AMD64XMMRegister r = targetABI.registerRoleAssignment.floatingPointRegisterActingAs(role);
        if (r == null) {
            return null;
        }
        return AMD64EirRegister.XMM.from(r);
    }

    private TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI;

    protected void initTargetABI(TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> abi) {
        this.targetABI = abi;
    }

    @Override
    public TargetABI<AMD64GeneralRegister64, AMD64XMMRegister> targetABI() {
        return targetABI;
    }
}

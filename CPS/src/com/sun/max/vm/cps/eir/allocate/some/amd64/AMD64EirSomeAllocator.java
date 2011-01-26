/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.eir.allocate.some.amd64;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.allocate.some.*;
import com.sun.max.vm.cps.eir.amd64.*;

/**
 * @author Bernd Mathiske
 */
public final class AMD64EirSomeAllocator extends EirSomeAllocator<AMD64EirRegister> {

    private final PoolSet<AMD64EirRegister> noRegisters = PoolSet.noneOf(AMD64EirRegister.pool());

    @Override
    protected PoolSet<AMD64EirRegister> noRegisters() {
        return noRegisters;
    }

    private final PoolSet<AMD64EirRegister> allocatableIntegerRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableIntegerRegisters() {
        return allocatableIntegerRegisters;
    }

    private final PoolSet<AMD64EirRegister> allocatableFloatingPointRegisters;

    @Override
    protected PoolSet<AMD64EirRegister> allocatableFloatingPointRegisters() {
        return allocatableFloatingPointRegisters;
    }

    public AMD64EirSomeAllocator(EirMethodGeneration methodGeneration) {
        super(methodGeneration);
        final AMD64EirABI abi = (AMD64EirABI) methodGeneration.abi;

        allocatableIntegerRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        allocatableIntegerRegisters.and(AMD64EirRegister.General.poolSet());
        allocatableIntegerRegisters.and(abi.allocatableRegisters());

        allocatableFloatingPointRegisters = PoolSet.allOf(AMD64EirRegister.pool());
        allocatableFloatingPointRegisters.and(AMD64EirRegister.XMM.poolSet());
        allocatableFloatingPointRegisters.and(abi.allocatableRegisters());
    }
}

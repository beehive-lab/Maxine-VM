/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.oracle.max.vm.ext.t1x;

import com.oracle.max.vm.ext.t1x.aarch64.*;
import com.oracle.max.vm.ext.t1x.amd64.*;
import com.oracle.max.vm.ext.t1x.armv7.*;
import com.oracle.max.vm.ext.t1x.riscv64.RISCV64T1XCompilation;
import com.oracle.max.vm.ext.t1x.riscv64.RISCV64T1XCompilationTest;
import com.sun.max.vm.*;

/**
 * Controls the exact subclass of {@link T1XCompilation} that is created.
 */
public class T1XCompilationFactory {

    public T1XCompilation newT1XCompilation(T1X t1x) {
        if (T1X.isAMD64()) {
            return new AMD64T1XCompilation(t1x);
        } else if (T1X.isARM()) {
            return MaxineVM.isHostedTesting() ? new ARMV7T1XCompilationTest(t1x) : new ARMV7T1XCompilation(t1x);
        } else if (T1X.isAARCH64()) {
            return MaxineVM.isHostedTesting() ? new Aarch64T1XCompilationTest(t1x) : new Aarch64T1XCompilation(t1x);
        } else if (T1X.isRISCV64()) {
            return MaxineVM.isHostedTesting() ? new RISCV64T1XCompilationTest(t1x) : new RISCV64T1XCompilation(t1x);
        } else {
            throw T1X.unimplISA();
        }
    }
}

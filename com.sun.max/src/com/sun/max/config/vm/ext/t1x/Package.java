/*
 * Copyright (c) 2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.config.vm.ext.t1x;

import com.sun.max.config.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;

/**
 * T1X compiler.
 */

public class Package extends BootImagePackage {

    public Package() {
        super("com.sun.t1x.**",
                "com.oracle.max.vm.ext.t1x.*");
    }

    public static boolean isT1X(String className) {
        return className.contains("T1X");
    }

    @Override
    public boolean isPartOfMaxineVM(VMConfiguration vmConfiguration) {
        return isT1X(CompilationBroker.optName()) || isT1X(CompilationBroker.baselineName());
    }
}

/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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
package com.oracle.max.vm.ext.t1x.armv7;

import static com.oracle.max.vm.ext.t1x.Package.*;
import static com.sun.max.platform.Platform.*;

import com.sun.max.config.*;
import com.sun.max.lang.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;

public class Package extends BootImagePackage {
    @Override
    public boolean isPartOfMaxineVM(VMConfiguration vmConfiguration) {
        if (platform().isa == ISA.ARM) {
            return isT1X(CompilationBroker.optName()) || isT1X(CompilationBroker.baselineName());
        }
        return false;
    }
}

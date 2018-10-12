/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.jvmti.t1x;

import com.oracle.max.vm.ext.jvmti.t1x.amd64.*;
import com.oracle.max.vm.ext.jvmti.t1x.armv7.*;
import com.oracle.max.vm.ext.t1x.*;

public class JVMTI_T1XCompilationFactory extends T1XCompilationFactory {
    @Override
    public T1XCompilation newT1XCompilation(T1X t1x) {
        if (T1X.isAMD64()) {
            return new JVMTI_AMD64T1XCompilation(t1x);
        } else if (T1X.isARM()) {
            return new JVMTI_ARMV7T1XCompilation(t1x);
        } else {
            throw T1X.unimplISA();
        }
    }

}

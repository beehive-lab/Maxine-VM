/*
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.c1x.vma;

import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.vma.*;
import com.oracle.max.vm.ext.vma.options.*;
import com.sun.cri.ci.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * VMA variant that conditionally chooses {@link VMAT1X} during boot image builds.
 */
public class VMAC1X extends C1X {

    VMAT1X vmaT1X;

    @Override
    public TargetMethod compile(final ClassMethodActor method, boolean isDeopt, boolean install, CiStatistics stats) {
        if (instrument(method)) {
            try {
                return vmaT1X.compile(method, isDeopt, install, stats);
            } catch (CiBailout ex) {
                return super.compile(method, isDeopt, install, stats);
            }
        } else {
            return super.compile(method, isDeopt, install, stats);
        }
    }

    public void setVMAT1X(VMAT1X vmaT1X) {
        this.vmaT1X = vmaT1X;
    }

    private static boolean instrument(final ClassMethodActor method) {
        if (!MaxineVM.isHosted()) {
            return false;
        }
        if (method.isNative()) {
            // problems compiling native stubs
            return false;
        }
        if (Actor.isUnsafe(method.compilee().flags())) {
            return false;
        }
        if (method.isVM()) {
            return false;
        }
        return VMAOptions.instrumentForAdvising(method);
    }

}

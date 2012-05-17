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
package com.oracle.max.vm.ext.vma.handlers.log.vmlog.h;

import com.sun.max.vm.*;
import com.sun.max.vm.log.nat.thread.var.*;
import com.sun.max.vm.thread.*;


public class VMLogNativeThreadVariableVMA extends VMLogNativeThreadVariableUnbound {
    public static final String VMA_RECORD_NAME = "VMA_RECORD";
    public static final String VMA_BUFFER_NAME = "VMA_BUFFER";
    public static final String VMA_BUFFER_OFFSETS_NAME = "VMA_BUFFER_OFFSETS";
    public static final VmThreadLocal VMA_RECORD = new VmThreadLocal(VMA_RECORD_NAME, true, "VMA Record");
    public static final VmThreadLocal VMA_BUFFER = new VmThreadLocal(VMA_BUFFER_NAME, false, "VMA buffer");
    public static final VmThreadLocal VMA_BUFFER_OFFSETS = new VmThreadLocal(VMA_BUFFER_OFFSETS_NAME, false, "VMA buffer first/next offsets");

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted() && phase == MaxineVM.Phase.BOOTSTRAPPING) {
            setNativeRecordThreadLocal(VMA_RECORD);
            setBufferThreadLocals(VMA_BUFFER, VMA_BUFFER_OFFSETS);
        }
    }

}

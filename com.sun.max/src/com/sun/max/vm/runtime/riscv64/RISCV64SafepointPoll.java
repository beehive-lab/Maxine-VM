/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package com.sun.max.vm.runtime.riscv64;

import static com.sun.max.platform.Platform.*;

import com.oracle.max.asm.target.riscv64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.*;

/**
 * The safepoint poll implementation for RISCV64.
 *
 * @see RISCV64TrapFrameAccess
 */
public final class RISCV64SafepointPoll extends SafepointPoll {

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final CiRegister LATCH_REGISTER = RISCV64.LATCH_REGISTER;

    @HOSTED_ONLY
    public RISCV64SafepointPoll() {
    }

    @HOSTED_ONLY
    @Override
    protected byte[] createCode() {
        final RISCV64MacroAssembler asm = new RISCV64MacroAssembler(target(), null);
        asm.ldru(64, LATCH_REGISTER, RISCV64Address.createBaseRegisterOnlyAddress(LATCH_REGISTER));
        return asm.codeBuffer.close(true);
    }
}

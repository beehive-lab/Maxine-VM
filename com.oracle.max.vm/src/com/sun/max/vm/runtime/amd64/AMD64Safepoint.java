/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime.amd64;

import static com.sun.max.platform.Platform.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.runtime.*;

/**
 * The safepoint implementation for AMD64 defines the safepoint code that is injected at safepoint sites,
 * as well as the {@linkplain #latchRegister() latch register}, and the layout and size of a trap state
 * area. A trap state area contains the {@linkplain Trap.Number trap number} and the values of the
 * processor's registers when a trap occurs. A trap state area is embedded in each trap stub's frame as follows:
 *
 * <pre>
 *   <-- stack grows downward                       higher addresses -->
 * |---- normal trap stub frame ---- | ---- trap state area --- | RIP |==== stack as it was when trap occurred ===>
 *                                   |<---  TRAP_STATE_SIZE --->|<-8->|
 *
 *                                   ^ trapState
 * </pre>
 * The layout of the trap state area is described by the following C-like struct declaration:
 * <pre>
 * trap_state {
 *     Word generalPurposeRegisters[16];
 *     DoubleWord xmmRegisters[16];
 *     Word trapNumber;
 *     Word flagsRegister;
 * }
 *
 * trap_state_with_rip {
 *     trap_state ts;
 *     Word trapInstructionPointer;
 * }
 * </pre>
 *
 * The fault address is stored in the RIP slot, making this frame appear as if the trap location
 * called the trap stub directly.
 */
public final class AMD64Safepoint extends Safepoint {

    /**
     * ATTENTION: must be callee-saved by all C ABIs in use.
     */
    public static final CiRegister LATCH_REGISTER = AMD64.r14;

    @HOSTED_ONLY
    public AMD64Safepoint() {
    }

    @HOSTED_ONLY
    @Override
    protected byte[] createCode() {
        final AMD64Assembler asm = new AMD64Assembler(target(), null);
        asm.movq(LATCH_REGISTER, new CiAddress(CiKind.Word, LATCH_REGISTER.asValue()));
        return asm.codeBuffer.close(true);
    }
}

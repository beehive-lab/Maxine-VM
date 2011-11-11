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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Encapsulates the values of the integer (or general purpose) registers for a tele native thread.
 */
public final class TeleIntegerRegisters extends TeleRegisters {

    private final CiRegister indirectCallRegister;
    private final CiRegister fp;
    private final CiRegister sp;

    public static CiRegister[] getIntegerRegisters() {
        if (platform().isa == ISA.AMD64) {
            return AMD64.cpuRegisters;
        }
        throw FatalError.unimplemented();
    }

    public TeleIntegerRegisters(TeleVM vm, TeleRegisterSet teleRegisterSet) {
        super(vm, teleRegisterSet, getIntegerRegisters());
        if (platform().isa == ISA.AMD64) {
            indirectCallRegister = AMD64.rax;
            sp = AMD64.rsp;
            fp = AMD64.rbp;
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * Returns the value of the register that is used to make indirect calls.
     *
     * @return null if there is no fixed register used to for indirect calls on the target platform
     */
    Address getCallRegisterValue() {
        if (indirectCallRegister == null) {
            return null;
        }
        return getValue(indirectCallRegister);
    }

    /**
     * Returns the value of the register that is used as the stack pointer.
     *
     * @return the current stack pointer
     */
    Pointer stackPointer() {
        return getValue(sp).asPointer();
    }

    /**
     * Returns the value of the register that is used as the frame pointer.
     *
     * @return the current frame pointer
     */
    Pointer framePointer() {
        return getValue(fp).asPointer();
    }
}

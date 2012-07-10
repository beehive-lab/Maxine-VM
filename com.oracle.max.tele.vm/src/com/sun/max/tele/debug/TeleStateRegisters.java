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

import com.sun.cri.ci.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

/**
 * Encapsulates the values of the state registers for a tele native thread.
 */
public final class TeleStateRegisters extends TeleRegisters {

    private final CiRegister instructionPointerRegister;
    private final CiRegister flagsRegister;

    static class AMD64 {
        static CiRegister RIP   = new CiRegister(0, 0, -1, "rip");
        static CiRegister FLAGS = new CiRegister(1, 1, -1, "flags");
        private static char[] flagNames = {
            'C', '1', 'P', '3', 'A', '5', 'Z', 'S',
            'T', 'I', 'D', 'O', 'I', 'L', 'N', 'F',
            'R', 'V', 'a', 'f', 'p', 'i', '2', '3',
            '4', '5', '6', '7', '8', '9', '0', '1'
        };

        private static final int USED_FLAGS = 22;

        public static String flagsToString(long flags) {
            final char[] chars = new char[USED_FLAGS];
            long f = flags;
            int charIndex = chars.length - 1;
            for (int i = 0; i < USED_FLAGS; i++) {
                if ((f & 1) != 0) {
                    chars[charIndex--] = flagNames[i];
                } else {
                    chars[charIndex--] = '_';
                }
                f >>>= 1;
            }
            return new String(chars);
        }
    }

    static CiRegister[] createStateRegisters() {
        if (platform().isa == ISA.AMD64) {
            return new CiRegister[] {AMD64.RIP, AMD64.FLAGS};
        }
        throw TeleError.unimplemented();
    }

    public TeleStateRegisters(TeleVM vm, TeleRegisterSet teleRegisterSet) {
        super(vm, teleRegisterSet, createStateRegisters());
        if (platform().isa == ISA.AMD64) {
            instructionPointerRegister = AMD64.RIP;
            flagsRegister = AMD64.FLAGS;
        } else {
            throw TeleError.unimplemented();
        }
    }

    /**
     * Gets the value of the instruction pointer.
     *
     * @return the value of the instruction pointer
     */
    Pointer instructionPointer() {
        return getValue(instructionPointerRegister).asPointer();
    }

    /**
     * Updates the value of the instruction point register in this cache. The update to the actual instruction pointer
     * in the remote process must be done by the caller of this method.
     *
     * @param value the new value of the instruction pointer
     */
    void setInstructionPointer(Address value) {
        setValue(instructionPointerRegister, value);
    }

    @Override
    boolean isInstructionPointerRegister(CiRegister register) {
        return register == instructionPointerRegister;
    }

    @Override
    boolean isFlagsRegister(CiRegister register) {
        return register == flagsRegister;
    }

    public static String flagsToString(MaxVM vm, long flags) {
        if (platform().isa == ISA.AMD64) {
            return AMD64.flagsToString(flags);
        }
        throw TeleError.unimplemented();
    }
}

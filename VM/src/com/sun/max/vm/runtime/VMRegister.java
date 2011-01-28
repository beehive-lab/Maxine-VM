/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

import static com.sun.cri.bytecode.Bytecodes.*;

import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.type.*;

/**
 * Direct access to certain CPU registers of the current thread, directed by ABI-managed register roles.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class VMRegister {

    private VMRegister() {
    }

    /**
     * Constant corresponding to the ordinal of {@link Role#CPU_STACK_POINTER}.
     */
    public static final int CPU_SP = 0;

    /**
     * Constant corresponding to the ordinal of {@link Role#CPU_FRAME_POINTER}.
     */
    public static final int CPU_FP = 1;

    /**
     * Constant corresponding to the ordinal of {@link Role#ABI_STACK_POINTER}.
     */
    public static final int ABI_SP = 2;

    /**
     * Constant corresponding to the ordinal of {@link Role#ABI_FRAME_POINTER}.
     */
    public static final int ABI_FP = 3;

    /**
     * Constant corresponding to the ordinal of {@link Role#SAFEPOINT_LATCH}.
     */
    public static final int LATCH = 7;

    /**
     * Constant corresponding to the ordinal of {@link Role#LINK_ADDRESS}.
     */
    public static final int LINK = 9;

    public enum Role {
        /**
         * The register that is customarily used as "the stack pointer" on the target CPU.
         * Typically this register is not flexibly allocatable for other uses.
         * AMD64: RSP
         */
        CPU_STACK_POINTER(CPU_SP) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },
        /**
         * The register that is customarily used as "frame pointer" on the target CPU.
         * AMD64: RBP
         */
        CPU_FRAME_POINTER(CPU_FP) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },
        /**
         * The register that the current target ABI actually uses as stack pointer,
         * i.e. for code sequences that call, push, pop etc.
         * Typically this is the same as CPU_STACK_POINTER.
         */
        ABI_STACK_POINTER(ABI_SP) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },
        /**
         * The register that the current target ABI actually uses as frame pointer,
         * i.e. for code sequences that access local variables, spill slots, stack parameters, etc.
         * This may or may not be the same as CPU_FRAME_POINTER.
         * For the JIT it is, but the optimizing compiler uses CPU_STACK_POINTER instead.
         */
        ABI_FRAME_POINTER(ABI_FP) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },

        /**
         * The register that callees use to return a value.
         */
        ABI_RETURN,

        /**
         * The register where the caller sees the returned value.
         * On most architecture, this is the same as the ABI_RETURN register.
         * On architecture with register windows, this may be a different register
         * (e.g., on SPARC, wherein ABI_RETURN would be assign %i0, whereas ABI_RESULT would
         * be assigned %o0).
         */
        ABI_RESULT,

        ABI_SCRATCH,
        SAFEPOINT_LATCH(LATCH) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },
        /**
         * The register used as the base pointer from which literal offset are computed.
         * Not all platform defines one.
         */
        LITERAL_BASE_POINTER {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }

        },
        /**
         * The register holding the address to which a call returns (e.g. {@code %i7 on SPARC}).
         * Not all platform defines one.
        */
        LINK_ADDRESS(LINK) {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        };

        Role(int expectedOrdinal) {
            assert expectedOrdinal == ordinal();
        }

        Role() {
        }

        public static final List<Role> VALUES = Arrays.asList(values());

        public Kind kind() {
            return null;
        }
    }

    @INLINE
    @INTRINSIC(READREG | (CPU_SP << 8))
    public static native Pointer getCpuStackPointer();

    @INLINE
    @INTRINSIC(WRITEREG | (CPU_SP << 8))
    public static native void setCpuStackPointer(Word value);

    @INLINE
    @INTRINSIC(READREG | (CPU_FP << 8))
    public static native Pointer getCpuFramePointer();

    @INLINE
    @INTRINSIC(WRITEREG | (CPU_FP << 8))
    public static native void setCpuFramePointer(Word value);

    @INLINE
    public static void addWordsToAbiStackPointer(int numberOfWords) {
        SpecialBuiltin.adjustJitStack(numberOfWords);
    }

    @INLINE
    @INTRINSIC(READREG | (ABI_SP << 8))
    public static native Pointer getAbiStackPointer();

    @INLINE
    @INTRINSIC(WRITEREG | (ABI_SP << 8))
    public static native void setAbiStackPointer(Word value);

    @INLINE
    @INTRINSIC(READREG | (ABI_FP << 8))
    public static native Pointer getAbiFramePointer();

    @INLINE
    @INTRINSIC(WRITEREG | (ABI_FP << 8))
    public static native void setAbiFramePointer(Word value);

    @INLINE
    @INTRINSIC(READREG | (LATCH << 8))
    public static native Pointer getSafepointLatchRegister();

    @INLINE
    @INTRINSIC(WRITEREG | (LATCH << 8))
    public static native void setSafepointLatchRegister(Word value);

    @INLINE
    @INTRINSIC(WRITEREG | (LINK << 8))
    public static native void setCallAddressRegister(Word value);
}

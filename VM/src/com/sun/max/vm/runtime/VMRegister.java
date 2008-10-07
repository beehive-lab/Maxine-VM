/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=4b00e572-aedc-4ae1-ade0-f3d4cdb5e1a4*/
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
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

    public enum Role {
        /**
         * The register that is customarily used as "the stack pointer" on the target CPU.
         * Typically this register is not flexibly allocatable for other uses.
         * AMD64: RSP
         */
        CPU_STACK_POINTER {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },
        /**
         * The register that is customarily used as "frame pointer" on the target CPU.
         * AMD64: RBP
         */
        CPU_FRAME_POINTER {
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
        ABI_STACK_POINTER {
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
        ABI_FRAME_POINTER {
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
        SAFEPOINT_LATCH {
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
         * The register holding the address of the call instruction
         * Not all platform defines one.
        */
        CALL_INSTRUCTION_ADDRESS {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        },

            /**
             * The register holding the address of the call instruction for a frame-less call.
             * Not all platform defines one.
             */
        FRAMELESS_CALL_INSTRUCTION_ADDRESS {
            @Override
            public Kind kind() {
                return Kind.WORD;
            }
        };

        public static final IndexedSequence<Role> VALUES = new ArraySequence<Role>(values());

        public Kind kind() {
            return null;
        }
    }

    @INLINE
    public static Pointer getCpuStackPointer() {
        return SpecialBuiltin.getIntegerRegister(Role.CPU_STACK_POINTER);
    }

    @INLINE
    public static void setCpuStackPointer(Word value) {
        SpecialBuiltin.setIntegerRegister(Role.CPU_STACK_POINTER, value);
    }

    @INLINE
    public static Pointer getCpuFramePointer() {
        return SpecialBuiltin.getIntegerRegister(Role.CPU_FRAME_POINTER);
    }

    @INLINE
    public static void setCpuFramePointer(Word value) {
        SpecialBuiltin.setIntegerRegister(Role.CPU_FRAME_POINTER, value);
    }

    @INLINE
    public static void addWordsToAbiStackPointer(int numberOfWords) {
        SpecialBuiltin.addWordsToIntegerRegister(Role.ABI_STACK_POINTER, numberOfWords);
    }

    @INLINE
    public static Pointer getAbiStackPointer() {
        return SpecialBuiltin.getIntegerRegister(Role.ABI_STACK_POINTER);
    }

    @INLINE
    public static void setAbiStackPointer(Word value) {
        SpecialBuiltin.setIntegerRegister(Role.ABI_STACK_POINTER, value);
    }

    @INLINE
    public static Pointer getAbiFramePointer() {
        return SpecialBuiltin.getIntegerRegister(Role.ABI_FRAME_POINTER);
    }

    @INLINE
    public static void setAbiFramePointer(Word value) {
        SpecialBuiltin.setIntegerRegister(Role.ABI_FRAME_POINTER, value);
    }

    @INLINE
    public static Pointer getSafepointLatchRegister() {
        return SpecialBuiltin.getIntegerRegister(Role.SAFEPOINT_LATCH);
    }

    @INLINE
    public static void setSafepointLatchRegister(Word value) {
        SpecialBuiltin.setIntegerRegister(Role.SAFEPOINT_LATCH, value);
    }

    @INLINE
    public static Pointer getInstructionPointer() {
        return SpecialBuiltin.getInstructionPointer();
    }

    @FOLD
    private static boolean callAddressRegisterExists() {
        return VMConfiguration.target().targetABIsScheme().optimizedJavaABI().registerRoleAssignment().integerRegisterActingAs(Role.CALL_INSTRUCTION_ADDRESS) != null;
    }

    @INLINE
    public static Pointer getCallAddressRegister() {
        if (callAddressRegisterExists()) {
            return SpecialBuiltin.getIntegerRegister(Role.CALL_INSTRUCTION_ADDRESS);
        }
        throw ProgramError.unexpected("There is no call address register in this target ABI");
    }

    @INLINE
    public static void setCallAddressRegister(Word value) {
        if (callAddressRegisterExists()) {
            SpecialBuiltin.setIntegerRegister(Role.CALL_INSTRUCTION_ADDRESS, value);
            return;
        }
        throw ProgramError.unexpected("There is no call address register in this target ABI");
    }

    @FOLD
    private static boolean framelessCallAddressRegisterExists() {
        return VMConfiguration.target().targetABIsScheme().optimizedJavaABI().registerRoleAssignment().integerRegisterActingAs(Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS) != null;
    }

    @INLINE
    public static Pointer getFramelessCallAddressRegister() {
        if (callAddressRegisterExists()) {
            return SpecialBuiltin.getIntegerRegister(Role.FRAMELESS_CALL_INSTRUCTION_ADDRESS);
        }
        throw ProgramError.unexpected("There is no frameless call address register in this target ABI");
    }
}

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
package com.sun.max.tele.debug;

import static com.sun.max.platform.Platform.*;

import com.sun.cri.ci.*;
import com.sun.max.asm.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;

/**
 * Encapsulates the values of the state registers for a tele native thread.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 * @author Michael Van De Vanter
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
        throw FatalError.unimplemented();
    }

    public TeleStateRegisters(TeleVM teleVM, TeleRegisterSet teleRegisterSet) {
        super(teleVM, teleRegisterSet, createStateRegisters());
        if (platform().isa == ISA.AMD64) {
            instructionPointerRegister = AMD64.RIP;
            flagsRegister = AMD64.FLAGS;
        } else {
            throw FatalError.unimplemented();
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

    public static String flagsToString(TeleVM teleVM, long flags) {
        if (platform().isa == ISA.AMD64) {
            return AMD64.flagsToString(flags);
        }
        throw FatalError.unimplemented();
    }
}

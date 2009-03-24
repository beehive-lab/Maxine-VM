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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;

/**
 * Encapsulates the values of the state registers for a tele native thread.
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class TeleStateRegisters extends TeleRegisters {

    private final Symbol _instructionPointerRegister;
    private final Symbol _flagsRegister;

    public TeleStateRegisters(VMConfiguration vmConfiguration) {
        super(symbolizer(vmConfiguration), vmConfiguration);
        switch (vmConfiguration.platform().processorKind().instructionSet()) {
            case AMD64: {
                _instructionPointerRegister = Amd64StateRegister.RIP;
                _flagsRegister = Amd64StateRegister.FLAGS;
                break;
            }
            case SPARC: {
                _instructionPointerRegister = SparcStateRegister.PC;
                _flagsRegister = SparcStateRegister.CCR;
                break;
            }
            default: {
                throw Problem.unimplemented();
            }
        }
    }

    /**
     * Gets the value of the instruction pointer.
     *
     * @return the value of the instruction pointer
     */
    public Pointer instructionPointer() {
        return get(_instructionPointerRegister).asPointer();
    }

    /**
     * Updates the value of the instruction point register in this cache. The update to the actual instruction pointer
     * in the remote process must be done by the caller of this method.
     *
     * @param value the new value of the instruction pointer
     */
    public void setInstructionPointer(Address value) {
        set(_instructionPointerRegister, value);
    }

    private enum Amd64StateRegister implements Enumerable<Amd64StateRegister> {

        RIP, FLAGS;

        public int value() {
            return ordinal();
        }

        static final Enumerator<Amd64StateRegister> ENUMERATOR = new Enumerator<Amd64StateRegister>(Amd64StateRegister.class);

        public Enumerator<Amd64StateRegister> enumerator() {
            return ENUMERATOR;
        }

        private static char[] _flagNames = {
            'C', '1', 'P', '3', 'A', '5', 'Z', 'S',
            'T', 'I', 'D', 'O', 'I', 'L', 'N', 'F',
            'R', 'V', 'a', 'f', 'p', 'i', '2', '3',
            '4', '5', '6', '7', '8', '9', '0', '1'
        };

        private static final int _USED_FLAGS = 22;

        public static String flagsToString(long flags) {
            final char[] chars = new char[_USED_FLAGS];
            long f = flags;
            int charIndex = chars.length - 1;
            for (int i = 0; i < _USED_FLAGS; i++) {
                if ((f & 1) != 0) {
                    chars[charIndex--] = _flagNames[i];
                } else {
                    chars[charIndex--] = '_';
                }
                f >>>= 1;
            }
            return new String(chars);
        }
    }

    private enum SparcStateRegister implements Enumerable<SparcStateRegister> {
        CCR, PC,  NPC;
        public int value() {
            return ordinal();
        }
        static final Enumerator<SparcStateRegister>  ENUMERATOR = new Enumerator<SparcStateRegister>(SparcStateRegister.class);
        public Enumerator<SparcStateRegister> enumerator() {
            return ENUMERATOR;
        }
        // TODO
        public static String flagsToString(long flags) {
            return "";
        }
    }

    /**
     * Gets the symbols representing all the state registers of the instruction set denoted by a given VM configuration.
     */
    public static Symbolizer<? extends Symbol> symbolizer(VMConfiguration vmConfiguration) {
        switch (vmConfiguration.platform().processorKind().instructionSet()) {
            case AMD64:
                return Amd64StateRegister.ENUMERATOR;
            case SPARC:
                return SparcStateRegister.ENUMERATOR;
            default:
                throw Problem.unimplemented();
        }
    }

    public boolean isInstructionPointerRegister(Symbol register) {
        return register == _instructionPointerRegister;
    }

    public boolean isFlagsRegister(Symbol register) {
        return register == _flagsRegister;
    }

    public static String flagsToString(TeleVM teleVM, long flags) {
        switch (teleVM.vmConfiguration().platform().processorKind().instructionSet()) {
            case AMD64: {
                return Amd64StateRegister.flagsToString(flags);
            }
            case SPARC: {
                return SparcStateRegister.flagsToString(flags);
            }
            default: {
                throw Problem.unimplemented();
            }
        }
    }
}

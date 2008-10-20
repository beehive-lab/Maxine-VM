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
package com.sun.max.asm.dis;

import com.sun.max.asm.gen.*;

/**
 * Mixin delegation style (for lack of multiple class inheritance in the Java(TM) Programming Language).
 *
 * @author Bernd Mathiske
 */
public interface Address32Instruction extends AddressInstruction {

    int address();

    public static class Mixin implements Address32Instruction {

        private final int _startAddress;
        private final DisassembledInstruction _disassembledInstruction;

        public Mixin(DisassembledInstruction disassembledInstruction, int startAddress) {
            _startAddress = startAddress;
            _disassembledInstruction = disassembledInstruction;
        }

        public int address() {
            return _startAddress + startPosition();
        }

        public int startPosition() {
            return _disassembledInstruction.startPosition();
        }

        public String addressString() {
            return String.format("0x%08X", address());
        }

        public int addressToPosition(ImmediateArgument argument) {
            final int argumentAddress = (int) argument.asLong();
            return argumentAddress - _startAddress;
        }
    }

}

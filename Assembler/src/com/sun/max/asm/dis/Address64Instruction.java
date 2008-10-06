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
/*VCSID=9aa886ec-74b0-4727-b53b-27d5b34fe87e*/
package com.sun.max.asm.dis;

import com.sun.max.asm.gen.*;

/**
 * Mixin delegation style (for lack of multiple class inheritance in the Java(TM) Programming Language).
 *
 * @author Bernd Mathiske
 */
public interface Address64Instruction extends AddressInstruction {

    long address();

    public static class Mixin implements Address64Instruction {

        private final long _startAddress;
        private final DisassembledInstruction _disassembledInstruction;

        public Mixin(DisassembledInstruction disassembledInstruction, long startAddress) {
            _startAddress = startAddress;
            _disassembledInstruction = disassembledInstruction;
        }

        public long address() {
            return _startAddress + startPosition();
        }

        public int startPosition() {
            return _disassembledInstruction.startPosition();
        }

        public String addressString() {
            return String.format("0x%016X", address());
        }

        public int addressToPosition(ImmediateArgument argument) {
            final long argumentAddress = argument.asLong();
            final long result = argumentAddress - _startAddress;
            if (result > Integer.MAX_VALUE) {
                return -1;
            }
            return (int) result;
        }
    }

}

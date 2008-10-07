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
/*VCSID=d50a56b5-3cd4-4d2f-862c-d3ea05e743a7*/
package com.sun.max.asm.sparc.complete;

import com.sun.max.asm.*;
import com.sun.max.asm.AssemblyObject.*;
import com.sun.max.lang.*;

/**
 * The concrete class for a 32-bit SPARC assembler.

 * @author Bernd Mathiske
 */
public class SPARC32Assembler extends SPARCAssembler implements Assembler32 {

    public SPARC32Assembler() {
    }

    private int _startAddress; // address of first instruction

    public SPARC32Assembler(int startAddress) {
        _startAddress = startAddress;
    }

    @Override
    public WordWidth wordWidth() {
        return WordWidth.BITS_32;
    }

    public int startAddress() {
        return _startAddress;
    }

    public void setStartAddress(int address) {
        _startAddress = address;
    }

    @Override
    public long baseAddress() {
        return _startAddress;
    }

    public void fixLabel(Label label, int address) {
        fixLabel32(label, address);
    }

    public int address(Label label) throws AssemblyException {
        return address32(label);
    }

    private SPARC32Directives _directives = new SPARC32Directives();

    @Override
    public Directives directives() {
        return _directives;
    }

    public class SPARC32Directives extends Directives {
        @Override
        public byte padByte() {
            return 0;
        }

        @Override
        protected Type padByteType() {
            return Type.DATA;
        }
    }
}

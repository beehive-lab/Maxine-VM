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
/*VCSID=7eddc65a-9761-4cdc-9eec-2675d8495c0a*/

package com.sun.max.asm.arm;

import com.sun.max.asm.*;
import com.sun.max.asm.AssemblyObject.Type;
import com.sun.max.lang.*;

/**
 *
 * @author Sumeet Panchal
 */
public abstract class AbstractARMAssembler extends BigEndianAssembler implements Assembler32 {

    private int _startAddress; // address of first instruction

    public AbstractARMAssembler(int startAddress) {
        _startAddress = startAddress;
    }

    public AbstractARMAssembler() {
    }

    @Override
    protected void emitPadding(int numberOfBytes) throws AssemblyException {
        if ((numberOfBytes % 4) != 0) {
            throw new AssemblyException("Cannot pad instruction stream with a number of bytes not divisble by 4");
        }
        for (int i = 0; i < numberOfBytes / 4; i++) {
            emitInt(0);
        }
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

    @Override
    public final InstructionSet instructionSet() {
        return InstructionSet.ARM;
    }

    @Override
    public WordWidth wordWidth() {
        return WordWidth.BITS_32;
    }

    private ARMDirectives _directives = new ARMDirectives();

    @Override
    public Directives directives() {
        return _directives;
    }

    public class ARMDirectives extends Directives {
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

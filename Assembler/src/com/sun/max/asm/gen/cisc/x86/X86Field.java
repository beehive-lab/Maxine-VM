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
/*VCSID=cd2c36ce-369c-4b9d-b685-58b30dd0af30*/
package com.sun.max.asm.gen.cisc.x86;


/**
 * @author Bernd Mathiske
 */
public final class X86Field {

    private final int _shift;
    private final int _mask;

    private X86Field(int shift, int width) {
        _shift = shift;
        _mask = ~(0xffffffff << width);
    }

    public int shift() {
        return _shift;
    }

    public int extract(byte b) {
        return (b >> _shift) & _mask;
    }

    public int inPlace(byte value) {
        return value << _shift;
    }

    public static final X86Field RM = new X86Field(0, 3);
    public static final X86Field REG = new X86Field(3, 3);
    public static final X86Field MOD = new X86Field(6, 2);

    public static final X86Field BASE = new X86Field(0, 3);
    public static final X86Field INDEX = new X86Field(3, 3);
    public static final X86Field SCALE = new X86Field(6, 2);

    public static final int REX_B_BIT_INDEX = 0;
    public static final int REX_X_BIT_INDEX = 1;
    public static final int REX_R_BIT_INDEX = 2;
    public static final int REX_W_BIT_INDEX = 3;

    public static int extractRexValue(int rexBitIndex, byte rexByte) {
        return ((rexByte >> rexBitIndex) & 1) << 3;
    }

    public static int inRexPlace(int rexBitIndex, byte rexValue) {
        return ((rexValue >> 3) & 1) << rexBitIndex;
    }
}

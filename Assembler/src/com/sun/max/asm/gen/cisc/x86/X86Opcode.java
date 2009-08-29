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
package com.sun.max.asm.gen.cisc.x86;

import static com.sun.max.util.HexByte.*;

import com.sun.max.util.*;

/**
 * x86 instruction prefix bytes.
 *
 * @author Bernd Mathiske
 */
public final class X86Opcode {

    private X86Opcode() {
    }

    public static final HexByte SEG_ES = _26;
    public static final HexByte SEG_SS = _36;
    public static final HexByte SEG_CS = _2E;
    public static final HexByte SEG_DS = _3E;
    public static final HexByte REX_MIN = _40;
    public static final HexByte REX_MAX = _4F;
    public static final HexByte SEG_FS = _64;
    public static final HexByte SEG_GS = _65;
    public static final HexByte OPERAND_SIZE = _66;
    public static final HexByte ADDRESS_SIZE = _67;
    public static final HexByte FWAIT = _9B;
    public static final HexByte LOCK = _F0;
    public static final HexByte REPNE = _F2;
    public static final HexByte REPE = _F3;

    public static boolean isRexPrefix(HexByte opcode) {
        return X86Opcode.REX_MIN.ordinal() <= opcode.ordinal() && opcode.ordinal() <= X86Opcode.REX_MAX.ordinal();
    }

    public static boolean isFloatingPointEscape(HexByte opcode) {
        switch (opcode) {
            case _D8:
            case _D9:
            case _DA:
            case _DB:
            case _DC:
            case _DD:
            case _DE:
            case _DF:
                return true;
            default:
                return false;
        }
    }

}

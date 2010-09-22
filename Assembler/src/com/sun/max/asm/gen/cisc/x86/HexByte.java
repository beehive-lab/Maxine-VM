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

import java.util.*;

/**
 * Finally: byte literals!
 * 
 * @author Bernd Mathiske
 */
public enum HexByte {
    _00, _01, _02, _03, _04, _05, _06, _07, _08, _09, _0A, _0B, _0C, _0D, _0E, _0F,
    _10, _11, _12, _13, _14, _15, _16, _17, _18, _19, _1A, _1B, _1C, _1D, _1E, _1F,
    _20, _21, _22, _23, _24, _25, _26, _27, _28, _29, _2A, _2B, _2C, _2D, _2E, _2F,
    _30, _31, _32, _33, _34, _35, _36, _37, _38, _39, _3A, _3B, _3C, _3D, _3E, _3F,
    _40, _41, _42, _43, _44, _45, _46, _47, _48, _49, _4A, _4B, _4C, _4D, _4E, _4F,
    _50, _51, _52, _53, _54, _55, _56, _57, _58, _59, _5A, _5B, _5C, _5D, _5E, _5F,
    _60, _61, _62, _63, _64, _65, _66, _67, _68, _69, _6A, _6B, _6C, _6D, _6E, _6F,
    _70, _71, _72, _73, _74, _75, _76, _77, _78, _79, _7A, _7B, _7C, _7D, _7E, _7F,
    _80, _81, _82, _83, _84, _85, _86, _87, _88, _89, _8A, _8B, _8C, _8D, _8E, _8F,
    _90, _91, _92, _93, _94, _95, _96, _97, _98, _99, _9A, _9B, _9C, _9D, _9E, _9F,
    _A0, _A1, _A2, _A3, _A4, _A5, _A6, _A7, _A8, _A9, _AA, _AB, _AC, _AD, _AE, _AF,
    _B0, _B1, _B2, _B3, _B4, _B5, _B6, _B7, _B8, _B9, _BA, _BB, _BC, _BD, _BE, _BF,
    _C0, _C1, _C2, _C3, _C4, _C5, _C6, _C7, _C8, _C9, _CA, _CB, _CC, _CD, _CE, _CF,
    _D0, _D1, _D2, _D3, _D4, _D5, _D6, _D7, _D8, _D9, _DA, _DB, _DC, _DD, _DE, _DF,
    _E0, _E1, _E2, _E3, _E4, _E5, _E6, _E7, _E8, _E9, _EA, _EB, _EC, _ED, _EE, _EF,
    _F0, _F1, _F2, _F3, _F4, _F5, _F6, _F7, _F8, _F9, _FA, _FB, _FC, _FD, _FE, _FF;

    public static final List<HexByte> VALUES = Arrays.asList(values());

    public byte byteValue() {
        return (byte) ordinal();
    }

    public static HexByte fromByte(byte byteValue) {
        return VALUES.get(byteValue & 0xff);
    }

    @Override
    public String toString() {
        return "0x" + name().substring(1);
    }
}

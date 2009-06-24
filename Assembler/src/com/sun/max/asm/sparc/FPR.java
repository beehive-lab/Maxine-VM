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
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;


/**
 * The class defining the symbolic identifiers for the floating-point registers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class FPR extends AbstractSymbolicArgument {

    FPR(int value) {
        super("F" + value, value);
    }

    public static final class Single extends FPR implements SFPR {
        private Single(int value) {
            super(value);
        }
    }

    public static final class Double extends FPR implements DFPR {
        private Double(int value) {
            super(value);
        }
    }

    public static final class SingleDouble extends FPR implements SFPR, DFPR {
        private SingleDouble(int value) {
            super(value);
        }
    }

    public static final class DoubleQuadruple extends FPR implements DFPR, QFPR {
        private DoubleQuadruple(int value) {
            super(value);
        }
    }

    public static final class SingleDoubleQuadruple extends FPR implements SFPR, DFPR, QFPR {
        private SingleDoubleQuadruple(int value) {
            super(value);
        }
    }

    public static final SingleDoubleQuadruple  F0 = new SingleDoubleQuadruple(0);
    public static final Single                 F1 = new Single(1);
    public static final SingleDouble           F2 = new SingleDouble(2);
    public static final Single                 F3 = new Single(3);
    public static final SingleDoubleQuadruple  F4 = new SingleDoubleQuadruple(4);
    public static final Single                 F5 = new Single(5);
    public static final SingleDouble           F6 = new SingleDouble(6);
    public static final Single                 F7 = new Single(7);
    public static final SingleDoubleQuadruple  F8 = new SingleDoubleQuadruple(8);
    public static final Single                 F9 = new Single(9);
    public static final SingleDouble          F10 = new SingleDouble(10);
    public static final Single                F11 = new Single(11);
    public static final SingleDoubleQuadruple F12 = new SingleDoubleQuadruple(12);
    public static final Single                F13 = new Single(13);
    public static final SingleDouble          F14 = new SingleDouble(14);
    public static final Single                F15 = new Single(15);
    public static final SingleDoubleQuadruple F16 = new SingleDoubleQuadruple(16);
    public static final Single                F17 = new Single(17);
    public static final SingleDouble          F18 = new SingleDouble(18);
    public static final Single                F19 = new Single(19);
    public static final SingleDoubleQuadruple F20 = new SingleDoubleQuadruple(20);
    public static final Single                F21 = new Single(21);
    public static final SingleDouble          F22 = new SingleDouble(22);
    public static final Single                F23 = new Single(23);
    public static final SingleDoubleQuadruple F24 = new SingleDoubleQuadruple(24);
    public static final Single                F25 = new Single(25);
    public static final SingleDouble          F26 = new SingleDouble(26);
    public static final Single                F27 = new Single(27);
    public static final SingleDoubleQuadruple F28 = new SingleDoubleQuadruple(28);
    public static final Single                F29 = new Single(29);
    public static final SingleDouble          F30 = new SingleDouble(30);
    public static final Single                F31 = new Single(31);
    public static final       DoubleQuadruple F32 = new DoubleQuadruple(32);
    public static final       Double          F34 = new Double(34);
    public static final       DoubleQuadruple F36 = new DoubleQuadruple(36);
    public static final       Double          F38 = new Double(38);
    public static final       DoubleQuadruple F40 = new DoubleQuadruple(40);
    public static final       Double          F42 = new Double(42);
    public static final       DoubleQuadruple F44 = new DoubleQuadruple(44);
    public static final       Double          F46 = new Double(46);
    public static final       DoubleQuadruple F48 = new DoubleQuadruple(48);
    public static final       Double          F50 = new Double(50);
    public static final       DoubleQuadruple F52 = new DoubleQuadruple(52);
    public static final       Double          F54 = new Double(54);
    public static final       DoubleQuadruple F56 = new DoubleQuadruple(56);
    public static final       Double          F58 = new Double(58);
    public static final       DoubleQuadruple F60 = new DoubleQuadruple(60);
    public static final       Double          F62 = new Double(62);

    private static final FPR[] singleValues = {F0, F1, F2, F3, F4, F5, F6, F7,
                                               F8, F9, F10, F11, F12, F13, F14, F15,
                                               F16, F17, F18, F19, F20, F21, F22, F23,
                                               F24, F25, F26, F27, F28, F29, F30, F31};

    private static final FPR[] doubleValues = {F0, F2, F4, F6, F8, F10, F12, F14, F16, F18, F20, F22, F24, F26, F28, F30,
                                               F32, F34, F36, F38, F40, F42, F44, F46, F48, F50, F52, F54, F56, F58, F60, F62};

    private static final FPR[] quadrupleValues = {F0, F4, F8, F12, F16, F20, F24, F28, F32, F36, F40, F44, F48, F52, F56, F60};

    public static FPR fromValue(int value) {
        if (value <= 31) {
            return singleValues[value];
        }
        if ((value & 0x1) == 0) {
            return doubleValues[value >> 1];
        }
        throw new IllegalArgumentException();
    }
}

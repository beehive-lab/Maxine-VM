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
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * The single-precision (32-bit) floating-point registers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface SFPR extends SymbolicArgument, StaticFieldName {
    SFPR F0 = FPR.F0;
    SFPR F1 = FPR.F1;
    SFPR F2 = FPR.F2;
    SFPR F3 = FPR.F3;
    SFPR F4 = FPR.F4;
    SFPR F5 = FPR.F5;
    SFPR F6 = FPR.F6;
    SFPR F7 = FPR.F7;
    SFPR F8 = FPR.F8;
    SFPR F9 = FPR.F9;
    SFPR F10 = FPR.F10;
    SFPR F11 = FPR.F11;
    SFPR F12 = FPR.F12;
    SFPR F13 = FPR.F13;
    SFPR F14 = FPR.F14;
    SFPR F15 = FPR.F15;
    SFPR F16 = FPR.F16;
    SFPR F17 = FPR.F17;
    SFPR F18 = FPR.F18;
    SFPR F19 = FPR.F19;
    SFPR F20 = FPR.F20;
    SFPR F21 = FPR.F21;
    SFPR F22 = FPR.F22;
    SFPR F23 = FPR.F23;
    SFPR F24 = FPR.F24;
    SFPR F25 = FPR.F25;
    SFPR F26 = FPR.F26;
    SFPR F27 = FPR.F27;
    SFPR F28 = FPR.F28;
    SFPR F29 = FPR.F29;
    SFPR F30 = FPR.F30;
    SFPR F31 = FPR.F31;

    Symbolizer<SFPR> SYMBOLIZER = Symbolizer.Static.initialize(SFPR.class);
}

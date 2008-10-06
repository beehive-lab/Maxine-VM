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
/*VCSID=7e87ef22-28f2-4a30-8538-cd3675fd1669*/
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * The double-precision (64-bit) floating-point registers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface DFPR extends SymbolicArgument, StaticFieldName {

    DFPR F0 = FPR.F0;
    DFPR F2 = FPR.F2;
    DFPR F4 = FPR.F4;
    DFPR F6 = FPR.F6;
    DFPR F8 = FPR.F8;
    DFPR F10 = FPR.F10;
    DFPR F12 = FPR.F12;
    DFPR F14 = FPR.F14;
    DFPR F16 = FPR.F16;
    DFPR F18 = FPR.F18;
    DFPR F20 = FPR.F20;
    DFPR F22 = FPR.F22;
    DFPR F24 = FPR.F24;
    DFPR F26 = FPR.F26;
    DFPR F28 = FPR.F28;
    DFPR F30 = FPR.F30;
    DFPR F32 = FPR.F32;
    DFPR F34 = FPR.F34;
    DFPR F36 = FPR.F36;
    DFPR F38 = FPR.F38;
    DFPR F40 = FPR.F40;
    DFPR F42 = FPR.F42;
    DFPR F44 = FPR.F44;
    DFPR F46 = FPR.F46;
    DFPR F48 = FPR.F48;
    DFPR F50 = FPR.F50;
    DFPR F52 = FPR.F52;
    DFPR F54 = FPR.F54;
    DFPR F56 = FPR.F56;
    DFPR F58 = FPR.F58;
    DFPR F60 = FPR.F60;
    DFPR F62 = FPR.F62;

    Symbolizer<DFPR> SYMBOLIZER = Symbolizer.Static.initialize(DFPR.class);
}

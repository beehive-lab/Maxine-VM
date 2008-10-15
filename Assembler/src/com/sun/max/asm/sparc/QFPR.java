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

import com.sun.max.lang.*;
import com.sun.max.util.*;

/**
 * The quad-precision (128-bit) floating-point registers.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface QFPR extends DFPR, StaticFieldName {
    QFPR F0 = FPR.F0;
    QFPR F4 = FPR.F4;
    QFPR F8 = FPR.F8;
    QFPR F12 = FPR.F12;
    QFPR F16 = FPR.F16;
    QFPR F20 = FPR.F20;
    QFPR F24 = FPR.F24;
    QFPR F28 = FPR.F28;
    QFPR F32 = FPR.F32;
    QFPR F36 = FPR.F36;
    QFPR F40 = FPR.F40;
    QFPR F44 = FPR.F44;
    QFPR F48 = FPR.F48;
    QFPR F52 = FPR.F52;
    QFPR F56 = FPR.F56;
    QFPR F60 = FPR.F60;

    Symbolizer<QFPR> SYMBOLIZER = Symbolizer.Static.initialize(QFPR.class);
}

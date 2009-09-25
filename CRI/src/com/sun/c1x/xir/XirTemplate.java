/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.xir;

import com.sun.c1x.xir.XirAssembler.XirLabel;
import com.sun.c1x.xir.XirAssembler.XirParameter;
import com.sun.c1x.ci.CiRegister;

/**
 * This class represents a completed template of XIR code that has been first assembled by
 * the runtime, and then verified and preprocessed by the compiler.
 */
public class XirTemplate {

    public static int DESTROY = 0x10000000;
    public static int INPUT   = 0x20000000;
    public static int OUTPUT  = 0x40000000;
    public static int FIXED   = 0x80000000;

    public static int HAS_JAVA_CALL = 0x001;
    public static int HAS_STUB_CALL = 0x002;
    public static int HAS_RUNTIME_CALL = 0x004;
    public static int HAS_CONTROL_FLOW = 0x008;
    public static int GLOBAL_STUB = 0x010;

    public final XirAssembler.XirInstruction[] fastPath;
    public final XirAssembler.XirInstruction[] slowPath;
    public final XirLabel[] labels;
    public final XirParameter[] parameters;
    public XirAssembler.XirTemp[] temps;
    public int[] tempFlags;
    public int flags;

    XirTemplate(XirAssembler.XirInstruction[] fastPath, XirAssembler.XirInstruction[] slowPath, XirLabel[] labels, XirParameter[] parameters, int flags) {
        this.fastPath = fastPath;
        this.slowPath = slowPath;
        this.labels = labels;
        this.parameters = parameters;
        this.flags = flags;
    }

    public int getResultParameterIndex() {
        return 1;
    }

    public boolean destroysTemp(int index) {
        return (tempFlags[index] & DESTROY) != 0;
    }

    public boolean inputTemp(int index) {
        return (tempFlags[index] & INPUT) != 0;
    }

    public boolean outputTemp(int index) {
        return (tempFlags[index] & OUTPUT) != 0;
    }

    public boolean inputOutputTemp(int index) {
        return inputTemp(index) && outputTemp(index);
    }

    public boolean fixedTemp(int index) {
        return (tempFlags[index] & FIXED) != 0;
    }

    public CiRegister fixedRegister(int index, CiRegister[] registers) {
        if ((tempFlags[index] & FIXED) != 0) {
            int regnum = tempFlags[index] & 0xfff;
            return registers[regnum];
        }
        return null;
    }

}
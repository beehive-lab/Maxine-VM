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
package com.sun.c1x.lir;

import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;

/**
 * The <code>LIROperandFactory</code> class is a factory for constructing LIROperand
 * instances and provides numerous utility methods.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 *
 */
public class LIROperandFactory {

    public static final LIRLocation IllegalLocation = new LIRLocation(CiKind.Illegal, CiRegister.None);

    public static LIRLocation singleLocation(CiKind type, CiRegister reg) {
        return new LIRLocation(type, reg);
    }

    public static LIRLocation doubleLocation(CiKind type, CiRegister reg1, CiRegister reg2) {
        return new LIRLocation(type, reg1, reg2);
    }

    public static LIRLocation virtualRegister(int index, CiKind type) {
        assert index >= CiRegister.FirstVirtualRegisterNumber : "must start at vregBase";
        return new LIRLocation(type, index);
    }

    public static LIRLocation stack(int index, CiKind type) {
        assert index >= 0;
        return new LIRLocation(type, -index - 1);
    }

    public static LIRConstant intConst(int i) {
        return new LIRConstant(CiConstant.forInt(i));
    }

    public static LIROperand longConst(long l) {
        return new LIRConstant(CiConstant.forLong(l));
    }

    public static LIROperand floatConst(float f) {
        return new LIRConstant(CiConstant.forFloat(f));
    }

    public static LIROperand doubleConst(double d) {
        return new LIRConstant(CiConstant.forDouble(d));
    }

    public static LIROperand oopConst(Object o) {
        return new LIRConstant(CiConstant.forObject(o));
    }

    public static LIROperand illegal() {
        return IllegalLocation;
    }

    public static LIROperand constant(Value type) {
        return new LIRConstant(type.asConstant());
    }

    public static LIROperand address(LIRLocation register, int disp, CiKind t) {
        return new LIRAddress(register, disp, t);
    }

    public static LIROperand address(CiRegister rsp, int disp, CiKind t) {
        return address(new LIRLocation(CiKind.Int, rsp), disp, t);
    }

    public static LIROperand constant(CiConstant value) {
        return new LIRConstant(value);
    }

    public static LIROperand scratch(CiKind type, CiTarget target) {
        return singleLocation(type, target.scratchRegister);
    }
}

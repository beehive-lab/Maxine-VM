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
import com.sun.c1x.util.*;


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

    public static final LIROperand IllegalOperand = LIROperand.ILLEGAL;

    public static LIROperand singleLocation(CiKind type, CiRegister reg) {
        return new LIRLocation(type, reg);
    }

    public static LIROperand doubleLocation(CiKind type, CiRegister reg1, CiRegister reg2) {
        return new LIRLocation(type, reg1, reg2);
    }

    public static LIROperand virtualRegister(int index, CiKind type) {
        assert index >= LIRLocation.virtualRegisterBase() : "must start at vregBase";
        return new LIRLocation(type, index);
    }

    public static LIROperand stack(int index, CiKind type) {
        assert index >= 0;
        return new LIRLocation(type, -index - 1);
    }

    public static LIROperand intConst(int i) {
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

    public static LIROperand intPtrConst(long p) {
        // TODO: address constants in ConstType
        return new LIRConstant(CiConstant.forLong(p));
    }

    public static LIROperand illegal() {
        return LIROperand.ILLEGAL;
    }

    public static LIROperand basicType(Instruction type) {

        if (type.type().isObject()) {
            return oopConst(type.asConstant().asObject());
        } else {
            Constant c = (Constant) type;
            CiConstant ct = c.value;
            if (ct.basicType.isJsr() || ct.basicType.isInt()) {
                return intConst(ct.asInt());
            } else if (ct.basicType.isFloat()) {
                return floatConst(ct.asFloat());
            } else if (ct.basicType.isLong()) {
                return longConst(ct.asLong());
            } else if (ct.basicType.isDouble()) {
                return doubleConst(ct.asDouble());
            } else {
                Util.shouldNotReachHere();
                return LIROperandFactory.intConst(-1);
            }
        }
    }

    public static LIROperand address(LIROperand register, int disp, CiKind t) {
        return new LIRAddress(register, disp, t);
    }

    public static LIROperand address(CiRegister rsp, int disp, CiKind t) {
        return address(new LIRLocation(CiKind.Int, rsp), disp, t);
    }
}

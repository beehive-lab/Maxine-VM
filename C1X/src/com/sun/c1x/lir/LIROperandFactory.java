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

import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


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

    public static final LIROperand illegalOperand = LIROperand.ILLEGAL;

    public static LIROperand singleCpu(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Int, reg);
    }

    public static LIROperand singleCpuOop(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Object, reg);
    }

    public static LIROperand doubleCpu(int reg1, int reg2) {
        assert reg1 > 0 && reg2 > 0;
        return new LIRLocation(BasicType.Long, reg1, reg2, 0);
    }

    public static LIROperand singleFpu(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Float, reg);
    }


    public static LIROperand doubleFpuSparc(int reg1, int reg2) {
        assert reg1 > 0 && reg2 > 0;
        return new LIRLocation(BasicType.Double, reg1, reg2, 0);
    }

    public static LIROperand doubleFpuX86(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Double, reg);
    }

    public static LIROperand singleXmmX86(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Float, reg, LIRLocation.XMM);
    }

    public static LIROperand doubleXmmX86(int reg) {
        assert reg > 0;
        return new LIRLocation(BasicType.Double, reg, LIRLocation.XMM);
    }

    public static LIROperand virtualRegister(int index, BasicType type) {
        assert index >= LIRLocation.virtualRegisterBase() : "must start at vregBase";
        return new LIRLocation(type, index);
    }

    // 'index' is computed by FrameMap.localStackPos(index); do not use other parameters as
    // the index is platform independent; a double stack useing indeces 2 and 3 has always
    // index 2.
    public static LIROperand stack(int index, BasicType type) {
        assert index > 0;
        return new LIRLocation(type, -index);
    }

    public static LIROperand intConst(int i) {
        return new LIRConstant(ConstType.forInt(i));
    }

    public static LIROperand longConst(long l) {
        return new LIRConstant(ConstType.forLong(l));
    }

    public static LIROperand floatConst(float f) {
        return new LIRConstant(ConstType.forFloat(f));
    }

    public static LIROperand doubleConst(double d) {
        return new LIRConstant(ConstType.forDouble(d));
    }

    public static LIROperand oopConst(Object o) {
        return new LIRConstant(ConstType.forObject(o));
    }

    public static LIROperand address(LIROperand a) {
        return a;
    }

    public static LIROperand intPtrConst(long p) {
        // TODO: address constants in ConstType
        return new LIRConstant(ConstType.forLong(p));
    }

    public static LIROperand intptrConst(int v) {
        return new LIRConstant(ConstType.forLong(v));
    }

    public static LIROperand illegal() {
        return LIROperand.ILLEGAL;
    }

    public static LIROperand valueType(ValueType type) {
        if (type instanceof ClassType) {
            ClassType c = (ClassType) type;
            if (!c.isConstant()) {
                return oopConst(null);
            } else {
                return oopConst(type.asConstant().asObject());
            }
        } else {
            assert type instanceof ConstType : "ValueType must be an instance of ConstType";
            ConstType c = (ConstType) type;
            if (c.isJsr() || c.isInt()) {
                return intConst(c.asInt());
            } else if (c.isFloat()) {
                return floatConst(c.asFloat());
            } else if (c.isLong()) {
                return longConst(c.asLong());
            } else if (c.isDouble()) {
                return doubleConst(c.asDouble());
            } else {
                Util.shouldNotReachHere();
                return LIROperandFactory.intConst(-1);
            }
        }
    }

    public static LIROperand dummyValueType(ValueType type) {
        if (type.isObject()) {
            return oopConst(null);
        } else if (type.isInt() | type.isJsr()) {
            return intConst(0);
        } else if (type.isFloat()) {
            return floatConst(type.asConstant().asFloat());
        } else if (type.isLong()) {
            return longConst(type.asConstant().asLong());
        } else if (type.isDouble()) {
            return doubleConst(type.asConstant().asDouble());
        } else {
            Util.shouldNotReachHere();
            return intConst(-1);
        }
    }

        // TODO Auto-generated method stub
    public static LIROperand intptrConst(Address counter) {
        return null;
    }

    public static LIROperand doubleFpu(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    public static LIROperand longConst(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    // TODO to be completed
}

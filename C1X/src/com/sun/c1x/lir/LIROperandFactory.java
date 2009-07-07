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

import com.sun.c1x.lir.LIROperand.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;


/**
 * The <code>LIROperandFactory</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class LIROperandFactory {

    public static LIROperand illegalOperand;

    public static LIROperand singleCpu(int reg) {
        return new LIROperand((reg  << OperandShift.Reg1Shift.value) |
                              OperandType.IntType.value              |
                              OperandKind.CpuRegister.value          |
                              OperandSize.SingleSize.value);
    }

    public static LIROperand singleCpuOop(int reg) {
        return new LIROperand((reg  << OperandShift.Reg1Shift.value) |
                              OperandType.ObjectType.value           |
                              OperandKind.CpuRegister.value          |
                              OperandSize.SingleSize.value);
    }

    public static LIROperand doubleCpu(int reg1, int reg2) {
      // LP64ONLY(assert reg1 == reg2 :  "must be identical"); TODO: Should we do this test or just remove it?
      return new LIROperand((reg1 << OperandShift.Reg1Shift.value) |
                            (reg2 << OperandShift.Reg2Shift.value) |
                            OperandType.LongType.value             |
                            OperandKind.CpuRegister.value          |
                            OperandType.DoubleType.value);
    }

    public static LIROperand singleFpu(int reg) {
        return new  LIROperand((reg  << OperandShift.Reg1Shift.value) |
                                OperandType.FloatType.value           |
                                OperandKind.FpuRegister.value         |
                                OperandSize.SingleSize.value);
    }


    public static LIROperand doubleFpuSparc(int reg1, int reg2) {
        return new LIROperand((reg1 << OperandShift.Reg1Shift.value) |
                              (reg2 << OperandShift.Reg2Shift.value) |
                              OperandType.DoubleType.value           |
                              OperandKind.FpuRegister.value          |
                              OperandType.DoubleType.value);
    }

    public static LIROperand doubleFpuX86(int reg) {
        return new LIROperand((reg << OperandShift.Reg1Shift.value) |
                              (reg << OperandShift.Reg2Shift.value) |
                              OperandType.DoubleType.value          |
                              OperandKind.FpuRegister.value         |
                              OperandType.DoubleType.value);
    }

    public static LIROperand singleXmmX86(int reg) {
        return new LIROperand((reg  << OperandShift.Reg1Shift.value) |
                              OperandType.FloatType.value            |
                              OperandKind.FpuRegister.value          |
                              OperandSize.SingleSize.value           |
                              OperandMask.IsXmmMask.value);
    }

    static LIROperand doubleXmmX86(int reg) {
        return new LIROperand((reg << OperandShift.Reg1Shift.value)  |
                              (reg  << OperandShift.Reg2Shift.value) |
                              OperandType.DoubleType.value           |
                              OperandKind.FpuRegister.value          |
                              OperandType.DoubleType.value           |
                              OperandMask.IsXmmMask.value);
    }

    public static LIROperand virtualRegister(int index, BasicType type) {
        LIROperand res;
        switch (type) {
            case Object: // fall through
                // case Array:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.ObjectType.value            |
                                     OperandKind.CpuRegister.value           |
                                     OperandSize.SingleSize.value            |
                                     OperandMask.VirtualMask.value);
                break;

            case Int:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.IntType.value               |
                                     OperandKind.CpuRegister.value           |
                                     OperandSize.SingleSize.value            |
                                     OperandMask.VirtualMask.value);
                break;

            case Long:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.LongType.value              |
                                     OperandKind.CpuRegister.value           |
                                     OperandType.DoubleType.value            |
                                     OperandMask.VirtualMask.value);
                break;

            case Float:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.FloatType.value             |
                                     OperandKind.FpuRegister.value           |
                                     OperandSize.SingleSize.value            |
                                     OperandMask.VirtualMask.value);
                break;

            case Double:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.DoubleType.value            |
                                     OperandKind.FpuRegister.value           |
                                     OperandType.DoubleType.value            |
                                     OperandMask.VirtualMask.value);
                break;

            default:
                Util.shouldNotReachHere();
                res = illegalOperand;
        }

        res.validateType();
        assert res.vregNumber() == index : "conversion check";
        assert index >= VirtualRegister.RegisterBase.value : "must start at vregBase";
        assert index <= (Integer.MAX_VALUE >> OperandShift.DataShift.value) : "index is too big";

        // old-style calculation; check if old and new method are equal
        OperandType t = LIROperand.asOperandType(type);
        LIROperand oldRes = new LIROperand((index << OperandShift.DataShift.value) |
                                           t.value |
                                           ((type == BasicType.Float || type == BasicType.Double) ? OperandKind.FpuRegister.value : OperandKind.CpuRegister.value) |
                                           LIROperand.sizeFor(type).value |
                                           OperandMask.VirtualMask.value);
        assert res == oldRes : "old and new method not equal";
        return res;
    }

    // 'index' is computed by FrameMap.localStackPos(index); do not use other parameters as
    // the index is platform independent; a double stack useing indeces 2 and 3 has always
    // index 2.
    public static LIROperand stack(int index, BasicType type) {
        LIROperand res;
        switch (type) {
            case Object: // fall through
            //case Array:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.ObjectType.value            |
                                     OperandKind.StackValue.value            |
                                     OperandSize.SingleSize.value);
                break;

            case Int:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.IntType.value               |
                                     OperandKind.StackValue.value            |
                                     OperandSize.SingleSize.value);
                break;

            case Long:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.LongType.value              |
                                     OperandKind.StackValue.value            |
                                     OperandType.DoubleType.value);
                break;

            case Float:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.FloatType.value             |
                                     OperandKind.StackValue.value            |
                                     OperandSize.SingleSize.value);
                break;

            case Double:
                res = new LIROperand((index << OperandShift.DataShift.value) |
                                     OperandType.DoubleType.value            |
                                     OperandKind.StackValue.value            |
                                     OperandType.DoubleType.value);
                break;

            default:
                Util.shouldNotReachHere();
                res = illegalOperand;
      }

      assert index >= 0 :  "index must be positive";
      assert index <= (Integer.MAX_VALUE >> OperandShift.DataShift.value) :  "index is too big";

      LIROperand oldRes = new LIROperand((index << OperandShift.DataShift.value) |
                                         OperandKind.StackValue.value            |
                                         LIROperand.asOperandType(type).value    |
                                         LIROperand.sizeFor(type).value);
      assert res == oldRes :  "old and new method not equal";
      return res;
    }

    public static LIROperand intConst(int i) {
        return new LIRConstant(i);
    }

    public static LIROperand longConst(long l) {
        return new LIRConstant(l);
    }

    public static LIROperand floatConst(float f) {
        return new LIRConstant(f);
    }

    public static LIROperand doubleConst(double d) {
        return new LIRConstant(d);
    }

    public static LIROperand oopConst(Object o) {
        return new LIRConstant(o);
    }

    public static LIROperand address(LIRAddress a) {
        return a;
    }

    public static LIROperand intPtrConst(long p) {
        return new LIRConstant(p);
    }

    public static LIROperand intptrConst(int v) {
        return new LIRConstant((long) v);
    }

    public static LIROperand illegal() {
        return new LIROperand(-1);
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
}

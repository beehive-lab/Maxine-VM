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

import com.sun.c1x.target.*;
import com.sun.c1x.target.sparc.*;
import com.sun.c1x.target.x86.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.c1x.value.BasicType;

/**
 * The <code>LIROperand</code> class definition.
 *
 * @author Marcelo Cintra
 */
public class LIROperand {
    public static int BitsPerWord                            = 64;
    public static int BitsPerInt                             = 32;
    // Number of register in the target machine
    // TODO: need to think better about how to get this information
    //       dinamically, according to the current hardware
    public static int  NumberOfRegisters                     = 16;

    public enum OperandKind {
        PointerValue(0),
        StackValue(1),
        CpuRegister(3),
        FpuRegister(5),
        IllegalValue(7);

        public final int value;

        OperandKind(int value) {
            this.value = value;
        }

        public static OperandKind fromInt(int code) {
            switch (code) {
                case 0:
                    return PointerValue;
                case 1:
                    return StackValue;
                case 3:
                    return CpuRegister;
                case 5:
                    return FpuRegister;
                case 7:
                    return IllegalValue;
            }
            throw new IllegalArgumentException("unknown  type code: " + code);
        }
    }

    public enum OperandBits {
        PointerBits(1),
        KindBits(3),
        TypeBits(4),
        SizeBits(2),
        DestroysBits(1),
        VirtualBits(1),
        IsXmmBits(1),
        LastUseBits(1),
        IsFpuStackOffsetBits(1),
        // used in assertion checking on x86 for FPU stack slot allocation
        NonDataBits(KindBits.value + TypeBits.value + SizeBits.value + DestroysBits.value + LastUseBits.value + IsFpuStackOffsetBits.value + VirtualBits.value + IsXmmBits.value),
        DataBits(BitsPerInt - NonDataBits.value),
        RegBits(DataBits.value / 2); // for two registers in one value encoding

        // in one value encoding

        public final int value;

        private OperandBits(int value) {
            this.value = value;
        }
    }

    public enum OperandShift {
        KindShift(0),
        TypeShift(KindShift.value + OperandBits.KindBits.value),
        SizeShift(TypeShift.value + OperandBits.TypeBits.value),
        DestroysShift(SizeShift.value + OperandBits.SizeBits.value),
        LastUseShift(DestroysShift.value + OperandBits.DestroysBits.value),
        IsFpuStackOffsetShift(LastUseShift.value + OperandBits.LastUseBits.value),
        VirtualShift(IsFpuStackOffsetShift.value + OperandBits.IsFpuStackOffsetBits.value),
        IsXmmShift(VirtualShift.value + OperandBits.VirtualBits.value),
        DataShift(IsXmmShift.value + OperandBits.IsXmmBits.value),
        Reg1Shift(DataShift.value),
        Reg2Shift(DataShift.value + OperandBits.RegBits.value);

        public final int value;

        OperandShift(int value) {
            this.value = value;
        }
    }

    enum OperandSize {
        SingleSize(0),
        DoubleSize(1);

        public final int value;

        private OperandSize(int n) {
            value = n << OperandShift.SizeShift.value;
        }

        public static OperandSize fromInt(int code) {
            switch (code) {
                case 0:
                    return SingleSize;
                case 1:
                    return DoubleSize;
            }
            throw new IllegalArgumentException("unknown operand size code: " + code);
        }
    }

    enum OperandMask {
        KindMask(rightNBits(OperandBits.KindBits.value)),
        TypeMask(rightNBits(OperandBits.TypeBits.value) << OperandShift.TypeShift.value),
        SizeMask(rightNBits(OperandBits.SizeBits.value) << OperandShift.SizeShift.value),
        LastUseMask(rightNBits(OperandBits.LastUseBits.value) << OperandShift.LastUseShift.value),
        IsFpuStackOffsetMask(rightNBits(OperandBits.IsFpuStackOffsetBits.value) << OperandShift.IsFpuStackOffsetShift.value),
        VirtualMask(rightNBits(OperandBits.VirtualBits.value) << OperandShift.VirtualShift.value),
        IsXmmMask(rightNBits(OperandBits.IsXmmBits.value) << OperandShift.IsXmmShift.value),
        PointerMask(rightNBits(OperandBits.PointerBits.value)),
        LowerRegMask(rightNBits(OperandBits.RegBits.value)),
        NoTypeMask((~(TypeMask.value | LastUseMask.value | IsFpuStackOffsetMask.value)));

        public final int value;

        private OperandMask(int value) {
            this.value = value;
        }

        static int rightNBits(int n) {
            return nthBit(n) - 1;
        }


        static int nthBit(int n) {
            return n >= BitsPerWord ? 0 : 1 << (n);
        }

        static int leftNBits(int n) {
            return (rightNBits(n) << (n >= BitsPerWord ? 0 : (BitsPerWord - n)));
        }
    }

    public enum VirtualRegister {
        RegisterBase(NumberOfRegisters),
        MaxRegisters((1 << OperandBits.DataBits.value) - 1);

        public final int value;

        VirtualRegister(int value) {
            this.value = value;
        }
    }

    public enum OperandType {
        UnknownType(0 << OperandShift.TypeShift.value), // means: not set (catch uninitialized types)
        IntType(1 << OperandShift.TypeShift.value),
        LongType(2 << OperandShift.TypeShift.value),
        ObjectType(3 << OperandShift.TypeShift.value),
        PointerType(4 << OperandShift.TypeShift.value),
        FloatType(5 << OperandShift.TypeShift.value),
        DoubleType(6 << OperandShift.TypeShift.value);

        public final int value;

        OperandType(int value) {
            this.value = value;
        }

        public static OperandType fromInt(int value) {
            if (value == UnknownType.value) {
                return UnknownType;
            } else if (value == IntType.value) {
                return IntType;
            } else if (value == LongType.value) {
                return LongType;
            } else if (value == ObjectType.value) {
                return ObjectType;
            } else if (value == PointerType.value) {
                return PointerType;
            } else if (value == FloatType.value) {
                return FloatType;
            } else if (value == DoubleType.value) {
                return DoubleType;
            } else {
                Util.shouldNotReachHere();
            }
            return null;
        }
    }

    public static OperandType asOperandType(BasicType type) {
        switch (type) {
            case Int:
                return OperandType.IntType;
            case Long:
                return OperandType.LongType;
            case Float:
                return OperandType.FloatType;
            case Double:
                return OperandType.DoubleType;
            case Object:
                return OperandType.ObjectType;
            case Illegal: // fall through
            default:
                Util.shouldNotReachHere();
                return OperandType.UnknownType;
        }
    }

    private int value;

    public LIROperand() {
        this.value = 0;
    }

    public LIROperand(int value) {
        this.value = value;
    }

    int value() {
        return value;
    }

    public boolean checkValueMask(int mask, int maskedValue) {
        return (value & mask) == maskedValue;
    }

    public int data() {
        return value >> OperandShift.DataShift.value;
    }

    public int lowerRegisterHalf() {
        return data() & OperandMask.LowerRegMask.value;
    }

    public int higherRegisterHalf() {
        return (data() >> OperandBits.RegBits.value) & OperandMask.LowerRegMask.value;
    }

    public OperandKind kindField() {
        return OperandKind.fromInt(value & OperandMask.KindMask.value);
    }

    public OperandSize sizeField() {
        return OperandSize.fromInt(value & OperandMask.SizeMask.value);
    }


    public static LIROperand illegalOpr() {
        return LIROperandFactory.illegalOperand;
    }

    OperandType typeFieldValid() {
        assert isRegister() || isStack() : "should not be called otherwise";
        return OperandType.fromInt(value() & OperandMask.TypeMask.value);
    }

    OperandType typeField() {
        return isIllegal() ? OperandType.UnknownType : OperandType.fromInt((value() & OperandMask.TypeMask.value));
    }

    public static OperandSize sizeFor(BasicType t) {
        switch (t) {
            case Long:
            case Double:
                return OperandSize.DoubleSize;

            case Float:
            case Boolean:
            case Char:
            case Byte:
            case Short:
            case Int:
            case Object:
                return OperandSize.SingleSize;

            default:
                assert false : "Illegal Basic Type!";
                return OperandSize.SingleSize;
        }
    }

    private BasicType asBasicType(OperandType t) {
        switch (t) {
        case IntType:
            return BasicType.Int;
        case LongType:
            return BasicType.Long;
        case FloatType:   return BasicType.Float;
        case DoubleType:  return BasicType.Double;
        case ObjectType:  return BasicType.Object;
        case UnknownType: // fall through
        default:
            Util.shouldNotReachHere();
            return BasicType.Illegal;
        }
    }

    /**
    *
    */
    void validateType() {
        if (!isPointer() && !isIllegal()) {
            switch (asBasicType(typeField())) {
                case Long:
                    assert (kindField() == OperandKind.CpuRegister || kindField() == OperandKind.StackValue) && sizeField() == OperandSize.DoubleSize : "must match";
                    break;
                case Float:
                    assert (kindField() == OperandKind.CpuRegister || kindField() == OperandKind.StackValue) && sizeField() == OperandSize.SingleSize : "must match";
                    break;
                case Double:
                    assert (kindField() == OperandKind.FpuRegister || kindField() == OperandKind.StackValue) && sizeField() == OperandSize.DoubleSize : "must match";
                    break;
                case Boolean:
                case Char:
                case Byte:
                case Short:
                case Int:
                case Object:
                    // case Array:
                    assert (kindField() == OperandKind.CpuRegister || kindField() == OperandKind.StackValue) && sizeField() == OperandSize.SingleSize : "must match";
                    break;

                case Illegal:
                    // XXX TKR also means unknown right now
                    // assert isIllegal() : "must match";
                    break;

                default:
                    Util.shouldNotReachHere();
            }
        }
    }

    public BasicType type() {
        if (isPointer()) {
            return pointer().type();
        }
        return asBasicType(typeField());
    }

    public ValueType valueType() {
        return ValueType.fromBasicType(type());
    }

    boolean isEqual(LIROperand opr) {
        return this.value() == opr.value();
    }

    // checks whether types are same
    boolean isSameType(LIROperand opr) {
        assert typeField() != OperandType.UnknownType && opr.typeField() != OperandType.UnknownType : "shouldn't see unknownType";
        return typeField() == opr.typeField();
    }

    boolean isSameRegister(LIROperand opr) {
        return (isRegister() && opr.isRegister() && kindField() == opr.kindField() && (value() & OperandMask.NoTypeMask.value) == (opr.value() & OperandMask.NoTypeMask.value));
    }

    public OperandType assertTypeField() {
        assert true;
        return null;
    }

    public boolean isPointer() {
        return checkValueMask(OperandMask.PointerMask.value, OperandKind.PointerValue.value);
    }

    public boolean isIllegal() {
        return kindField() == OperandKind.IllegalValue;
    }

    public boolean isValid() {
        return kindField() != OperandKind.IllegalValue;
    }

    public boolean isRegister() {
        return false; // isCpuRegister() || isFpuRegister();
    }

    public boolean isVirtual() {
        return false; // isVirtualCpu() || isVirtualFpu();
    }

    public boolean isConstant() {
        return isPointer() && false; // pointer()->asConstant() != null;
    }

    public boolean isAddress() {
        return isPointer() && false; // && pointer()->as_address() != NULL;
    }

    public void print(LogStream out) {
        // TODO to be completed later
    }

    boolean isFloatKind() {
        return isPointer() ? pointer().isFloatKind() : (kindField() == OperandKind.FpuRegister);
    }

    boolean isOop() {
        if (isPointer()) {
            return pointer().isOopPointer();
        } else {
            OperandType t = typeField();
            assert t != OperandType.UnknownType : "type not set";
            return t == OperandType.ObjectType;
        }
    }

    // semantic for fpu- and xmm-registers:
    // * isFloat and isDouble return true for xmmRegisters
    // (so isSingleFpu and isSingleXmm are true)
    // * So you must always check for is_???xmm prior to is_???fpu to
    // distinguish between fpu- and xmm-registers

    public boolean isStack() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value, OperandKind.StackValue.value);
    }

    public boolean isSingleStack() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.StackValue.value  | OperandSize.SingleSize.value);
    }

    public boolean isDoubleStack() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.StackValue.value | OperandSize.DoubleSize.value);
    }

    public boolean isCpuRegister() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value, OperandKind.CpuRegister.value);
    }

    public boolean isVirtualCpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.VirtualMask.value, OperandKind.CpuRegister.value | OperandMask.VirtualMask.value);
    }

    public boolean isFixedCpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.VirtualMask.value, OperandKind.CpuRegister.value);
    }

    public boolean isSingleCpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.CpuRegister.value | OperandSize.SingleSize.value);
    }

    public boolean isDoubleCpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.CpuRegister.value | OperandSize.DoubleSize.value);
    }

    public boolean isFpuRegister() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value, OperandKind.FpuRegister.value);
    }

    public boolean isVirtualFpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.VirtualMask.value, OperandKind.FpuRegister.value | OperandMask.VirtualMask.value);
    }

    public boolean isFixedFpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.VirtualMask.value, OperandKind.FpuRegister.value);
    }

    public boolean isSingleFpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.FpuRegister.value | OperandSize.SingleSize.value);
    }

    public boolean isDoubleFpu() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value, OperandKind.FpuRegister.value | OperandSize.DoubleSize.value);
    }

    public boolean isXmmRegister() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.IsXmmMask.value, OperandKind.FpuRegister.value | OperandMask.IsXmmMask.value);
    }

    public boolean isSingleXmm() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value  | OperandMask.IsXmmMask.value, OperandKind.FpuRegister.value | OperandSize.SingleSize.value | OperandMask.IsXmmMask.value);
    }

    public boolean isDoubleXmm() {
        validateType();
        return checkValueMask(OperandMask.KindMask.value | OperandMask.SizeMask.value | OperandMask.IsXmmMask.value, OperandKind.FpuRegister.value | OperandSize.DoubleSize.value | OperandMask.IsXmmMask.value);
    }

    // fast accessor functions for special bits that do not work for pointers
    // (in this functions, the check for isPointer() is omitted)
    public boolean isSingleWord() {
        assert isRegister() || isStack() : "type check";
        return checkValueMask(OperandMask.SizeMask.value, OperandSize.SingleSize.value);
    }

    public boolean isDoubleWord() {
        assert isRegister() || isStack() : "type check";
        return checkValueMask(OperandMask.SizeMask.value, OperandSize.DoubleSize.value);
    }

    public boolean isVirtualRegister() {
        assert isRegister() : "type check";
        return checkValueMask(OperandMask.VirtualMask.value, OperandMask.VirtualMask.value);
    }

    public boolean isOopRegister() {
        assert isRegister() || isStack() : "type check";
        return typeFieldValid() == OperandType.ObjectType;
    }

    public BasicType typeRegister() {
        assert isRegister() || isStack() : "type check";
        return asBasicType(typeFieldValid());
    }

    public boolean isLastUse() {
        assert isRegister() : "only works for registers";
        return (value() & OperandMask.LastUseMask.value) != 0;
    }

    public boolean isFpuStackOffset() {
        assert isRegister() : "only works for registers";
        return (value() & OperandMask.IsFpuStackOffsetMask.value) != 0;
    }

    public LIROperand makeLastUse() {
        assert isRegister() : "only works for registers";
        return new LIROperand(value() | OperandMask.LastUseMask.value);
    }

    public LIROperand makeFpuStackOffset() {
        assert isRegister() : "only works for registers";
        return new LIROperand(value() | OperandMask.IsFpuStackOffsetMask.value);
    }

    public int singleStackIx() {
        assert isSingleStack() && !isVirtual() : "type check";
        return data();
    }

    public int doubleStackIx() {
        assert isDoubleStack() && !isVirtual() : "type check";
        return data();
    }

    public int cpuRegnr() {
        assert isSingleCpu() && !isVirtual() : "type check";
        return  data();
    }

    public int cpuRegnrLo() {
        assert isDoubleCpu() && !isVirtual() : "type check";
        return  lowerRegisterHalf();
    }

    public int cpuRegnrHi() {
        assert isDoubleCpu() && !isVirtual() : "type check";
        return  higherRegisterHalf();
    }

    public int fpuRegnr() {
        assert isSingleFpu() && !isVirtual() : "type check";
        return  data();
    }

    public int fpuRegnrLo() {
        assert isDoubleFpu() && !isVirtual() : "type check";
        return  lowerRegisterHalf();
    }

    public int fpuRegnrHi() {
        assert isDoubleFpu() && !isVirtual() : "type check";
        return  higherRegisterHalf();
    }

    public int xmmRegnr() {
        assert isSingleXmm() && !isVirtual() : "type check";
        return  data();
    }

    public int xmmRegnrLo() {
        assert isDoubleXmm() && !isVirtual() : "type check";
        return  lowerRegisterHalf();
    }

    public int xmmRegnrHi() {
        assert isDoubleXmm() && !isVirtual() : "type check";
        return  higherRegisterHalf();
    }

    public int vregNumber() {
        assert isVirtual() : "type check";
        return data();
    }

    public LIROperand pointer() {
        assert isPointer() : "type check";
        return this;
    }

    public LIRConstant asConstantPtr() {
        return pointer().asConstant();
    }

    public LIRAddress asAddressPtr() {
        return pointer().asAddress();
    }

    public Register asRegister() {
        return FrameMap.cpuRnr2Reg(cpuRegnr());
    }

    public Register asRegisterLo() {
        return FrameMap.cpuRnr2Reg(cpuRegnrLo());
    }

    public Register asRegisterHi() {
        return FrameMap.cpuRnr2Reg(cpuRegnrHi());
    }

    public Register asPointerRegister(Architecture architecture) {

        if (architecture.is64bit() && isDoubleCpu()) {
            assert asRegisterLo() == asRegisterHi() : "should be a single register";
            return asRegisterLo();
        }
        return asRegister();
    }

    // X86 specific

    public XMMRegister asXmmFloatReg() {
        return FrameMap.nr2XmmReg(xmmRegnr());
    }

    public XMMRegister asXmmDoubleReg() {
        assert xmmRegnrLo() == xmmRegnrHi() : "assumed in calculation";
        return FrameMap.nr2XmmReg(xmmRegnrLo());
    }

    // for compatibility with RInfo
    public int fpu() {
        return lowerRegisterHalf();
    }

    // SPARC specific
    public FloatRegister asFloatReg() {
        return FrameMap.nr2FloatReg(fpuRegnr());
    }

    public FloatRegister asDoubleReg() {
        return FrameMap.nr2FloatReg(fpuRegnrHi());
    }

    public int asInt() {
        return asConstantPtr().asInt();
    }

    public long asLong() {
        return asConstantPtr().asLong();
    }

    public float asJfloat() {
        return asConstantPtr().asFloat();
    }

    public double asJdouble() {
        return asConstantPtr().asJdouble();
    }

    public Object asJobject() {
        return asConstantPtr().asObject();
    }

    // methods moved from LIROprPtr in C1
    boolean isOopPointer() {
        return (type() == BasicType.Object);
    }

    boolean isFloat() {
        BasicType t = type();
        return (t == BasicType.Float) || (t == BasicType.Double);
    }

    public LIRConstant asConstant() {
        return null; // TODO merged  from LIROprPtr
    }

    public LIRAddress asAddress() {
        return null; // TODO merged  from LIROprPtr
    }

    // public BasicType type() {
    // Util.shouldNotReachHere();
    // return null;
    // }

    public void printValueOn(LogStream out) {
        Util.shouldNotReachHere(); // TODO merged  from LIROprPtr
    }
}

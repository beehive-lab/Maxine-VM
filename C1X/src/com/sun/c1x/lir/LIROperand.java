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

import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>LIROperand</code> class definition.
 *
 * @author Marcelo Cintra
 */
public class LIROperand {
    public static int BitsPerWord                            = 64;
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

        private final int value;

        OperandKind(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static OperandKind fromInt(int code) {
            switch(code) {
                case 0: return PointerValue;
                case 1: return StackValue;
                case 3: return CpuRegister;
                case 5: return FpuRegister;
                case 7: return IllegalValue;
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
        IsFpuStackOffsetBits(1),        // used in assertion checking on x86 for FPU stack slot allocation
        NonDataBits(KindBits.value() + TypeBits.value() + SizeBits.value() + DestroysBits.value() + LastUseBits.value() + IsFpuStackOffsetBits.value() + VirtualBits.value() + IsXmmBits.value()),
        DataBits (32 - IsXmmBits.value()),
        RegBits(DataBits.value() / 2);      // for two registers in one value encoding

        private final int value;

        private OperandBits(int value) {
          this.value = value;
        }

        public final int value() {
          return value;
        }
    }

    public enum OperandShift {
        KindShift(0),
        TypeShift(KindShift.value() + OperandBits.KindBits.value),
        SizeShift(TypeShift.value + OperandBits.TypeBits.value),
        DestroysShift(SizeShift.value + OperandBits.SizeBits.value),
        LastUseShift(DestroysShift.value + OperandBits.DestroysBits.value),
        IsFpuStackOffsetShift(LastUseShift.value + OperandBits.LastUseBits.value),
        VirtualShift(IsFpuStackOffsetShift.value + OperandBits.IsFpuStackOffsetBits.value),
        IsXmmShift(VirtualShift.value + OperandBits.VirtualBits.value),
        DataShift(IsXmmShift.value + OperandBits.IsXmmBits.value),
        Reg1Shift(DataShift.value),
        Reg2Shift(DataShift.value + OperandBits.RegBits.value);

      private int value;
      OperandShift(int value) {
          this.value = value;
      }

      public final int value() {
          return value;
      }
    }

    enum OperandSize {
        SingleSize(0),
        DoubleSize(1);

        private final int value;

        private OperandSize(int n) {
            value = n << OperandShift.SizeShift.value;
        }

        public final int value() {
            return value;
        }

        public static OperandSize fromInt(int code) {
            switch(code) {
                case 0: return SingleSize;
                case 1: return DoubleSize;
            }
            throw new IllegalArgumentException("unknown operand size code: " + code);
        }

    }

    enum OperandMask {
        KindMask(rightNBits(OperandBits.KindBits.value) << OperandShift.TypeShift.value),
        TypeMask(rightNBits(OperandBits.TypeBits.value) << OperandShift.TypeShift.value),
        SizeMask(rightNBits(OperandBits.SizeBits.value) << OperandShift.SizeShift.value),
        LastUseMask(rightNBits(OperandBits.LastUseBits.value) << OperandShift.LastUseShift.value),
        IsFpuStackOffsetMask(rightNBits(OperandBits.IsFpuStackOffsetBits.value) << OperandShift.IsFpuStackOffsetShift.value),
        VirtualMask(rightNBits(OperandBits.VirtualBits.value) << OperandShift.VirtualShift.value),
        IsXmmMask(rightNBits(OperandBits.IsXmmBits.value) << OperandShift.IsXmmShift.value),
        PointerMask(rightNBits(OperandBits.PointerBits.value)),
        LowerRegMask(rightNBits(OperandBits.RegBits.value)),
        NoTypeMask((~(TypeMask.value | LastUseMask.value | IsFpuStackOffsetMask.value)));

        final int value;
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

        public final int value() {
            return value;
        }
    }

    public enum VirtualRegister {
        RegisterBase(NumberOfRegisters),
        MaxRegisters((1 << OperandBits.DataBits.value) - 1);

        private final int value;
        VirtualRegister(int value) {
            this.value = value;
        }
        public int value() {
            return value;
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

        private final int value;

        OperandType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    private int value;

    public int value() {
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

    public OperandType assertTypeField() {
        assert true;
        return null;
    }
    public static OperandSize sizeFor(BasicType t) {
        switch(t) {
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
        return false; //isCpuRegister() || isFpuRegister();
    }

    public boolean isVirtual() {
        return false; // isVirtualCpu() || isVirtualFpu();
    }

    public boolean isConstant() {
        return isPointer() && false; //pointer()->asConstant() != null;
    }

    public boolean isAddress() {
        return isPointer() && false; //&& pointer()->as_address() != NULL;
    }

    public boolean isFloatKind() {
        return true; //isPointer ?
    }

    public void print(LogStream out) {
        // TODO to be completed later
    }

    // TODO continue from  bool is_float_kind() const
}

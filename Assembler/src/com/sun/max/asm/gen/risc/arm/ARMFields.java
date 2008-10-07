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
/*VCSID=ee5bcd5c-6853-4fea-942b-a53cc2688fa4*/

package com.sun.max.asm.gen.risc.arm;

import static com.sun.max.asm.arm.GPR.*;

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.arm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.lang.*;


/**
 * The fields used in defining ARM instruction templates.
 * 
 * @author Sumeet Panchal
 */

public final class ARMFields {

    private ARMFields() {
    }

    public static final SymbolicOperandField<ConditionCode> _cond = SymbolicOperandField.createDescending("cond", ConditionCode.SYMBOLIZER, 31, 28);
    public static final SymbolicOperandField<SBit> _s = SymbolicOperandField.createDescending(SBit.SYMBOLIZER, 20, 20);

    public static final ImmediateOperandField _byte0 = ImmediateOperandField.createDescending(31, 24);
    public static final ImmediateOperandField _byte1 = ImmediateOperandField.createDescending(23, 16);
    public static final ImmediateOperandField _byte2 = ImmediateOperandField.createDescending(15, 8);
    public static final ImmediateOperandField _byte3 = ImmediateOperandField.createDescending(7, 0);
    public static final ImmediateOperandField _bits_27_26 = ImmediateOperandField.createDescending(27, 26);
    public static final ImmediateOperandField _i = ImmediateOperandField.createDescending(25, 25);
    public static final ImmediateOperandField _opcode = ImmediateOperandField.createDescending(24, 21);
    public static final ImmediateOperandField _rotate_imm = ImmediateOperandField.createDescending(11, 8);
    public static final ImmediateOperandField _immed_8 = ImmediateOperandField.createDescending(7, 0);
    public static final ImmediateOperandField _shifter_operand = ImmediateOperandField.createDescending(11, 0);
    public static final ImmediateOperandField _bits_11_7 = ImmediateOperandField.createDescending(11, 7);
    public static final ImmediateOperandField _bits_6_4 = ImmediateOperandField.createDescending(6, 4);
    public static final ImmediateOperandField _shift_imm = ImmediateOperandField.createDescending(11, 7);
    public static final ImmediateOperandField _bits_7_4 = ImmediateOperandField.createDescending(7, 4);
    public static final ImmediateOperandField _bits_11_4 = ImmediateOperandField.createDescending(11, 4);
    public static final ImmediateOperandField _bits_4_0 = ImmediateOperandField.createDescending(4, 0);
    public static final ImmediateOperandField _sbz_19_16 = ImmediateOperandField.createDescending(19, 16);
    public static final ImmediateOperandField _sbz_15_12 = ImmediateOperandField.createDescending(15, 12);
    public static final ImmediateOperandField _sbz_11_0 = ImmediateOperandField.createDescending(11, 0);
    public static final ImmediateOperandField _sbz_11_8 = ImmediateOperandField.createDescending(11, 8);
    public static final ImmediateOperandField _sbo_19_16 = ImmediateOperandField.createDescending(19, 16);
    public static final ImmediateOperandField _sbo_11_8 = ImmediateOperandField.createDescending(11, 8);
    public static final ImmediateOperandField _bits_5_0 = new ImmediateOperandField(new DescendingBitRange(5, 0)) {
        @Override
        public ArgumentRange argumentRange() {
            return new ArgumentRange(this, 0, 32);
        }
    };
    public static final ImmediateOperandField _bits_31_0 = new ImmediateOperandField(new DescendingBitRange(31, 0)) {
        @Override
        public Iterable< ? extends Argument> getIllegalTestArguments() {
            final List<Immediate32Argument> illegalTestArguments = new ArrayList<Immediate32Argument>();
            illegalTestArguments.add(new Immediate32Argument(0x101));
            illegalTestArguments.add(new Immediate32Argument(0x102));
            illegalTestArguments.add(new Immediate32Argument(0xff1));
            illegalTestArguments.add(new Immediate32Argument(0xff04));
            illegalTestArguments.add(new Immediate32Argument(0xff003));
            illegalTestArguments.add(new Immediate32Argument(0xf000001f));
            return illegalTestArguments;
        }
        @Override
        public Iterable< ? extends Argument> getLegalTestArguments() {
            final List<Immediate32Argument> legalTestArguments = new ArrayList<Immediate32Argument>();
            int argument;
            for (int immediate : new int[]{0, 1, 31, 32, 33, 63, 64, 65, 127, 128, 129, 254, 255}) {
                for (int i = 0; i < 32; i += 2) {
                    argument = Integer.rotateLeft(immediate, i);
                    final Immediate32Argument immediate32Argument = new Immediate32Argument(argument);
                    if (!legalTestArguments.contains(immediate32Argument)) {
                        legalTestArguments.add(immediate32Argument);
                    }
                }
            }
            return legalTestArguments;
        }
        @Override
        public ArgumentRange argumentRange() {
            return new ArgumentRange(this, 0x80000000, 0x7fffffff);
        }
    };
    public static final ImmediateOperandField _bits_27_21 = ImmediateOperandField.createDescending(27, 21);
    public static final ImmediateOperandField _bits_27_20 = ImmediateOperandField.createDescending(27, 20);
    public static final ImmediateOperandField _bit_27 = ImmediateOperandField.createDescending(27, 27);
    public static final ImmediateOperandField _bit_26 = ImmediateOperandField.createDescending(26, 26);
    public static final ImmediateOperandField _bit_25 = ImmediateOperandField.createDescending(25, 25);
    public static final ImmediateOperandField _bit_24 = ImmediateOperandField.createDescending(24, 24);
    public static final ImmediateOperandField _bit_23 = ImmediateOperandField.createDescending(23, 23);
    public static final ImmediateOperandField _r = ImmediateOperandField.createDescending(22, 22);
    public static final ImmediateOperandField _bit_21 = ImmediateOperandField.createDescending(21, 21);
    public static final ImmediateOperandField _bit_20 = ImmediateOperandField.createDescending(20, 20);
    public static final ImmediateOperandField _bit_4 = ImmediateOperandField.createDescending(4, 4);
    public static final ImmediateOperandField _p = ImmediateOperandField.createDescending(24, 24);
    public static final ImmediateOperandField _u = ImmediateOperandField.createDescending(23, 23);
    public static final ImmediateOperandField _b = ImmediateOperandField.createDescending(22, 22);
    public static final ImmediateOperandField _w = ImmediateOperandField.createDescending(21, 21);
    public static final ImmediateOperandField _l = ImmediateOperandField.createDescending(20, 20);
    public static final ImmediateOperandField _offset_12 = ImmediateOperandField.createDescending(11, 0);
    public static final ImmediateOperandField _shift = ImmediateOperandField.createDescending(6, 5);
    public static final ImmediateOperandField _bits_31_28 = ImmediateOperandField.createDescending(31, 28);
    public static final ImmediateOperandField _immed_19_8 = ImmediateOperandField.createDescending(19, 8);
    public static final ImmediateOperandField _immed_3_0 = ImmediateOperandField.createDescending(3, 0);
    public static final ImmediateOperandField _immed_24 = ImmediateOperandField.createDescending(23, 0);
    public static final ImmediateOperandField _bits_27_24 = ImmediateOperandField.createDescending(27, 24);

    public static final InputOperandField _immediate = InputOperandField.create(_bits_31_0).setVariableName("immediate");
    public static final InputOperandField _rotate_amount = InputOperandField.create(_bits_4_0).setVariableName("rotate_amount");
    public static final InputOperandField _shift_imm2 = (InputOperandField) InputOperandField.create(_bits_5_0).withExcludedExternalTestArguments(new Immediate32Argument(0)).setVariableName("shift_imm");
    public static final InputOperandField _immediate2 = InputOperandField.create(new ImmediateOperandField(new DescendingBitRange(15, 0))).setVariableName("immediate");

    public static RiscConstant s(int value) {
        return _s.constant(value);
    }
    public static RiscConstant cond(int value) {
        return _cond.constant(value);
    }
    public static RiscConstant bits_27_26(int value) {
        return _bits_27_26.constant(value);
    }
    public static RiscConstant i(int value) {
        return _i.constant(value);
    }
    public static RiscConstant opcode(int value) {
        return _opcode.constant(value);
    }
    public static RiscConstant bits_11_7(int value) {
        return _bits_11_7.constant(value);
    }
    public static RiscConstant bits_6_4(int value) {
        return _bits_6_4.constant(value);
    }
    public static RiscConstant bits_7_4(int value) {
        return _bits_7_4.constant(value);
    }
    public static RiscConstant bits_11_4(int value) {
        return _bits_11_4.constant(value);
    }
    public static RiscConstant sbz_19_16(int value) {
        return _sbz_19_16.constant(value);
    }
    public static RiscConstant sbz_15_12(int value) {
        return _sbz_15_12.constant(value);
    }
    public static RiscConstant sbz_11_0(int value) {
        return _sbz_11_0.constant(value);
    }
    public static RiscConstant sbz_11_8(int value) {
        return _sbz_11_8.constant(value);
    }
    public static RiscConstant sbo_19_16(int value) {
        return _sbo_19_16.constant(value);
    }
    public static RiscConstant sbo_11_8(int value) {
        return _sbo_11_8.constant(value);
    }
    public static RiscConstant bits_27_21(int value) {
        return _bits_27_21.constant(value);
    }
    public static RiscConstant bits_27_20(int value) {
        return _bits_27_20.constant(value);
    }
    public static RiscConstant bit_27(int value) {
        return _bit_27.constant(value);
    }
    public static RiscConstant bit_26(int value) {
        return _bit_26.constant(value);
    }
    public static RiscConstant bit_25(int value) {
        return _bit_25.constant(value);
    }
    public static RiscConstant bit_24(int value) {
        return _bit_24.constant(value);
    }
    public static RiscConstant bit_23(int value) {
        return _bit_23.constant(value);
    }
    public static RiscConstant bit_4(int value) {
        return _bit_4.constant(value);
    }
    public static RiscConstant r(int value) {
        return _r.constant(value);
    }
    public static RiscConstant bit_21(int value) {
        return _bit_21.constant(value);
    }
    public static RiscConstant bit_20(int value) {
        return _bit_20.constant(value);
    }
    public static RiscConstant p(int value) {
        return _p.constant(value);
    }
    public static RiscConstant u(int value) {
        return _u.constant(value);
    }
    public static RiscConstant b(int value) {
        return _b.constant(value);
    }
    public static RiscConstant w(int value) {
        return _w.constant(value);
    }
    public static RiscConstant l(int value) {
        return _l.constant(value);
    }
    public static RiscConstant shift(int value) {
        return _shift.constant(value);
    }
    public static RiscConstant shift_imm(int value) {
        return _shift_imm.constant(value);
    }
    public static RiscConstant bits_31_28(int value) {
        return _bits_31_28.constant(value);
    }
    public static RiscConstant bits_27_24(int value) {
        return _bits_27_24.constant(value);
    }

    public static OperandField<ImmediateArgument> rotate_imm(Expression expression) {
        return _rotate_imm.bindTo(expression);
    }
    public static OperandField<ImmediateArgument> shifter_operand(Expression expression) {
        return _shifter_operand.bindTo(expression);
    }
    public static OperandField<ImmediateArgument> shift_imm(Expression expression) {
        return _shift_imm.bindTo(expression);
    }
    public static OperandField<ImmediateArgument> immed_19_8(Expression expression) {
        return _immed_19_8.bindTo(expression);
    }
    public static OperandField<ImmediateArgument> immed_3_0(Expression expression) {
        return _immed_3_0.bindTo(expression);
    }

    public static final SymbolicOperandField<GPR> _Rn = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 19, 16);
    public static final SymbolicOperandField<GPR> _Rn2 = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 15, 12).setVariableName("Rn");
    public static final SymbolicOperandField<GPR> _Rd = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 15, 12);
    public static final SymbolicOperandField<GPR> _Rd2 = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 19, 16).setVariableName("Rd");
    public static final SymbolicOperandField<GPR> _Rm = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 3, 0);
    public static final SymbolicOperandField<GPR> _Rs = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 11, 8);
    public static final SymbolicOperandField<GPR> _RdHi = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 19, 16);
    public static final SymbolicOperandField<GPR> _RdLo = SymbolicOperandField.createDescending(GPR_SYMBOLIZER, 15, 12);

    static {
        StaticFieldName.Static.initialize(ARMFields.class, new StaticFieldName.StringFunction() {
            public String function(String name) {
                if (name.startsWith("_")) {
                    return name.substring(1);
                }
                return name;
            }
        });
    }

}

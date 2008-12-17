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
package com.sun.max.asm.gen.risc.sparc;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.asm.sparc.*;
import com.sun.max.lang.*;

/**
 * The fields used in defining the SPARC instruction templates.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public final class SPARCFields {

    private SPARCFields() {
    }

    // Checkstyle: stop constant name checks

    private static final ConstantField _bits_29_29 = ConstantField.createDescending(29, 29);

    public static RiscConstant bits_29_29(int value) {
        return _bits_29_29.constant(value);
    }

    private static final ConstantField _bits_29_27 = ConstantField.createDescending(29, 27);

    public static RiscConstant bits_29_27(int value) {
        return _bits_29_27.constant(value);
    }

    private static final ConstantField _bits_28_28 = ConstantField.createDescending(28, 28);

    public static RiscConstant bits_28_28(int value) {
        return _bits_28_28.constant(value);
    }

    private static final ConstantField _bits_24_22 = ConstantField.createDescending(24, 22);

    public static RiscConstant bits_24_22(int value) {
        return _bits_24_22.constant(value);
    }

    private static final ConstantField _bits_18_18 = ConstantField.createDescending(18, 18);

    public static RiscConstant bits_18_18(int value) {
        return _bits_18_18.constant(value);
    }

    private static final ConstantField _bits_18_14 = ConstantField.createDescending(18, 14);

    public static RiscConstant bits_18_14(int value) {
        return _bits_18_14.constant(value);
    }

    private static final ConstantField _bits_13_13 = ConstantField.createDescending(13, 13);

    public static RiscConstant bits_13_13(int value) {
        return _bits_13_13.constant(value);
    }

    private static final ConstantField _cond_17_14 = ConstantField.createDescending(17, 14);

    public static RiscConstant cond_17_14(int value) {
        return _cond_17_14.constant(value);
    }

    private static final ConstantField _fcnc = ConstantField.createDescending(29, 25);

    public static RiscConstant fcnc(int value) {
        return _fcnc.constant(value);
    }

    private static final ConstantField _i = ConstantField.createDescending(13, 13);

    public static RiscConstant i(int value) {
        return _i.constant(value);
    }

    private static final ConstantField _movTypeBit = ConstantField.createDescending(18, 18);

    public static RiscConstant movTypeBit(int value) {
        return _movTypeBit.constant(value);
    }

    private static final ConstantField _fmovTypeBit = ConstantField.createDescending(13, 13);

    public static RiscConstant fmovTypeBit(int value) {
        return _fmovTypeBit.constant(value);
    }

    private static final ConstantField _op = ConstantField.createDescending(31, 30);

    public static RiscConstant op(int value) {
        return _op.constant(value);
    }

    private static final ConstantField _op2 = ConstantField.createDescending(24, 22);

    public static RiscConstant op2(int value) {
        return _op2.constant(value);
    }

    private static final ConstantField _op3 = ConstantField.createDescending(24, 19);

    public static RiscConstant op3(int value) {
        return _op3.constant(value);
    }

    private static final ConstantField _opf = ConstantField.createDescending(13, 5);

    public static RiscConstant opf(int value) {
        return _opf.constant(value);
    }

    private static final ConstantField _opfLow_10_5 = ConstantField.createDescending(10, 5);

    public static RiscConstant opfLow_10_5(int value) {
        return _opfLow_10_5.constant(value);
    }

    private static final ConstantField _opfLow_9_5 = ConstantField.createDescending(9, 5);

    public static RiscConstant opfLow_9_5(int value) {
        return _opfLow_9_5.constant(value);
    }

    private static final ConstantField _rcond_12_10 = ConstantField.createDescending(12, 10);

    public static RiscConstant rcond_12_10(int value) {
        return _rcond_12_10.constant(value);
    }

    private static final ConstantField _x = ConstantField.createDescending(12, 12);

    public static RiscConstant x(int value) {
        return _x.constant(value);
    }

    public static final IgnoredOperandField _const22 = IgnoredOperandField.createDescendingIgnored(21, 0);

    public static final ImmediateOperandField _fcn = ImmediateOperandField.createDescending(29, 25);

    public static final ImmediateOperandField _imm22 = ImmediateOperandField.createDescending(21, 0).beSignedOrUnsigned();

    public static RiscConstant imm22(int value) {
        return _imm22.constant(value);
    }

    public static final ImmediateOperandField _immAsi = ImmediateOperandField.createDescending(12, 5);

    public static RiscConstant immAsi(int value) {
        return _immAsi.constant(value);
    }

    public static final ImmediateOperandField _shcnt32 = ImmediateOperandField.createDescending(4, 0);
    public static final ImmediateOperandField _shcnt64 = ImmediateOperandField.createDescending(5, 0);
    public static final ImmediateOperandField _simm10 = ImmediateOperandField.createDescending(9, 0).beSigned();
    public static final ImmediateOperandField _simm11 = ImmediateOperandField.createDescending(10, 0).beSigned();
    public static final ImmediateOperandField _swTrapNumber = ImmediateOperandField.createDescending(6, 0);
    static {
        _swTrapNumber.setVariableName("software_trap_number");
    }

    public static final ImmediateOperandField _simm13 = ImmediateOperandField.createDescending(12, 0).beSigned();

    public static RiscConstant simm13(int value) {
        return _simm13.constant(value);
    }

    public static final BranchDisplacementOperandField _disp30 = BranchDisplacementOperandField.createDescendingBranchDisplacementOperandField(29, 0);
    public static final BranchDisplacementOperandField _disp22 = BranchDisplacementOperandField.createDescendingBranchDisplacementOperandField(21, 0);
    public static final BranchDisplacementOperandField _disp19 = BranchDisplacementOperandField.createDescendingBranchDisplacementOperandField(18, 0);
    public static final BranchDisplacementOperandField _d16 = BranchDisplacementOperandField.createDescendingBranchDisplacementOperandField(21, 20, 13, 0);

    public static final SymbolicOperandField<MembarOperand> _membarMask = SymbolicOperandField.createDescending(MembarOperand.SYMBOLIZER, 6, 0);

    private static SymbolicOperandField<ICCOperand> createICCOperandField(int... bits) {
        return SymbolicOperandField.createDescending("i_or_x_cc", ICCOperand.SYMBOLIZER, bits);
    }

    private static SymbolicOperandField<FCCOperand> createFCCOperandField(int... bits) {
        return SymbolicOperandField.createDescending("n", FCCOperand.SYMBOLIZER, bits);
    }

    public static final SymbolicOperandField<ICCOperand> _cc = createICCOperandField(21, 20);

    public static RiscConstant cc(ICCOperand icc) {
        return _cc.constant(icc);
    }


    public static final SymbolicOperandField<FCCOperand> _fcc_26_25 = createFCCOperandField(26, 25);
    public static final SymbolicOperandField<FCCOperand> _fcc_21_20 = createFCCOperandField(21, 20);
    public static final SymbolicOperandField<ICCOperand> _fmovicc = createICCOperandField(12, 11);
    public static final SymbolicOperandField<FCCOperand> _fmovfcc = createFCCOperandField(12, 11);
    public static final SymbolicOperandField<ICCOperand> _movicc = createICCOperandField(12, 11);
    public static final SymbolicOperandField<FCCOperand> _movfcc = createFCCOperandField(12, 11);

    public static final SymbolicOperandField<GPR> _rs1 = SymbolicOperandField.createDescending(GPR.SYMBOLIZER, 18, 14);

    public static RiscConstant rs1(GPR gpr) {
        return _rs1.constant(gpr);
    }

    public static SymbolicOperandField<GPR> rs1(Expression expression) {
        return _rs1.bindTo(expression);
    }

    public static RiscConstant rs1(int value) {
        return _rs1.constant(value);
    }

    public static final SymbolicOperandField<StateRegister> _rs1_state = SymbolicOperandField.createDescending("rs1", StateRegister.SYMBOLIZER, 18, 14);

    public static RiscConstant rs1_state(int value) {
        return _rs1_state.constant(value);
    }

    public static final SymbolicOperandField<GPR> _rs2 = SymbolicOperandField.createDescending(GPR.SYMBOLIZER, 4, 0);

    public static RiscConstant rs2(GPR gpr) {
        return _rs2.constant(gpr);
    }

    public static SymbolicOperandField<GPR> rs2(Expression expression) {
        return _rs2.bindTo(expression);
    }

    public static final SymbolicOperandField<GPR> _rd = SymbolicOperandField.createDescending(GPR.SYMBOLIZER, 29, 25);

    public static RiscConstant rd(GPR gpr) {
        return _rd.constant(gpr);
    }

    public static RiscConstant rd(int value) {
        return _rd.constant(value);
    }

    public static final SymbolicOperandField<GPR.Even> _rd_even = SymbolicOperandField.createDescending("rd", GPR.EVEN_SYMBOLIZER, 29, 25);

    public static final SymbolicOperandField<StateRegister.Writable> _rd_state = SymbolicOperandField.createDescending("rd", StateRegister.WRITE_ONLY_SYMBOLIZER, 29, 25);

    public static final SymbolicOperandField<ICCOperand> _tcc = createICCOperandField(12, 11);
    public static final SymbolicOperandField<SFPR> _sfrs1 = SymbolicOperandField.createDescending("rs1", SFPR.SYMBOLIZER, 18, 14);
    public static final SymbolicOperandField<SFPR> _sfrs2 = SymbolicOperandField.createDescending("rs2", SFPR.SYMBOLIZER, 4, 0);
    public static final SymbolicOperandField<SFPR> _sfrd = SymbolicOperandField.createDescending("rd", SFPR.SYMBOLIZER, 29, 25);
    public static final SymbolicOperandField<DFPR> _dfrs1 = SymbolicOperandField.createDescending("rs1", DFPR.SYMBOLIZER, 14, 14, 18, 15, -1);
    public static final SymbolicOperandField<DFPR> _dfrs2 = SymbolicOperandField.createDescending("rs2", DFPR.SYMBOLIZER, 0, 0, 4, 1, -1);
    public static final SymbolicOperandField<DFPR> _dfrd = SymbolicOperandField.createDescending("rd", DFPR.SYMBOLIZER, 25, 25, 29, 26, -1);
    public static final SymbolicOperandField<PrivilegedRegister> _rs1PrivReg = SymbolicOperandField.createDescending("rs1", PrivilegedRegister.SYMBOLIZER, 18, 14);
    public static final SymbolicOperandField<PrivilegedRegister.Writable> _rdPrivReg = SymbolicOperandField.createDescending("rd", PrivilegedRegister.WRITE_ONLY_SYMBOLIZER, 29, 25);

    private static final SymbolicOperandField<QFPR> _qfrs1_raw = SymbolicOperandField.createDescending("rs1", QFPR.SYMBOLIZER, 14, 14, 18, 16, -2);
    private static final SymbolicOperandField<QFPR> _qfrs2_raw = SymbolicOperandField.createDescending("rs2", QFPR.SYMBOLIZER, 0, 0, 4, 2, -2);
    private static final SymbolicOperandField<QFPR> _qfrd_raw = SymbolicOperandField.createDescending("rd", QFPR.SYMBOLIZER, 25, 25, 29, 27, -2);

    public static final Object[] _qfrs1 = {_qfrs1_raw, ReservedField.createDescending(15, 15)};
    public static final Object[] _qfrs2 = {_qfrs2_raw, ReservedField.createDescending(1, 1)};
    public static final Object[] _qfrd =  {_qfrd_raw, ReservedField.createDescending(26, 26)};

    public static final SymbolicOperandField<BPr> _rcond_27_25 = SymbolicOperandField.createDescending("cond", BPr.SYMBOLIZER, 27, 25);

    public static RiscConstant rcond_27_25(BPr value) {
        return _rcond_27_25.constant(value);
    }

    public static final SymbolicOperandField<FBfcc> _fcond_28_25 = SymbolicOperandField.createDescending("cond", FBfcc.SYMBOLIZER, 28, 25);

    public static RiscConstant fcond_28_25(FBfcc value) {
        return _fcond_28_25.constant(value);
    }

    public static final SymbolicOperandField<Bicc> _icond_28_25 = SymbolicOperandField.createDescending("cond", Bicc.SYMBOLIZER, 28, 25);

    public static RiscConstant icond_28_25(Bicc value) {
        return _icond_28_25.constant(value);
    }

    public static final SymbolicOperandField<AnnulBit> _a = SymbolicOperandField.createDescending(AnnulBit.SYMBOLIZER, 29, 29);
    public static RiscConstant a(AnnulBit value) {
        return _a.constant(value);
    }

    public static final SymbolicOperandField<BranchPredictionBit> _p = SymbolicOperandField.createDescending(BranchPredictionBit.SYMBOLIZER, 19, 19);
    public static RiscConstant p(BranchPredictionBit value) {
        return _p.constant(value);
    }

    public static final ReservedField _res_29_29 = ReservedField.createDescending(29, 29);
    public static final ReservedField _res_29_25 = ReservedField.createDescending(29, 25);
    public static final ReservedField _res_18_14 = ReservedField.createDescending(18, 14);
    public static final ReservedField _res_18_0 = ReservedField.createDescending(18, 0);
    public static final ReservedField _res_13_0 = ReservedField.createDescending(13, 0);
    public static final ReservedField _res_12_7 = ReservedField.createDescending(12, 7);
    public static final ReservedField _res_12_5 = ReservedField.createDescending(12, 5);
    public static final ReservedField _res_12_0 = ReservedField.createDescending(12, 0);
    public static final ReservedField _res_11_6 = ReservedField.createDescending(11, 6);
    public static final ReservedField _res_11_5 = ReservedField.createDescending(11, 5);
    public static final ReservedField _res_10_7 = ReservedField.createDescending(10, 7);
    public static final ReservedField _res_10_5 = ReservedField.createDescending(10, 5);
    public static final ReservedField _res_9_5 = ReservedField.createDescending(9, 5);
    public static final ReservedField _impl_dep = ReservedField.createDescending(29, 25, 18, 0);

    // Checkstyle: resume constant name checks

    static {
        StaticFieldName.Static.initialize(SPARCFields.class, new StaticFieldName.StringFunction() {

            public String function(String name) {
                if (name.startsWith("_")) {
                    return name.substring(1);
                }
                return name;
            }
        });
        StaticFieldLiteral.Static.initialize(SPARCFields.class);
    }
}

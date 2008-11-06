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
package com.sun.max.asm.gen.risc.ppc;

import static com.sun.max.asm.gen.InstructionConstraint.Static.*;
import static com.sun.max.asm.ppc.BOOperand.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.asm.ppc.*;
import com.sun.max.lang.*;

/**
 * The fields used in defining the PowerPC instruction templates.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
final class PPCFields {

    private PPCFields() {
    }

    // Checkstyle: stop constant name checks

    /**
     * RA field that can also accept the constant 0.
     */
    public static final SymbolicOperandField<ZeroOrRegister> _ra0 = SymbolicOperandField.createAscending(ZeroOrRegister.symbolizer(), 11, 15).setVariableName("ra");

    public static RiscConstant ra0(ZeroOrRegister value) {
        return _ra0.constant(value);
    }

    /**
     * RA field that can only accept GPR symbols.
     */
    public static final SymbolicOperandField<GPR> _ra = SymbolicOperandField.createAscending(GPR.GPR_SYMBOLIZER, 11, 15);

    public static RiscConstant ra(GPR value) {
        return _ra.constant(value);
    }

    /**
     * GPR symbol RA field with constraint: RA != GPR.R0.
     */
    public static final Object[] _ra_notR0 = {_ra, ne(_ra, GPR.R0)};

    /**
     * GPR symbol or 0 RA field with constraint: RA != GPR.R0.
     */
    public static final Object[] _ra0_notR0 = {_ra0, ne(_ra0, GPR.R0)};

    public static final SymbolicOperandField<GPR> _rb = SymbolicOperandField.createAscending(GPR.GPR_SYMBOLIZER, 16, 20);

    public static RiscConstant rb(GPR value) {
        return _rb.constant(value);
    }

    public static RiscConstant rs(GPR value) {
        return _rs.constant(value);
    }

    public static SymbolicOperandField<GPR> rs(Expression expression) {
        return _rs.bindTo(expression);
    }

    public static final SymbolicOperandField<GPR> _rs = SymbolicOperandField.createAscending(GPR.GPR_SYMBOLIZER, 6, 10);
    public static final SymbolicOperandField<GPR> _rt = SymbolicOperandField.createAscending(GPR.GPR_SYMBOLIZER, 6, 10);

    /**
     * GPR symbol RA field with constraint: RA != GPR.R0 && RA != RT.
     */
    public static final Object[] _ra_notR0_notRT = {_ra, ne(_ra, GPR.R0), ne(_ra, _rt)};

    /**
     * GCP symbol or 0 RA field with constraint: RA != GPR.R0 && RA < RT.
     */
    public static final Object[] _ra0_notR0_ltRT = {_ra0, ne(_ra0, GPR.R0), lt(_ra0, _rt)};

    public static final SymbolicOperandField<CRF> _bf = SymbolicOperandField.createAscending(CRF.ENUMERATOR, 6, 8);

    public static RiscConstant bf(CRF value) {
        return _bf.constant(value);
    }

    public static final SymbolicOperandField<CRF> _bfa = SymbolicOperandField.createAscending(CRF.ENUMERATOR, 11, 13);
    public static final SymbolicOperandField<CRF> _br_crf = SymbolicOperandField.createAscending(CRF.ENUMERATOR, 11, 13).setVariableName("crf");

    public static final ImmediateOperandField _spr = ImmediateOperandField.createAscending(16, 20, 11, 15);

    public static final SymbolicOperandField<FPR> _frt = SymbolicOperandField.createAscending(FPR.ENUMERATOR, 6, 10);
    public static final SymbolicOperandField<FPR> _frs = SymbolicOperandField.createAscending(FPR.ENUMERATOR, 6, 10);
    public static final SymbolicOperandField<FPR> _fra = SymbolicOperandField.createAscending(FPR.ENUMERATOR, 11, 15);
    public static final SymbolicOperandField<FPR> _frb = SymbolicOperandField.createAscending(FPR.ENUMERATOR, 16, 20);
    public static final SymbolicOperandField<FPR> _frc = SymbolicOperandField.createAscending(FPR.ENUMERATOR, 21, 25);

    public static final SymbolicOperandField<BOOperand> _bo = SymbolicOperandField.createAscending(BOOperand.SYMBOLIZER, 6, 10);

    public static RiscConstant bo(BOOperand value) {
        return _bo.constant(value);
    }

    public static final ImmediateOperandField _d = ImmediateOperandField.createAscending(16, 31).beSigned();
    public static final ImmediateOperandField _ds = new AlignedImmediateOperandField(new AscendingBitRange(16, 29), 2).beSigned();
    public static final ImmediateOperandField _si = ImmediateOperandField.createAscending(16, 31).beSigned();
    public static final ImmediateOperandField _sis = ImmediateOperandField.createAscending(16, 31).beSignedOrUnsigned();
    public static final ImmediateOperandField _ui = ImmediateOperandField.createAscending(16, 31);
    public static final ImmediateOperandField _to = ImmediateOperandField.createAscending(6, 10);
    public static final ImmediateOperandField _sh64 = ImmediateOperandField.createAscending(30, 30, 16, 20).setVariableName("sh");
    public static final ImmediateOperandField _mb64 = ImmediateOperandField.createAscending(26, 26, 21, 25).setVariableName("mb");
    public static final ImmediateOperandField _me64 = ImmediateOperandField.createAscending(26, 26, 21, 25).setVariableName("me");
    public static final ImmediateOperandField _sh = ImmediateOperandField.createAscending(16, 20);
    public static final ImmediateOperandField _mb = ImmediateOperandField.createAscending(21, 25);
    public static final ImmediateOperandField _me = ImmediateOperandField.createAscending(26, 30);
    public static final ImmediateOperandField _fxm = ImmediateOperandField.createAscending(12, 19);
    public static final ImmediateOperandField _bi = ImmediateOperandField.createAscending(11, 15);
    public static final ImmediateOperandField _bt = ImmediateOperandField.createAscending(6, 10);
    public static final ImmediateOperandField _ba = ImmediateOperandField.createAscending(11, 15);
    public static final ImmediateOperandField _bb = ImmediateOperandField.createAscending(16, 20);
    public static final ImmediateOperandField _u = ImmediateOperandField.createAscending(16, 19);
    public static final ImmediateOperandField _flm = ImmediateOperandField.createAscending(7, 14);
    public static final ImmediateOperandField _l = ImmediateOperandField.createAscending(10, 10);

    private static final ImmediateOperandField _bh_raw = ImmediateOperandField.createAscending(19, 20).setVariableName("bh");
    public static final Object[] _bh = {_bh_raw, InstructionConstraint.Static.ne(_bh_raw, 2)};

    public static final ImmediateOperandField _nb = ImmediateOperandField.createAscending(16, 20);
    public static final ImmediateOperandField _numBits64 = ImmediateOperandField.createAscending(0, 5);
    public static final ImmediateOperandField _numBits32 = ImmediateOperandField.createAscending(0, 4);

    public static final ImmediateOperandField _byte0 = ImmediateOperandField.createAscending(0, 7);
    public static final ImmediateOperandField _byte1 = ImmediateOperandField.createAscending(8, 15);
    public static final ImmediateOperandField _byte2 = ImmediateOperandField.createAscending(16, 23);
    public static final ImmediateOperandField _byte3 = ImmediateOperandField.createAscending(24, 31);

    public static final InputOperandField _value = InputOperandField.create(_si);
    public static final InputOperandField _n = InputOperandField.create(_sh);
    public static final InputOperandField _b = InputOperandField.create(_sh);
    public static final InputOperandField _n64 = InputOperandField.create(_sh64).setVariableName("n");
    public static final InputOperandField _b64 = InputOperandField.create(_sh64).setVariableName("b");

    public static RiscConstant to(int value) {
        return _to.constant(value);
    }

    public static RiscConstant sh(int value) {
        return _sh.constant(value);
    }

    public static RiscConstant mb(int value) {
        return _mb.constant(value);
    }

    public static RiscConstant me(int value) {
        return _me.constant(value);
    }

    public static RiscConstant sh64(int value) {
        return _sh64.constant(value);
    }

    public static RiscConstant mb64(int value) {
        return _mb64.constant(value);
    }

    public static RiscConstant me64(int value) {
        return _me64.constant(value);
    }

    public static RiscConstant fxm(int value) {
        return _fxm.constant(value);
    }

    public static RiscConstant bi(int value) {
        return _bi.constant(value);
    }

    public static RiscConstant ui(int value) {
        return _ui.constant(value);
    }

    public static RiscConstant si(int value) {
        return _si.constant(value);
    }

    public static RiscConstant sis(int value) {
        return _sis.constant(value);
    }

    public static RiscConstant bh(int value) {
        return _bh_raw.constant(value);
    }

    public static RiscConstant l(int value) {
        return _l.constant(value);
    }

    public static ImmediateOperandField bb(Expression expression) {
        return _bb.bindTo(expression);
    }

    public static ImmediateOperandField bt(Expression expression) {
        return _bt.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> si(Expression expression) {
        return _si.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> sh(Expression expression) {
        return _sh.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> me(Expression expression) {
        return _me.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> mb(Expression expression) {
        return _mb.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> sh64(Expression expression) {
        return _sh64.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> me64(Expression expression) {
        return _me64.bindTo(expression);
    }

    public static OperandField<ImmediateArgument> mb64(Expression expression) {
        return _mb64.bindTo(expression);
    }

    public static final BranchDisplacementOperandField _li = BranchDisplacementOperandField.createAscendingBranchDisplacementOperandField(6, 29);
    public static final BranchDisplacementOperandField _bd = BranchDisplacementOperandField.createAscendingBranchDisplacementOperandField(16, 29);

    private static final ConstantField _bit_11 = ConstantField.createAscending(11, 11);

    public static RiscConstant bit_11(int value) {
        return _bit_11.constant(value);
    }

    private static final ConstantField _bit_30 = ConstantField.createAscending(30, 30);

    public static RiscConstant bit_30(int value) {
        return _bit_30.constant(value);
    }

    private static final ConstantField _bit_31 = ConstantField.createAscending(31, 31);

    public static RiscConstant bit_31(int value) {
        return _bit_31.constant(value);
    }

    private static final ConstantField _opcd = ConstantField.createAscending(0,  5);

    public static RiscConstant opcd(int value) {
        return _opcd.constant(value);
    }

    private static final ConstantField _xo_21_29 = ConstantField.createAscending(21, 29);

    public static RiscConstant xo_21_29(int value) {
        return _xo_21_29.constant(value);
    }

    private static final ConstantField _xo_21_30 = ConstantField.createAscending(21, 30);

    public static RiscConstant xo_21_30(int value) {
        return _xo_21_30.constant(value);
    }

    private static final ConstantField _xo_22_30 = ConstantField.createAscending(22, 30);

    public static RiscConstant xo_22_30(int value) {
        return _xo_22_30.constant(value);
    }

    private static final ConstantField _xo_27_29 = ConstantField.createAscending(27, 29);

    public static RiscConstant xo_27_29(int value) {
        return _xo_27_29.constant(value);
    }

    private static final ConstantField _xo_26_30 = ConstantField.createAscending(26, 30);

    public static RiscConstant xo_26_30(int value) {
        return _xo_26_30.constant(value);
    }

    private static final ConstantField _xo_27_30 = ConstantField.createAscending(27, 30);

    public static RiscConstant xo_27_30(int value) {
        return _xo_27_30.constant(value);
    }

    private static final ConstantField _xo_30_31 = ConstantField.createAscending(30, 31);

    public static RiscConstant xo_30_31(int value) {
        return _xo_30_31.constant(value);
    }


    public static final ReservedField _res_6 = ReservedField.createAscending(6,  6);
    public static final ReservedField _res_6_10 = ReservedField.createAscending(6, 10);
    public static final ReservedField _res_9 = ReservedField.createAscending(9,  9);
    public static final ReservedField _res_9_10 = ReservedField.createAscending(9, 10);
    public static final ReservedField _res_11 = ReservedField.createAscending(11, 11);
    public static final ReservedField _res_11_15 = ReservedField.createAscending(11, 15);
    public static final ReservedField _res_12_20 = ReservedField.createAscending(12, 20);
    public static final ReservedField _res_14_15 = ReservedField.createAscending(14, 15);
    public static final ReservedField _res_15 = ReservedField.createAscending(15, 15);
    public static final ReservedField _res_16_20 = ReservedField.createAscending(16, 20);
    public static final ReservedField _res_16_18 = ReservedField.createAscending(16, 18);
    public static final ReservedField _res_16_29 = ReservedField.createAscending(16, 29);
    public static final ReservedField _res_20 = ReservedField.createAscending(20, 20);
    public static final ReservedField _res_21 = ReservedField.createAscending(21, 21);
    public static final ReservedField _res_21_25 = ReservedField.createAscending(21, 25);
    public static final ReservedField _res_31 = ReservedField.createAscending(31, 31);

    public static final OptionField _oe = OptionField.createAscending(21, 21).withOption("", 0).withOption("o", 1);
    public static final OptionField _rc = OptionField.createAscending(31, 31).withOption("", 0).withOption("_", 1, ".");
    public static final OptionField _lk = OptionField.createAscending(31, 31).withOption("", 0).withOption("l", 1);
    public static final OptionField _aa = OptionField.createAscending(30, 30).withOption("", 0).withOption("a", 1);

    public static RiscConstant lk(int value) {
        return _lk.constant(value);
    }

    public static final OptionField _to_option = OptionField.createAscending(6, 10).
        withOption("lt", 16).
        withOption("le", 20).
        withOption("eq", 4).
        withOption("ge", 12).
        withOption("gt", 8).
        withOption("nl", 12).
        withOption("ne", 24).
        withOption("ng", 20).
        withOption("llt", 2).
        withOption("lle", 6).
        withOption("lge", 5).
        withOption("lgt", 1).
        withOption("lnl", 5).
        withOption("lng", 6);

    public static final OptionField _spr_option = OptionField.createAscending(16, 20, 11, 15).
        withOption("xer", SPR.XER).
        withOption("lr", SPR.LR).
        withOption("ctr", SPR.CTR);

    private static OptionField createSuffixField(String suffix) {
        // When using option fields, we sometimes need a suffix in the mnemonic AFTER the option field.
        // We can construct this using option field with one option and no bits in it.
        return OptionField.createAscending(-1).withOption(suffix, 0);
    }

    public static final OptionField _put_i_in_name = createSuffixField("i");
    public static final OptionField _put_lr_in_name = createSuffixField("lr");
    public static final OptionField _put_ctr_in_name = createSuffixField("ctr");

    private static int boTrue(int crValue) {
        return CRTrue.value() | crValue;
    }

    private static int boFalse(int crValue) {
        return CRFalse.value() | crValue;
    }

    public static final OptionField _branch_conds = OptionField.createAscending(6, 8, 14, 15).
        withOption("lt", boTrue(CRF.LT)).
        withOption("le", boFalse(CRF.GT)).
        withOption("eq", boTrue(CRF.EQ)).
        withOption("ge", boFalse(CRF.LT)).
        withOption("gt", boTrue(CRF.GT)).
        withOption("nl", boFalse(CRF.LT)).
        withOption("ne", boFalse(CRF.EQ)).
        withOption("ng", boFalse(CRF.GT)).
        withOption("so", boTrue(CRF.SO)).
        withOption("ns", boFalse(CRF.SO)).
        withOption("un", boTrue(CRF.UN)).
        withOption("nu", boFalse(CRF.UN));

    /**
     * An OptionField for the BO values that are in terms of the Count Register (CTR) and a bit in the Condition Register (CR).
     */
    public static final OptionField _bo_CTR_and_CR = OptionField.createAscending(6, 10).
        withOption("dnzt", CTRNonZero_CRTrue).
        withOption("dnzf", CTRNonZero_CRFalse).
        withOption("dzt", CTRZero_CRTrue).
        withOption("dzf", CTRZero_CRFalse);

    /**
     * An OptionField for the BO values that are only in terms of the Count Register (CTR) and don't include the prediction bits.
     */
    public static final OptionField _bo_CTR = OptionField.createAscending(6, 6, 8, 9).
        withOption("dnz", CTRNonZero.valueWithoutPredictionBits()).
        withOption("dz", CTRZero.valueWithoutPredictionBits());

    /**
     * An OperandField for the prediction bits in the BO values that are only in terms of a bit in the Condition Register (CR).
     */
    public static final SymbolicOperandField<BranchPredictionBits> _bo_CR_prediction = SymbolicOperandField.createAscending(BranchPredictionBits.SYMBOLIZER, 9, 10).setVariableName("prediction");

    /**
     * An OperandField for the prediction bits in the BO values that are only in terms of a bit in the Count Register (CTR).
     */
    public static final SymbolicOperandField<BranchPredictionBits> _bo_CTR_prediction = SymbolicOperandField.createAscending(BranchPredictionBits.SYMBOLIZER, 7, 7, 10, 10).setVariableName("prediction");

    /**
     * An OptionField for the prediction bits in the BO values that are only in terms of a bit in the Condition Register (CR).
     */
    public static final OptionField _bo_CTR_prediction_option = OptionField.createAscending(9, 10).
        withOption("", BranchPredictionBits.NONE).
        withOption("_pt", BranchPredictionBits.PT).
        withOption("_pn", BranchPredictionBits.PN);

    /**
     * An OptionField for the BO values that are only in terms of a bit in the Condition Register (CR) and don't include the prediction bits.
     */
    public static final OptionField _bo_CR = OptionField.createAscending(6, 8).
        withOption("t", CRTrue.valueWithoutPredictionBits()).
        withOption("f", CRFalse.valueWithoutPredictionBits());


    /**
     * An OptionField for the prediction bits in the BO values that are only in terms of a bit in the Condition Register (CR).
     */
    public static final OptionField _bo_CR_prediction_option = OptionField.createAscending(7, 7, 10, 10).
        withOption("", BranchPredictionBits.NONE).
        withOption("_pt", BranchPredictionBits.PT).
        withOption("_pn", BranchPredictionBits.PN);

    // Checkstyle: resume constant name checks

    static {
        StaticFieldName.Static.initialize(PPCFields.class, new StaticFieldName.StringFunction() {
            public String function(String name) {
                if (name.startsWith("_")) {
                    return name.substring(1);
                }
                return name;
            }
        });
//        StaticFieldLiteral.Static.initialize(PPCFields.class);
    }
}

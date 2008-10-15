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

import static com.sun.max.asm.gen.Expression.Static.*;
import static com.sun.max.asm.gen.InstructionConstraint.Static.*;
import static com.sun.max.asm.gen.risc.ppc.PPCFields.*;
import static com.sun.max.asm.ppc.BOOperand.*;

import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.asm.ppc.*;

/**
 * The definitions of the synthetic PowerPC instructions.
 * <p>
 * The instructions assembled for the extended Rotate and Shift Mnemonics can not be disassembled
 * into their extended mnemonic form as some of their parameters are not directly correlated with
 * a field in the instruction. Instead, these {@link InputOperandField input} parameters are
 * part of an expression that gives a field its value and often, the same parameter is
 * part of an expression for more than one field. Recovering these parameters during
 * disassembly would require support for solving simultaneous equations. Given that the
 * external Mac OS X 'otool' disassembler does not disassemble these instructions into their
 * extended mnemonic form either, no one should be too upset with this limited functionality.
 * 
 * @author Doug Simon
 */
class SyntheticInstructions extends PPCInstructionDescriptionCreator {

    @Override
    protected RiscInstructionDescription define(Object... specifications) {
        return (RiscInstructionDescription) super.define(specifications).beSynthetic();
    }

    SyntheticInstructions(PPCTemplateCreator creator) {
        super(creator);

        setCurrentArchitectureManualSection("B.2.2");

        // Derived from "bc", "bca", "bcl" and "bcla" raw instructions
        define("b", opcd(16), _bo_CR, _bi,  _bd, _lk, _aa, _bo_CR_prediction);
        define("b", opcd(16), _bo_CTR, bi(0), _bd, _lk, _aa, _bo_CTR_prediction);
        define("b", opcd(16), _bo_CTR_and_CR, _bi,  _bd, _lk, _aa);

        // Derived from "bclr" and "bclrl" raw instructions
        define("blr", opcd(19), bo(Always), bi(0), _res_16_18, bh(0), xo_21_30(16), _lk);
        define("b", opcd(19), _bo_CR, _bi, _res_16_18, bh(0), xo_21_30(16), _put_lr_in_name, _lk, _bo_CR_prediction);
        define("b", opcd(19), _bo_CTR, bi(0), _res_16_18, bh(0), xo_21_30(16), _put_lr_in_name, _lk, _bo_CTR_prediction);
        define("b", opcd(19), _bo_CTR_and_CR, _bi, _res_16_18, bh(0), xo_21_30(16), _put_lr_in_name, _lk);

        // Derived from "bcctr" and "bcctrl" raw instructions
        define("bctr", opcd(19), bo(Always), bi(0), _res_16_18, bh(0), xo_21_30(528), _lk);
        define("b", opcd(19), _bo_CR, _bi, _res_16_18, bh(0), xo_21_30(528), _put_ctr_in_name, _lk, _bo_CR_prediction);

        setCurrentArchitectureManualSection("B.2.3");

        // Derived from "bc", "bca", "bcl" and "bcla" raw instructions
        define("b", opcd(16), "cr", _br_crf, _branch_conds, _bd, _lk, _aa, _bo_CR_prediction);
        define("b", opcd(19), "cr", _br_crf, _branch_conds, _res_16_18, bh(0), xo_21_30(16), _put_lr_in_name, _lk, _bo_CR_prediction);
        define("b", opcd(19), "cr", _br_crf, _branch_conds, _res_16_18, bh(0), xo_21_30(528), _put_ctr_in_name, _lk, _bo_CR_prediction);

        setCurrentArchitectureManualSection("B.3");

        synthesize("crset", "creqv", bt(_ba), bb(_ba));
        synthesize("crclr", "crxor", bt(_ba), bb(_ba));
        synthesize("crmove", "cror", bb(_ba));
        synthesize("crnot", "crnor", bb(_ba));

        setCurrentArchitectureManualSection("B.4.1");

        synthesize("subi", "addi", si(neg(_value)), _value);
        synthesize("subis", "addis", si(neg(_value)), _value);
        synthesize("subic", "addic", si(neg(_value)), _value);
        synthesize("subic_", "addic_", si(neg(_value)), _value).setExternalName("subic.");

        setCurrentArchitectureManualSection("B.4.2");

        synthesize("sub", "subf", _rt).swap(_ra, _rb);
        synthesize("subc", "subfc", _rt).swap(_ra, _rb);

        setCurrentArchitectureManualSection("B.5.1");

        synthesize("cmpdi", "cmpi", l(1));
        synthesize("cmpdi", "cmpi", bf(CRF.CR0), l(1));
        synthesize("cmpd", "cmp", l(1));
        synthesize("cmpd", "cmp", bf(CRF.CR0), l(1));
        synthesize("cmpldi", "cmpli", l(1));
        synthesize("cmpldi", "cmpli", bf(CRF.CR0), l(1));
        synthesize("cmpld", "cmpl", l(1));
        synthesize("cmpld", "cmpl", bf(CRF.CR0), l(1));

        setCurrentArchitectureManualSection("B.5.2");

        synthesize("cmpwi", "cmpi", l(0));
        synthesize("cmpwi", "cmpi", bf(CRF.CR0), l(0));
        synthesize("cmpw", "cmp", l(0));
        synthesize("cmpw", "cmp", bf(CRF.CR0), l(0));
        synthesize("cmplwi", "cmpli", l(0));
        synthesize("cmplwi", "cmpli", bf(CRF.CR0), l(0));
        synthesize("cmplw", "cmpl", l(0));
        synthesize("cmplw", "cmpl", bf(CRF.CR0), l(0));

        setCurrentArchitectureManualSection("B.6");

        synthesize("tw", "twi", _to_option, _put_i_in_name);
        synthesize("tw", "tw", _to_option);
        synthesize("trap", "tw", to(31), ra(GPR.R0), rb(GPR.R0));
        synthesize64("td", "tdi", _to_option, _put_i_in_name);
        synthesize64("td", "td", _to_option);

        setCurrentArchitectureManualSection("B.7.1");

        synthesize("extldi", "rldicr", sh64(_b64), me64(sub(_n64, 1)), _n64, _b64);
        synthesize("extrdi", "rldicl", sh64(add(_b64, _n64)), mb64(sub(64, _n64)), _n64, _b64);
        synthesize("insrdi", "rldimi", sh64(sub(64, add(_b64, _n64))), mb64(_b64), _n64, _b64);
        synthesize("rotldi", "rldicl", sh64(_n64), mb64(0), _n64);
        synthesize("rotrdi", "rldicl", sh64(sub(64, _n64)), mb64(0), _n64);
        synthesize("rotld", "rldcl", mb64(0));
        synthesize("sldi", "rldicr", sh64(_n64), mb64(sub(63, _n64)), _n64);
        synthesize("srdi", "rldicl", sh64(sub(64, _n64)), mb64(_n64), _n64);
        synthesize("clrldi", "rldicl", sh64(0), mb64(_n64), _n64);
        synthesize("clrrdi", "rldicr", sh64(0), me64(sub(63, _n64)), _n64);
        synthesize("clrlsldi", "rldic", sh64(_n64), mb64(sub(_b64, _n64)), _b64, _n64);

        setCurrentArchitectureManualSection("B.7.2");

        synthesize("extlwi", "rlwinm", sh(_b), mb(0), me(sub(_n, 1)), _n, _b, gt(_n, 0));
        synthesize("extrwi", "rlwinm", sh(add(_b, _n)), mb(sub(32, _n)), me(31), _n, _b, gt(_n, 0));
        synthesize("inslwi", "rlwimi", sh(sub(32, _b)), mb(_b), me(sub(add(_b, _n), 1)), _n, _b, gt(_n, 0));
        synthesize("insrwi", "rlwimi", sh(sub(32, add(_b, _n))), mb(_b), me(sub(add(_b, _n), 1)), _n, _b, gt(_n, 0));
        synthesize("rotlwi", "rlwinm", sh(_n), mb(0), me(31), _n);
        synthesize("rotrwi", "rlwinm", sh(sub(32, _n)), mb(0), me(31), _n);
        synthesize("rotlw", "rlwnm", mb(0), me(31));
        synthesize("slwi", "rlwinm", sh(_n), mb(0), me(sub(31, _n)), _n, lt(_n, 32));
        synthesize("srwi", "rlwinm", sh(sub(32, _n)), mb(_n), me(31), _n, lt(_n, 32));
        synthesize("clrlwi", "rlwinm", sh(0), mb(_n), me(31), _n, lt(_n, 32));
        synthesize("clrrwi", "rlwinm", sh(0), mb(0), me(sub(31, _n)), _n, lt(_n, 32));
        synthesize("clrlslwi", "rlwinm", sh(_n), mb(sub(_b, _n)), me(sub(31, _n)), _b, _n, le(_n, _b), lt(_b, 32));

        setCurrentArchitectureManualSection("B.8");

        synthesize("mt", "mtspr", _spr_option);
        synthesize("mf", "mfspr", _spr_option);

        setCurrentArchitectureManualSection("B.9");

        synthesize("nop", "ori", ra0(Zero.ZERO), rs(GPR.R0), ui(0));
        synthesize("li", "addi", ra0(Zero.ZERO));
        synthesize("lis", "addis", ra0(Zero.ZERO));
        define("la", opcd(14), _rt, _si, "(", _ra0_notR0, ")");
        synthesize("mr", "or", rs(_rb));
        synthesize("not", "nor", rs(_rb));
        synthesize("mtcr", "mtcrf", fxm(0xFF));
    }
}

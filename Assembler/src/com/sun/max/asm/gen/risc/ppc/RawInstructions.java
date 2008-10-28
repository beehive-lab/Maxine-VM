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
import static com.sun.max.asm.gen.risc.ppc.PPCFields.*;

import java.lang.reflect.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.ppc.*;

/**
 * The definitions of the raw (i.e. non-synthetic) PowerPC instructions.
 *
 * @author Doug Simon
 */
public final class RawInstructions extends PPCInstructionDescriptionCreator {

    RawInstructions(PPCTemplateCreator templateCreator) {
        super(templateCreator);

        setCurrentArchitectureManualSection("2.4.1");
        generateBranches();

        setCurrentArchitectureManualSection("2.4.3");
        generateConditionRegisterLogicals();

        setCurrentArchitectureManualSection("2.4.4");
        generateConditionRegisterFields();

        setCurrentArchitectureManualSection("3.3.2");
        generateLoads();

        setCurrentArchitectureManualSection("3.3.3");
        generateStores();

        setCurrentArchitectureManualSection("3.3.4");
        generateByteReversals();

        setCurrentArchitectureManualSection("3.3.5");
        generateLoadStoreMultiple();

        setCurrentArchitectureManualSection("3.3.6");
        generateMoveAssists();

        setCurrentArchitectureManualSection("3.3.8");
        generateFixedPointArithmetics();

        setCurrentArchitectureManualSection("3.3.9");
        generateFixedPointCompares();

        setCurrentArchitectureManualSection("3.3.10");
        generateFixedPointTraps();

        setCurrentArchitectureManualSection("3.3.11");
        generateFixedPointLogicals();

        setCurrentArchitectureManualSection("3.3.12");
        generateFixedPointRotates();

        setCurrentArchitectureManualSection("3.3.12.2");
        generateFixedPointShifts();

        setCurrentArchitectureManualSection("3.3.13");
        generateMoveToFromSystemRegisters();

        setCurrentArchitectureManualSection("4.6.2");
        generateFloatingPointLoads();

        setCurrentArchitectureManualSection("4.6.3");
        generateFloatingPointStores();

        setCurrentArchitectureManualSection("4.6.4");
        generateFloatingPointMoves();

        setCurrentArchitectureManualSection("4.6.5");
        generateFloatingPointAriths();

        setCurrentArchitectureManualSection("4.6.6");
        generateFloatingPointRoundsAndCvts();

        setCurrentArchitectureManualSection("4.6.7");
        generateFloatingPointCompares();

        setCurrentArchitectureManualSection("4.6.8");
        generateFloatingPointStatusAndCRs();

        setCurrentArchitectureManualSection("5.1.1");
        generateMoveToFromSystemRegistersOptional();

        setCurrentArchitectureManualSection("5.2.1");
        generateFloatingPointArithsOptional();

        setCurrentArchitectureManualSection("5.2.2");
        generateFloatingPointSelectOptional();

        setCurrentArchitectureManualSection("6.1");
        generateDeprecated();

        setCurrentArchitectureManualSection("3.2.1 [Book 2]");
        generateICacheManagement();

        setCurrentArchitectureManualSection("3.2.2 [Book 2]");
        generateDCacheManagement();

        setCurrentArchitectureManualSection("3.3.1 [Book 2]");
        generateInstructionSynchronization();

        setCurrentArchitectureManualSection("3.3.2 [Book 2]");
        generateAtomicUpdates();

        setCurrentArchitectureManualSection("3.3.3 [Book 2]");
        generateMemoryBarrier();
    }

    private void generateBranches() {
        define("b", opcd(18), _li, _lk, _aa);
        define("bc", opcd(16), _bo, _bi, _bd, _lk, _aa);
        define("bclr", opcd(19), _bo, _bi, _res_16_18, _bh, xo_21_30(16), _lk);
        define("bcctr", opcd(19), _bo, _bi, _res_16_18, _bh, xo_21_30(528), _lk);
    }

    private void generateConditionRegisterLogicals() {
        define("crand", opcd(19), _bt, _ba, _bb, xo_21_30(257), _res_31);
        define("crxor", opcd(19), _bt, _ba, _bb, xo_21_30(193), _res_31);
        define("cror", opcd(19), _bt, _ba, _bb, xo_21_30(449), _res_31);
        define("crnand", opcd(19), _bt, _ba, _bb, xo_21_30(225), _res_31);

        define("crnor", opcd(19), _bt, _ba, _bb, xo_21_30(33), _res_31);
        define("creqv", opcd(19), _bt, _ba, _bb, xo_21_30(289), _res_31);
        define("crandc", opcd(19), _bt, _ba, _bb, xo_21_30(129), _res_31);
        define("crorc", opcd(19), _bt, _ba, _bb, xo_21_30(417), _res_31);
    }

    private void generateConditionRegisterFields() {
        define("mcrf", opcd(19), _bf, _res_9_10, _bfa, _res_14_15, _res_16_20, xo_21_30(0), _res_31);
    }

    private void generateLoads() {

        define("lbz", opcd(34), _rt, _d, "(", _ra0_notR0, ")");
        define("lbzu", opcd(35), _rt, _d, "(", _ra_notR0_notRT, ")");
        define("lbzx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(87), _res_31);
        define("lbzux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(119), _res_31);

        define("lhz", opcd(40), _rt, _d, "(", _ra0_notR0, ")");
        define("lhzu", opcd(41), _rt, _d, "(", _ra_notR0_notRT, ")");
        define("lhzx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(279), _res_31);
        define("lhzux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(311), _res_31);

        define("lha", opcd(42), _rt, _d, "(", _ra0_notR0, ")");
        define("lhau", opcd(43), _rt, _d, "(", _ra_notR0_notRT, ")");
        define("lhax", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(343), _res_31);
        define("lhaux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(375), _res_31);

        define("lwz", opcd(32), _rt, _d, "(", _ra0_notR0, ")");
        define("lwzu", opcd(33), _rt, _d, "(", _ra_notR0_notRT, ")");
        define("lwzx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(23), _res_31);
        define("lwzux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(55), _res_31);

        define64("lwa", opcd(58), _rt, _ds, "(", _ra0_notR0, ")", xo_30_31(2));
        define64("lwax", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(341), _res_31);
        define64("lwaux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(373), _res_31);

        define64("ld", opcd(58), _rt, _ds, "(", _ra0_notR0, ")", xo_30_31(0));
        define64("ldu", opcd(58), _rt, _ds, "(", _ra_notR0_notRT, ")", xo_30_31(1));
        define64("ldx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(21), _res_31);
        define64("ldux", opcd(31), _rt, _ra_notR0_notRT, _rb, xo_21_30(53), _res_31);
    }

    private void generateStores() {

        define("stb", opcd(38), _rs, _d, "(", _ra0_notR0, ")");
        define("stbu", opcd(39), _rs, _d, "(", _ra_notR0, ")");
        define("stbx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(215), _res_31);
        define("stbux", opcd(31), _rs, _ra_notR0, _rb, xo_21_30(247), _res_31);

        define("sth", opcd(44), _rs, _d, "(", _ra0_notR0, ")");
        define("sthu", opcd(45), _rs, _d, "(", _ra_notR0, ")");
        define("sthx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(407), _res_31);
        define("sthux", opcd(31), _rs, _ra_notR0, _rb, xo_21_30(439), _res_31);

        define("stw", opcd(36), _rs, _d, "(", _ra0_notR0, ")");
        define("stwu", opcd(37), _rs, _d, "(", _ra_notR0, ")");
        define("stwx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(151), _res_31);
        define("stwux", opcd(31), _rs, _ra_notR0, _rb, xo_21_30(183), _res_31);

        define64("std", opcd(62), _rs, _ds, "(", _ra0_notR0, ")", xo_30_31(0));
        define64("stdu", opcd(62), _rs, _ds, "(", _ra_notR0, ")", xo_30_31(1));
        define64("stdx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(149), _res_31);
        define64("stdux", opcd(31), _rs, _ra_notR0, _rb, xo_21_30(181), _res_31);
    }

    private void generateByteReversals() {

        define("lhbrx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(790), _res_31);
        define("lwbrx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(534), _res_31);
        define("sthbrx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(918), _res_31);
        define("stwbrx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(662), _res_31);
    }

    private void generateLoadStoreMultiple() {

        define("lmw", opcd(46), _rt, _d, "(", _ra0_notR0_ltRT, ")");
        define("stmw", opcd(47), _rs, _d, "(", _ra0_notR0, ")");
    }

    private void generateMoveAssists() {

        final Method predicateMethod = InstructionConstraint.Static.getPredicateMethod(ZeroOrRegister.class, "isOutsideRegisterRange", GPR.class, Integer.TYPE);
        final InstructionConstraint lswiConstraint = InstructionConstraint.Static.makePredicate(predicateMethod, _ra0, _rt, _nb);
        define("lswi", opcd(31), _rt, _ra0_notR0, _nb, xo_21_30(597), _res_31, lswiConstraint);
        define("lswx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(533), _res_31, ne(_rt, _ra0), ne(_rt, _rb));
        define("stswi", opcd(31), _rs, _ra0_notR0, _nb, xo_21_30(725), _res_31);
        define("stswx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(661), _res_31);
    }

    private void generateFixedPointArithmetics() {
        define("addi", opcd(14), _rt, _ra0_notR0, _si);
        define("addis", opcd(15), _rt, _ra0_notR0, _sis);
        define("add", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(266), _rc);
        define("subf", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(40), _rc);
        define("addic", opcd(12), _rt, _ra, _si);
        define("addic_", opcd(13), _rt, _ra, _si).setExternalName("addic.");
        define("subfic", opcd(8), _rt, _ra, _si);
        define("addc", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(10), _rc);
        define("subfc", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(8), _rc);
        define("adde", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(138), _rc);
        define("subfe", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(136), _rc);
        define("addme", opcd(31), _rt, _ra, _res_16_20, _oe, xo_22_30(234), _rc);
        define("subfme", opcd(31), _rt, _ra, _res_16_20, _oe, xo_22_30(232), _rc);
        define("addze", opcd(31), _rt, _ra, _res_16_20, _oe, xo_22_30(202), _rc);
        define("subfze", opcd(31), _rt, _ra, _res_16_20, _oe, xo_22_30(200), _rc);
        define("neg", opcd(31), _rt, _ra, _res_16_20, _oe, xo_22_30(104), _rc);

        define("mulli", opcd(7), _rt, _ra, _si);
        define64("mulld", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(233), _rc);
        define("mullw", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(235), _rc);
        define64("mulhd", opcd(31), _rt, _ra, _rb, _res_21, xo_22_30(73), _rc);
        define("mulhw", opcd(31), _rt, _ra, _rb, _res_21, xo_22_30(75), _rc);
        define64("mulhdu", opcd(31), _rt, _ra, _rb, _res_21, xo_22_30(9), _rc);
        define("mulhwu", opcd(31), _rt, _ra, _rb, _res_21, xo_22_30(11), _rc);
        define64("divd", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(489), _rc);
        define("divw", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(491), _rc);
        define64("divdu", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(457), _rc);
        define("divwu", opcd(31), _rt, _ra, _rb, _oe, xo_22_30(459), _rc);
    }

    private void generateFixedPointCompares() {
        define("cmpi", opcd(11), _bf, _res_9, _l, _ra, _si);
        define("cmp", opcd(31), _bf, _res_9, _l, _ra, _rb, xo_21_30(0), _res_31);
        define("cmpli", opcd(10), _bf, _res_9, _l, _ra, _ui);
        define("cmpl", opcd(31), _bf, _res_9, _l, _ra, _rb, xo_21_30(32), _res_31);
    }

    private void generateFixedPointTraps() {
        define64("tdi", opcd(2), _to, _ra, _si);
        define("twi", opcd(3), _to, _ra, _si);
        define64("td", opcd(31), _to, _ra, _rb, xo_21_30(68), _res_31);
        define("tw", opcd(31), _to, _ra, _rb, xo_21_30(4), _res_31);
    }

    private void generateFixedPointLogicals() {

        define("andi_", opcd(28), _ra, _rs, _ui).setExternalName("andi.");
        define("andis_", opcd(29), _ra, _rs, _ui).setExternalName("andis.");
        define("ori", opcd(24), _ra, _rs, _ui);
        define("oris", opcd(25), _ra, _rs, _ui);
        define("xori", opcd(26), _ra, _rs, _ui);
        define("xoris", opcd(27), _ra, _rs, _ui);

        define("and", opcd(31), _ra, _rs, _rb, xo_21_30(28), _rc);
        define("or", opcd(31), _ra, _rs, _rb, xo_21_30(444), _rc);
        define("xor", opcd(31), _ra, _rs, _rb, xo_21_30(316), _rc);
        define("nand", opcd(31), _ra, _rs, _rb, xo_21_30(476), _rc);
        define("nor", opcd(31), _ra, _rs, _rb, xo_21_30(124), _rc);
        define("eqv", opcd(31), _ra, _rs, _rb, xo_21_30(284), _rc);
        define("andc", opcd(31), _ra, _rs, _rb, xo_21_30(60), _rc);
        define("orc", opcd(31), _ra, _rs, _rb, xo_21_30(412), _rc);

        define("extsb", opcd(31), _ra, _rs, _res_16_20, xo_21_30(954), _rc);
        define("extsh", opcd(31), _ra, _rs, _res_16_20, xo_21_30(922), _rc);
        define64("extsw", opcd(31), _ra, _rs, _res_16_20, xo_21_30(986), _rc);
        define64("cntlzd", opcd(31), _ra, _rs, _res_16_20, xo_21_30(58), _rc);
        define("cntlzw", opcd(31), _ra, _rs, _res_16_20, xo_21_30(26), _rc);

        defineP5("popcntb", opcd(31), _ra, _rs, _res_16_20, xo_21_30(122), _res_31);
    }

    private void generateFixedPointRotates() {
        define64("rldicl", opcd(30), _ra, _rs, _sh64, _mb64, xo_27_29(0), _rc);
        define64("rldicr", opcd(30), _ra, _rs, _sh64, _me64, xo_27_29(1), _rc);
        define64("rldic", opcd(30), _ra, _rs, _sh64, _mb64, xo_27_29(2), _rc);
        define("rlwinm", opcd(21), _ra, _rs, _sh, _mb, _me, _rc);
        define64("rldcl", opcd(30), _ra, _rs, _rb, _mb64, xo_27_30(8), _rc);
        define64("rldcr", opcd(30), _ra, _rs, _rb, _me64, xo_27_30(9), _rc);
        define("rlwnm", opcd(23), _ra, _rs, _rb, _mb, _me, _rc);
        define64("rldimi", opcd(30), _ra, _rs, _sh64, _mb64, xo_27_29(3), _rc);
        define("rlwimi", opcd(20), _ra, _rs, _sh, _mb, _me, _rc);
    }

    private void generateFixedPointShifts() {
        define64("sld", opcd(31), _ra, _rs, _rb, xo_21_30(27), _rc);
        define("slw", opcd(31), _ra, _rs, _rb, xo_21_30(24), _rc);
        define64("srd", opcd(31), _ra, _rs, _rb, xo_21_30(539), _rc);
        define("srw", opcd(31), _ra, _rs, _rb, xo_21_30(536), _rc);
        define64("sradi", opcd(31), _ra, _rs, _sh64, xo_21_29(413), _rc);
        define("srawi", opcd(31), _ra, _rs, _sh, xo_21_30(824), _rc);
        define64("srad", opcd(31), _ra, _rs, _rb, xo_21_30(794), _rc);
        define("sraw", opcd(31), _ra, _rs, _rb, xo_21_30(792), _rc);
    }

    private void generateMoveToFromSystemRegisters() {
        define("mtspr", opcd(31), _spr, _rs, xo_21_30(467), _res_31);
        define("mfspr", opcd(31), _rt, _spr, xo_21_30(339), _res_31);
        define("mtcrf", opcd(31), _fxm, _rs, bit_11(0), _res_20, xo_21_30(144), _res_31);
        define("mfcr", opcd(31), _rt, bit_11(0), _res_12_20, xo_21_30(19), _res_31);
    }

    private void generateFloatingPointLoads() {

        define("lfs", opcd(48), _frt, _d, "(", _ra0_notR0, ")");
        define("lfsx", opcd(31), _frt, _ra0_notR0, _rb, xo_21_30(535), _res_31);
        define("lfsu", opcd(49), _frt, _d, "(", _ra_notR0, ")");
        define("lfsux", opcd(31), _frt, _ra_notR0, _rb, xo_21_30(567), _res_31);

        define("lfd", opcd(50), _frt, _d, "(", _ra0_notR0, ")");
        define("lfdx", opcd(31), _frt, _ra0_notR0, _rb, xo_21_30(599), _res_31);
        define("lfdu", opcd(51), _frt, _d, "(", _ra_notR0, ")");
        define("lfdux", opcd(31), _frt, _ra_notR0, _rb, xo_21_30(631), _res_31);
    }

    private void generateFloatingPointStores() {

        define("stfs", opcd(52), _frs, _d, "(", _ra0_notR0, ")");
        define("stfsx", opcd(31), _frs, _ra0_notR0, _rb, xo_21_30(663), _res_31);
        define("stfsu", opcd(53), _frs, _d, "(", _ra_notR0, ")");
        define("stfsux", opcd(31), _frs, _ra_notR0, _rb, xo_21_30(695), _res_31);

        define("stfd", opcd(54), _frs, _d, "(", _ra0_notR0, ")");
        define("stfdx", opcd(31), _frs, _ra0_notR0, _rb, xo_21_30(727), _res_31);
        define("stfdu", opcd(55), _frs, _d, "(", _ra_notR0, ")");
        define("stfdux", opcd(31), _frs, _ra_notR0, _rb, xo_21_30(759), _res_31);
    }

    private void generateFloatingPointMoves() {
        define("fmr", opcd(63), _frt, _res_11_15, _frb, xo_21_30(72), _rc);
        define("fneg", opcd(63), _frt, _res_11_15, _frb, xo_21_30(40), _rc);
        define("fabs", opcd(63), _frt, _res_11_15, _frb, xo_21_30(264), _rc);
        define("fnabs", opcd(63), _frt, _res_11_15, _frb, xo_21_30(136), _rc);
    }

    private void generateFloatingPointAriths() {
        define("fadd", opcd(63), _frt, _fra, _frb, _res_21_25, xo_26_30(21), _rc);
        define("fadds", opcd(59), _frt, _fra, _frb, _res_21_25, xo_26_30(21), _rc);
        define("fsub", opcd(63), _frt, _fra, _frb, _res_21_25, xo_26_30(20), _rc);
        define("fsubs", opcd(59), _frt, _fra, _frb, _res_21_25, xo_26_30(20), _rc);
        define("fmul", opcd(63), _frt, _fra, _res_16_20, _frc, xo_26_30(25), _rc);
        define("fmuls", opcd(59), _frt, _fra, _res_16_20, _frc, xo_26_30(25), _rc);
        define("fdiv", opcd(63), _frt, _fra, _frb, _res_21_25, xo_26_30(18), _rc);
        define("fdivs", opcd(59), _frt, _fra, _frb, _res_21_25, xo_26_30(18), _rc);

        define("fmadd", opcd(63), _frt, _fra, _frc, _frb, xo_26_30(29), _rc);
        define("fmadds", opcd(59), _frt, _fra, _frc, _frb, xo_26_30(29), _rc);
        define("fmsub", opcd(63), _frt, _fra, _frc, _frb, xo_26_30(28), _rc);
        define("fmsubs", opcd(59), _frt, _fra, _frc, _frb, xo_26_30(28), _rc);
        define("fnmadd", opcd(63), _frt, _fra, _frc, _frb, xo_26_30(31), _rc);
        define("fnmadds", opcd(59), _frt, _fra, _frc, _frb, xo_26_30(31), _rc);
        define("fnmsub", opcd(63), _frt, _fra, _frc, _frb, xo_26_30(30), _rc);
        define("fnmsubs", opcd(59), _frt, _fra, _frc, _frb, xo_26_30(30), _rc);
    }

    private void generateFloatingPointRoundsAndCvts() {
        define("frsp", opcd(63), _frt, _res_11_15, _frb, xo_21_30(12), _rc);
        define64("fctid", opcd(63), _frt, _res_11_15, _frb, xo_21_30(814), _rc);
        define64("fctidz", opcd(63), _frt, _res_11_15, _frb, xo_21_30(815), _rc);
        define("fctiw", opcd(63), _frt, _res_11_15, _frb, xo_21_30(14), _rc);
        define("fctiwz", opcd(63), _frt, _res_11_15, _frb, xo_21_30(15), _rc);
        define64("fcfid", opcd(63), _frt, _res_11_15, _frb, xo_21_30(846), _rc);
    }

    private void generateFloatingPointCompares() {
        define("fcmpu", opcd(63), _bf, _res_9_10, _fra, _frb, xo_21_30(0), _res_31);
        define("fcmpo", opcd(63), _bf, _res_9_10, _fra, _frb, xo_21_30(32), _res_31);
    }

    private void generateFloatingPointStatusAndCRs() {
        define("mffs", opcd(63), _frt, _res_11_15, _res_16_20, xo_21_30(583), _rc);
        define("mcrfs", opcd(63), _bf, _res_9_10, _bfa, _res_14_15, _res_16_20, xo_21_30(64), _res_31);
        define("mtfsfi", opcd(63), _bf, _res_9_10, _res_11_15, _u, _res_20, xo_21_30(134), _rc);
        define("mtfsf", opcd(63), _res_6, _flm, _res_15, _frb, xo_21_30(711), _rc);
        define("mtfsb0", opcd(63), _bt, _res_11_15, _res_16_20, xo_21_30(70), _rc);
        define("mtfsb1", opcd(63), _bt, _res_11_15, _res_16_20, xo_21_30(38), _rc);
    }

    private void generateMoveToFromSystemRegistersOptional() {
        define("mtocrf", opcd(31), _fxm, _rs, bit_11(1), _res_20, xo_21_30(144), _res_31);
        final Method predicateMethod = InstructionConstraint.Static.getPredicateMethod(CRF.class, "isExactlyOneCRFSelected", int.class);
        final InstructionConstraint ic = InstructionConstraint.Static.makePredicate(predicateMethod, _fxm);
        define("mfocrf", opcd(31), _rt, _fxm, bit_11(1), _res_20, xo_21_30(19), _res_31, ic);
    }

    private void generateFloatingPointArithsOptional() {
        define64("fsqrt", opcd(63), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(22), _rc);
        define("fsqrts", opcd(59), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(22), _rc);
        define64("fre", opcd(63), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(24), _rc);
        define("fres", opcd(59), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(24), _rc);
        define64("frsqrte", opcd(63), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(26), _rc);
        define("frsqrtes", opcd(59), _frt, _res_11_15, _frb, _res_21_25, xo_26_30(26), _rc);
    }

    private void generateFloatingPointSelectOptional() {
        define("fsel", opcd(63), _frt, _fra, _frc, _frb, xo_26_30(23), _rc);
    }

    private void generateDeprecated() {
        define("mcrxr", opcd(31), _bf, _res_9_10, _res_11_15, _res_16_20, xo_21_30(512), _res_31);
    }

    private void generateICacheManagement() {
        define("icbi", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(982), _res_31);
    }

    private void generateDCacheManagement() {
        define("dcbt", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(278), _res_31);
        define("dcbtst", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(246), _res_31);
        define("dcbz", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(1014), _res_31);
        define("dcbst", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(54), _res_31);
        define("dcbf", opcd(31), _res_6_10, _ra0_notR0, _rb, xo_21_30(86), _res_31);
    }

    private void generateInstructionSynchronization() {
        define("isync", opcd(19), _res_6_10, _res_11_15, _res_16_20, xo_21_30(150), _res_31);
    }

    private void generateAtomicUpdates() {
        define("lwarx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(20), _res_31);
        define64("ldarx", opcd(31), _rt, _ra0_notR0, _rb, xo_21_30(84), _res_31);
        define("stwcx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(150), bit_31(1)).setExternalName("stwcx.");
        define64("stdcx", opcd(31), _rs, _ra0_notR0, _rb, xo_21_30(214), bit_31(1)).setExternalName("stdcx.");
    }

    private void generateMemoryBarrier() {
        define("sync", opcd(31), _res_6_10, _res_11_15, _res_16_20, xo_21_30(598), _res_31);
        define("eieio", opcd(31), _res_6_10, _res_11_15, _res_16_20, xo_21_30(854), _res_31);
    }
}

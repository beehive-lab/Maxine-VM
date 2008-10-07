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

import static com.sun.max.asm.gen.risc.sparc.SPARCFields.*;

import com.sun.max.asm.gen.risc.field.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
class ConditionalMove extends SPARCInstructionDescriptionCreator {

    private void addIccOrFcc(String suffix, RiscField fmovccField, RiscField movccField, int typeBitContents, int condContents) {
        // A.33
        final Object[] fmovHead = {op(0x2), op3(0x35), bits_18_18(0), cond_17_14(condContents), fmovTypeBit(typeBitContents), fmovccField};
        setCurrentArchitectureManualSection("A.33");
        define("fmovs" + suffix, fmovHead, opfLow_10_5(0x1), _sfrs2, _sfrd);
        define("fmovd" + suffix, fmovHead, opfLow_10_5(0x2), _dfrs2, _dfrd);
        define("fmovq" + suffix, fmovHead, opfLow_10_5(0x3), _qfrs2, _qfrd);
        // A.35
        final Object[] movHead = {op(0x2), op3(0x2c), movTypeBit(typeBitContents), cond_17_14(condContents), movccField};
        setCurrentArchitectureManualSection("A.35");
        define("mov" + suffix, movHead, i(1), _simm11, _rd);
        define("mov" + suffix, movHead, i(0), _res_10_5, _rs2, _rd);
    }

    private void addMovr(String suffix, int rcondContents) {
        // A.34
        final Object[] fmovrHead = {op(0x2), op3(0x35), bits_13_13(0), rcond_12_10(rcondContents)};
        setCurrentArchitectureManualSection("A.34");
        define("fmovrs" + suffix, fmovrHead, opfLow_9_5(0x5), _rs1, _sfrs2, _sfrd);
        define("fmovrd" + suffix, fmovrHead, opfLow_9_5(0x6), _rs1, _dfrs2, _dfrd);
        define("fmovrq" + suffix, fmovrHead, opfLow_9_5(0x7), _rs1, _qfrs2, _qfrd);
        // A.36
        final Object[] movrHead = {op(0x2), op3(0x2f), rcond_12_10(rcondContents)};
        setCurrentArchitectureManualSection("A.36");
        define("movr" + suffix, movrHead, i(0), _res_9_5, _rs1, _rs2, _rd);
        // sparc asm is too lenient with simm10
        define("movr" + suffix, movrHead, i(1), _rs1, _simm10, _rd);
    }

    private void addIcc(String suffix, int condContents) {
        addIccOrFcc(suffix, _fmovicc, _movicc, 1, condContents);
    }

    private void addFcc(String suffix, int condContents) {
        addIccOrFcc(suffix, _fmovfcc, _movfcc, 0, condContents);
    }

    private void create_A33_A35() {
        addIcc("a", 0x8);
        addIcc("n", 0x0);
        addIcc("ne", 0x9);
        addIcc("e", 0x1);
        addIcc("g", 0xa);
        addIcc("le", 0x2);
        addIcc("ge", 0xb);
        addIcc("l", 0x3);
        addIcc("gu", 0xc);
        addIcc("leu", 0x4);
        addIcc("cc", 0xd);
        addIcc("cs", 0x5);
        addIcc("pos", 0xe);
        addIcc("neg", 0x6);
        addIcc("vc", 0xf);
        addIcc("vs", 0x7);

        addFcc("a", 0x8);
        addFcc("n", 0x0);
        addFcc("u", 0x7);
        addFcc("g", 0x6);
        addFcc("ug", 0x5);
        addFcc("l", 0x4);
        addFcc("ul", 0x3);
        addFcc("lg", 0x2);
        addFcc("ne", 0x1);
        addFcc("e", 0x9);
        addFcc("ue", 0xa);
        addFcc("ge", 0xb);
        addFcc("uge", 0xc);
        addFcc("le", 0xd);
        addFcc("ule", 0xe);
        addFcc("o", 0xf);
    }

    private void create_A34_A36() {
        addMovr("e", 0x1);
        addMovr("lez", 0x2);
        addMovr("lz", 0x3);
        addMovr("ne", 0x5);
        addMovr("gz", 0x6);
        addMovr("gez", 0x7);
    }

    ConditionalMove(SPARCTemplateCreator templateCreator) {
        super(templateCreator);

        create_A34_A36();
        create_A33_A35();
    }

}

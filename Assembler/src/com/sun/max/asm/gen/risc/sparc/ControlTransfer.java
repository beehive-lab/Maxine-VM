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
import static com.sun.max.asm.sparc.AnnulBit.*;
import static com.sun.max.asm.sparc.BranchPredictionBit.*;

import com.sun.max.asm.sparc.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
class ControlTransfer extends SPARCInstructionDescriptionCreator {

    private void createBPr(String prefix, Object... objects) {
        for (BPr condition : BPr.SYMBOLIZER) {
            define(prefix + condition.name().toLowerCase(), objects, rcond_27_25(condition));
        }
    }

    private void create_A3() {
        createBPr("br", op(0x0), _a, _p, bits_28_28(0), bits_24_22(0x3), _rs1, _d16);
        createBPr("br", op(0x0), a(NO_A), p(PT), bits_28_28(0), bits_24_22(0x3), _rs1, _d16);
        define("br", op(0x0), _rcond_27_25, _a, _p, bits_28_28(0), bits_24_22(0x3), _rs1, _d16);
    }

    private void createFBfcc(String prefix, Object... objects) {
        for (FBfcc condition : FBfcc.SYMBOLIZER) {
            define(prefix + condition.name().toLowerCase(), objects, fcond_28_25(condition));
        }
    }

    private void create_A4() {
        if (assembly().generatingDeprecatedInstructions()) {
            createFBfcc("fb", op(0x0), _a, bits_24_22(0x6), _disp22);
            createFBfcc("fb", op(0x0), a(NO_A), bits_24_22(0x6), _disp22);
            define("fb", op(0x0), _fcond_28_25, _a, bits_24_22(0x6), _disp22);
        }
    }

    private void create_A5() {
        if (assembly().generatingV9Instructions()) {
            createFBfcc("fb", op(0x0), _a, _p, bits_24_22(0x5), _fcc_21_20, _disp19);
            createFBfcc("fb", op(0x0), a(NO_A), p(PT), bits_24_22(0x5), _fcc_21_20, _disp19);
            define("fb", op(0x0), _fcond_28_25, _a, _p, bits_24_22(0x5), _fcc_21_20, _disp19);
        }
    }

    private void createBicc(String prefix, Object... objects) {
        for (Bicc condition : Bicc.SYMBOLIZER) {
            define(prefix + condition.name().toLowerCase(), objects, icond_28_25(condition));
        }
    }


    private void create_A6() {
        if (assembly().generatingDeprecatedInstructions()) {
            createBicc("b", op(0x0), _a, bits_24_22(0x2), _disp22);
            createBicc("b", op(0x0), a(NO_A), bits_24_22(0x2), _disp22);
            define("b", op(0x0), _icond_28_25, _a, bits_24_22(0x2), _disp22);
        }
    }

    private void create_A7() {
        if (assembly().generatingV9Instructions()) {
            createBicc("b", op(0x0), _a, _p, bits_24_22(0x1), _cc, _disp19);
            createBicc("b", op(0x0), a(NO_A), p(PT), bits_24_22(0x1), _cc, _disp19);
            define("b", op(0x0), _icond_28_25, _a, _p, bits_24_22(0x1), _cc, _disp19);
        }
    }

    private void create_A8() {
        define("call", op(0x1), _disp30);
    }

    private void create_A11() {
        define("done", op(0x2), op3(0x3e), _res_18_0, fcnc(0));
        define("retry", op(0x2), op3(0x3e), _res_18_0, fcnc(1));
    }

    private void create_A22() {
        define("illtrap", op(0x0), _res_29_25, op2(0x0), _const22);
    }

    private void create_A24() {
        define("jmpl", op(0x2), op3(0x38), _rs1, " + ", i(0), _res_12_5, _rs2, _rd);
        define("jmpl", op(0x2), op3(0x38), _rs1, " + ", i(1), _simm13, _rd);
    }

    private void create_A45() {
        final Object[] head = {op(0x2), _res_29_25, op3(0x39), _rs1, " + "};

        // should be called "return", but that's a Java keyword
        define("return_", head, i(0), _res_12_5, _rs2).setExternalName("return");
        define("return_", head, i(1), _simm13).setExternalName("return");
    }

    private void create_A50() {
        define("sir", op(0x2), rd(15), op3(0x30), bits_18_14(0), i(1), _simm13);
    }

    private void create_A61() {
        final Object[] head = {op(0x2), _res_29_29, op3(0x3a), _tcc, _rs1, " + "};
        createBicc("t", head, i(0), _res_10_5, _rs2);
        createBicc("t", head, i(1), _res_10_7, _swTrapNumber);
    }

    ControlTransfer(SPARCTemplateCreator templateCreator) {
        super(templateCreator);

        setCurrentArchitectureManualSection("A.3");
        create_A3();

        setCurrentArchitectureManualSection("A.4");
        create_A4();

        setCurrentArchitectureManualSection("A.5");
        create_A5();

        setCurrentArchitectureManualSection("A.6");
        create_A6();

        setCurrentArchitectureManualSection("A.7");
        create_A7();

        setCurrentArchitectureManualSection("A.8");
        create_A8();

        setCurrentArchitectureManualSection("A.11");
        create_A11();

        setCurrentArchitectureManualSection("A.22");
        create_A22();

        setCurrentArchitectureManualSection("A.24");
        create_A24();

        setCurrentArchitectureManualSection("A.45");
        create_A45();

        setCurrentArchitectureManualSection("A.50");
        create_A50();

        setCurrentArchitectureManualSection("A.61");
        create_A61();

        setCurrentArchitectureManualSection(null);
    }
}

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
/*VCSID=776be073-e302-4110-aeb5-e1b4f99c6c5c*/
package com.sun.max.asm.gen.risc.sparc;

import static com.sun.max.asm.gen.risc.sparc.SPARCFields.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
class MemoryAccess extends SPARCInstructionDescriptionCreator {

    private void create_A9() {
        define("casa",  op(0x3), op3(0x3c), "[", _rs1, "] ", i(0), _immAsi, ",",    _rs2, _rd);
        define("casa",  op(0x3), op3(0x3c), "[", _rs1, "] %asi, ", i(1), _res_12_5, _rs2, _rd);
        define("casxa", op(0x3), op3(0x3e), "[", _rs1, "] ", i(0), _immAsi, ",",    _rs2, _rd);
        define("casxa", op(0x3), op3(0x3e), "[", _rs1, "] %asi, ", i(1), _res_12_5, _rs2, _rd);
    }

    private void create_A20() {
        final Object[] head = {op(0x2), _res_29_25, op3(0x3b), _rs1, " + "};

        define("flush", head, i(0), _res_12_5, _rs2);
        define("flush", head, i(1), _simm13);
    }

    private void createLoad(String name, Object resultRegister, int op3Contents) {
        String internalName = name;
        String resultPrefix = "";
        if (op3Contents == 0x21) {
            internalName += "_fsr";
            resultPrefix = "%fsr";
        }
        final Object[] head = {op(0x3), "[", _rs1, " + ", op3(op3Contents)};
        define(internalName, head, i(0), _res_12_5,  _rs2, "], ", resultPrefix, resultRegister).setExternalName(name);
        define(internalName, head, i(1), _simm13, "], ", resultPrefix, resultRegister).setExternalName(name);
    }

    private void create_A25() {
        createLoad("ld", _sfrd, 0x20);
        createLoad("ldd", _dfrd, 0x23);
        createLoad("ldq", _qfrd, 0x22);
        createLoad("ldx", rd(1), 0x21); // ldxfsr
        if (assembly().generatingDeprecatedInstructions()) {
            createLoad("ld", rd(0), 0x21); // ldfsr
            createLoad("swap", _rd, 0xf);
        }
    }

    private void createLoadA(String name, Object resultRegister, int op3Contents) {
        final Object[] head = {op(0x3), "[", _rs1, " + ", op3(op3Contents)};
        define(name, head, i(0), _rs2, "] ", _immAsi, resultRegister);
        define(name, head, i(1), _simm13, "] %asi, ", resultRegister);
    }

    private void create_A26() {
        createLoadA("lda", _sfrd, 0x30);
        createLoadA("ldda", _dfrd, 0x33);
        createLoadA("ldqa", _qfrd, 0x32);
    }

    private void create_A27() {
        createLoad("ldsb", _rd, 0x9);
        createLoad("ldsh", _rd, 0xa);
        createLoad("ldsw", _rd, 0x8);
        createLoad("ldub", _rd, 0x1);
        createLoad("lduh", _rd, 0x2);
        createLoad("lduw", _rd, 0x0);
        createLoad("ldx", _rd, 0xb);
        if (assembly().generatingDeprecatedInstructions()) {
            createLoad("ldd", _rd_even, 0x3);
        }
    }

    private void create_A28() {
        createLoadA("ldsba", _rd, 0x19);
        createLoadA("ldsha", _rd, 0x1a);
        createLoadA("ldswa", _rd, 0x18);
        createLoadA("lduba", _rd, 0x11);
        createLoadA("lduha", _rd, 0x12);
        createLoadA("lduwa", _rd, 0x10);
        createLoadA("ldxa", _rd, 0x1b);

        if (assembly().generatingDeprecatedInstructions()) {
            createLoadA("ldda", _rd_even, 0x13);
        }
    }

    private void create_A29() {
        createLoad("ldstub", _rd, 0xd);
    }

    private void create_A30() {
        createLoadA("ldstuba", _rd, 0x1d);
    }

    private void create_A41() {
        define("prefetch", op(0x3), op3(0x2d), "[", _rs1, " + ", i(0), _res_12_5, _rs2, "], ", _fcn);
        define("prefetch", op(0x3), op3(0x2d), "[", _rs1, " + ", i(1), _simm13, "], ", _fcn);
        define("prefetcha", op(0x3), op3(0x3d), "[", _rs1, " + ", i(0), _rs2, "] ", _immAsi, _fcn);
        define("prefetcha", op(0x3), op3(0x3d), "[", _rs1, " + ", i(1), _simm13, "] %asi, ", _fcn);
    }

    private void createStore(String name, Object fromRegister, int op3Contents) {
        String internalName = name;
        Object openBracket = new Object[]{", [", };
        if (op3Contents == 0x25) {
            internalName += "_fsr";
            openBracket = " %fsr, [";
        }
        final Object[] head = {op(0x3), op3(op3Contents), fromRegister, openBracket, _rs1, " + "};
        define(internalName, head, i(0), _res_12_5, _rs2, "]").setExternalName(name);
        define(internalName, head, i(1), _simm13, "]").setExternalName(name);
    }

    private void create_A51() {
        createStore("st", _sfrd, 0x24);
        createStore("std", _dfrd, 0x27);
        createStore("stq", _qfrd, 0x26);
        createStore("stx", rd(1), 0x25);
        if (assembly().generatingDeprecatedInstructions()) {
            createStore("st", rd(0), 0x25);
        }
    }

    private void createStoreA(String name, Object fromRegister, int op3Contents) {
        final Object[] head = {op(0x3), op3(op3Contents), fromRegister, ", [", _rs1, " + "};
        define(name, head, i(0), _rs2, "]", _immAsi);
        define(name, head, i(1), _simm13, "] %asi");
    }

    private void create_A52() {
        createStoreA("sta", _sfrd, 0x34);
        createStoreA("stda", _dfrd, 0x37);
        createStoreA("stqa", _qfrd, 0x36);
    }

    private void create_A53() {
        createStore("stb", _rd, 0x5);
        createStore("sth", _rd, 0x6);
        createStore("stw", _rd, 0x4);
        createStore("stx", _rd, 0xe);

        if (assembly().generatingDeprecatedInstructions()) {
            createStore("std", _rd_even, 0x7);
        }
    }

    private void create_A54() {
        createStoreA("stba", _rd, 0x15);
        createStoreA("stha", _rd, 0x16);
        createStoreA("stwa", _rd, 0x14);
        createStoreA("stxa", _rd, 0x1e);

        if (assembly().generatingDeprecatedInstructions()) {
            createStoreA("stda", _rd_even, 0x17);
        }
    }

    MemoryAccess(SPARCTemplateCreator templateCreator) {
        super(templateCreator);

        setCurrentArchitectureManualSection("A.9");
        create_A9();

        setCurrentArchitectureManualSection("A.20");
        create_A20();

        setCurrentArchitectureManualSection("A.25");
        create_A25();

        setCurrentArchitectureManualSection("A.26");
        create_A26();

        setCurrentArchitectureManualSection("A.27");
        create_A27();

        setCurrentArchitectureManualSection("A.28");
        create_A28();

        setCurrentArchitectureManualSection("A.29");
        create_A29();

        setCurrentArchitectureManualSection("A.30");
        create_A30();

        setCurrentArchitectureManualSection("A.41");
        create_A41();

        setCurrentArchitectureManualSection("A.51");
        create_A51();

        setCurrentArchitectureManualSection("A.52");
        create_A52();

        setCurrentArchitectureManualSection("A.53");
        create_A53();

        setCurrentArchitectureManualSection("A.54");
        create_A54();
    }

}

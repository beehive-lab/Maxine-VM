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

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
class IntegerArithmetic extends SPARCInstructionDescriptionCreator {

    private void createBinaryArithmetic(String name, int op3Contents) {
        define(name, op(0x2), rs1, i(0), res_12_5, rs2, rd, op3(op3Contents));
        define(name, op(0x2), rs1, i(1), simm13, rd, op3(op3Contents));
    }

    private void create_A2() {
        createBinaryArithmetic("add", 0x0);
        createBinaryArithmetic("addc", 0x8);
        createBinaryArithmetic("addcc", 0x10);
        createBinaryArithmetic("addccc", 0x18);
    }

    private void create_A10() {
        if (assembly().generatingDeprecatedInstructions()) {
            createBinaryArithmetic("udiv", 0xe);
            createBinaryArithmetic("sdiv", 0xf);
            createBinaryArithmetic("udivcc", 0x1e);
            createBinaryArithmetic("sdivcc", 0x1f);
        }
    }

    private void create_A31() {
        createBinaryArithmetic("and", 0x1);
        createBinaryArithmetic("andcc", 0x11);
        createBinaryArithmetic("andn", 0x5);
        createBinaryArithmetic("andncc", 0x15);
        createBinaryArithmetic("or", 0x2);
        createBinaryArithmetic("orcc", 0x12);
        createBinaryArithmetic("orn", 0x6);
        createBinaryArithmetic("orncc", 0x16);
        createBinaryArithmetic("xor", 0x3);
        createBinaryArithmetic("xorcc", 0x13);
        createBinaryArithmetic("xnor", 0x7);

        createBinaryArithmetic("xnorcc", 0x17);
    }

    private void create_A36() {
        if (assembly().generatingV9Instructions()) {
            createBinaryArithmetic("mulx", 0x9);
            createBinaryArithmetic("sdivx", 0x2d);
            createBinaryArithmetic("udivx", 0xd);
        }
    }

    private void create_A38() {
        if (assembly().generatingDeprecatedInstructions()) {
            createBinaryArithmetic("umul", 0xa);
            createBinaryArithmetic("smul", 0xb);
            createBinaryArithmetic("umulcc", 0x1a);
            createBinaryArithmetic("smulcc", 0x1b);
        }
    }

    private void create_A39() {
        if (assembly().generatingDeprecatedInstructions()) {
            createBinaryArithmetic("mulscc", 0x24);
        }
    }

    private void create_A41() {
        define("popc", op(0x2), res_18_14, i(0), res_12_5, rs2, rd, op3(0x2e));
        define("popc", op(0x2), res_18_14, i(1), simm13, rd, op3(0x2e));
    }

    private void create_A48() {
        define("sethi", op(0x0), op2(0x4), imm22, rd);
    }

    private void createShift(String name, int op3Contents) {
        final Object[] head = {op(0x2), rs1, op3(op3Contents)};
        define(name, head, i(0), x(0), res_11_5, rs2, rd);
        define(name + "x", head, i(0), x(1), res_11_5, rs2, rd);
        define(name, head, i(1), x(0), res_11_5, shcnt32, rd);
        define(name + "x", head, i(1), x(1), res_11_6, shcnt64, rd);
    }

    private void create_A49() {
        createShift("sll", 0x25);
        createShift("srl", 0x26);
        createShift("sra", 0x27);
    }

    private void create_A56() {
        createBinaryArithmetic("sub", 0x4);
        createBinaryArithmetic("subcc", 0x14);
        createBinaryArithmetic("subc", 0xc);
        createBinaryArithmetic("subccc", 0x1c);
    }

    private void create_A59() {
        createBinaryArithmetic("taddcc", 0x20);
        if (assembly().generatingDeprecatedInstructions()) {
            createBinaryArithmetic("taddcctv", 0x22);
        }
    }

    private void create_A60() {
        createBinaryArithmetic("tsubcc", 0x21);
        if (assembly().generatingDeprecatedInstructions()) {
            createBinaryArithmetic("tsubcctv", 0x23);
        }
    }

    IntegerArithmetic(SPARCTemplateCreator templateCreator) {
        super(templateCreator);

        setCurrentArchitectureManualSection("A.2");
        create_A2();

        setCurrentArchitectureManualSection("A.10");
        create_A10();

        setCurrentArchitectureManualSection("A.31");
        create_A31();

        setCurrentArchitectureManualSection("A.36");
        create_A36();

        setCurrentArchitectureManualSection("A.38");
        create_A38();

        setCurrentArchitectureManualSection("A.39");
        create_A39();

        setCurrentArchitectureManualSection("A.41");
        create_A41();

        setCurrentArchitectureManualSection("A.48");
        create_A48();

        setCurrentArchitectureManualSection("A.49");
        create_A49();

        setCurrentArchitectureManualSection("A.56");
        create_A56();

        setCurrentArchitectureManualSection("A.59");
        create_A59();

        setCurrentArchitectureManualSection("A.60");
        create_A60();
    }
}

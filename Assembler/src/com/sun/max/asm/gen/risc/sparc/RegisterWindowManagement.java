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
/*VCSID=3ae8cc88-e204-49be-9e7a-ecda1f52dcbb*/
package com.sun.max.asm.gen.risc.sparc;

import static com.sun.max.asm.gen.risc.sparc.SPARCFields.*;

/**
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public class RegisterWindowManagement extends SPARCInstructionDescriptionCreator {

    private void createSaveOrRestore(String name, int op3Contents) {
        define(name, op(0x2), _rs1, op3(op3Contents), i(0), _res_12_5, _rs2, _rd);
        define(name, op(0x2), _rs1, op3(op3Contents), i(1), _simm13, _rd);
    }

    private void create_A21() {
        define("flushw", op(0x2), _res_29_25, op3(0x2b), _res_18_14, i(0), _res_12_0);
    }

    private void create_A45() {
        createSaveOrRestore("save", 0x3c);
        createSaveOrRestore("restore", 0x3d);
    }

    private void create_A46() {
        define("saved", op(0x2), fcnc(0), op3(0x31), _res_18_0);
        define("restored", op(0x2), fcnc(1), op3(0x31), _res_18_0);
    }

    RegisterWindowManagement(SPARCTemplateCreator templateCreator) {
        super(templateCreator);

        setCurrentArchitectureManualSection("A.21");
        create_A21();

        setCurrentArchitectureManualSection("A.45");
        create_A45();

        setCurrentArchitectureManualSection("A.46");
        create_A46();
    }
}

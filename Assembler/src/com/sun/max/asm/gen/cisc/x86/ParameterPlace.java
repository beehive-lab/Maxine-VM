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
package com.sun.max.asm.gen.cisc.x86;

/**
 * The place/field into which the argument value passed to a parameter is to be assembled.
 *
 * @author Bernd Mathiske
 */
public enum ParameterPlace {

    MOD_REG("reg field of the modR/M byte"),
    MOD_REG_REXR("mod field extension by REX.R bit"),
    MOD_RM("rm field of the modR/M byte"),
    MOD_RM_REXB("rm field extension by REX.B bit"),
    SIB_SCALE("scale field of the SIB byte"),
    SIB_INDEX("index field of the SIB byte"),
    SIB_INDEX_REXX("SIB index field extension by REX.X bit"),
    SIB_BASE("base field of the SIB byte"),
    SIB_BASE_REXB("SIB base field extension by REX.B bit"),
    APPEND("appended to the instruction"),
    OPCODE1("added to the first opcode"),
    OPCODE1_REXB("opcode1 extension by REX.B bit"),
    OPCODE2("added to the second opcode"),
    OPCODE2_REXB("opcode2 extension by REX.B bit");

    private final String comment;

    private ParameterPlace(String comment) {
        this.comment = comment;
    }

    public String comment() {
        return comment;
    }
}

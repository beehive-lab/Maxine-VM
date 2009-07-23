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
package com.sun.max.asm.gen;

import com.sun.max.asm.*;

/**
 * Thrown when {@link Assembly#assemble(Assembler, Template, com.sun.max.collect.IndexedSequence)} cannot assemble
 * a template and a set of arguments because the given assembler does not include a method generated from the
 * template. This will be the case if {@linkplain Template#isRedundant() redundant} templates were
 * {@linkplain AssemblerGenerator#generateRedundantInstructionsOption ignored} when the assembler was generated.
 * A disassembler always works with the complete set of redundant templates.
 *
 * @author Doug Simon
 */
public class NoSuchAssemblerMethodError extends NoSuchMethodError {

    public final Template template;

    public NoSuchAssemblerMethodError(String message, Template template) {
        super(message);
        this.template = template;
    }
}

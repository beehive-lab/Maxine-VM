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
/*VCSID=de983304-a916-4c23-8959-a7dc7eb4a9ee*/
/**
 * Java Virtual Machine instruction and opcode definitions and facilities for dealing with them. Included in this package:
 * <ul>
 * <li> A visitor pattern based framework for {@linkplain BytecodeScanner decoding} and
 * {@linkplain BytecodeVisitor processing} individual bytecode instructions from an instruction stream.</li>
 * <li></li>
 * </ul>
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
package com.sun.max.vm.bytecode;

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
/**
 * A complete assembly system for AMD 64 processors in "64-bit mode".
 * 
 * We have the capability to include instructions with 32-bit or 16-bit addressing,
 * but by default we don't.

 * Once the AMD4RawAssembler and AM64LabelAssembler have been generated,
 * this package can be used separate from the framework
 * by importing the following assembler packages only:
 * 
 *     com.sun.max.asm
 *     com.sun.max.asm.x86
 *     com.sun.max.asm.amd64
 *     com.sun.max.asm.amd64.complete
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.asm.amd64.complete;

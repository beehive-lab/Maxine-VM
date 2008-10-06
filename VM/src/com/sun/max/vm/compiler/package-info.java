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
/*VCSID=ba1c9cad-7ba9-4671-9534-877276c74605*/
/**
 * The compiler framework of the MaxineVM VM.
 * It contains several alternative "layered" compilers
 * that use different sequences of intermediate representations (IRs).
 * Each IR layer has interfaces that make its implementation replaceable.
 * This includes replacing subsequent IRs with different ones.
 * 
 * Here is a sequence of existing IRs that can be used in alphabetical order:
 * 
 * - AIR: Actors, internal representations of Java structures, @see com.sun.max.vm.actor
 * 
 * - BIR: Basic Blocks of Bytes codes resulting from control flow analysis, @see com.sun.max.vm.compiler.bir
 * 
 * - CIR: Continuation Passing Style, @see com.sun.max.vm.compiler.bir
 * 
 * - DIR: Direct Style, @see com.sun.max.vm.compiler.dir
 * 
 * - EIR: Effective IR, the last step before the executable target, @see com.sun.max.vm.compiler.eir
 *        Annotated machine instructions in a control flow graph.
 *        There is a different variant for each ISA.
 * 
 * - FIR: Final, 1-to-1 mapping to machine instructions, @see com.sun.max.vm.compiler.fir
 *        Used by our ISA simulator (see project MaxineSimulator).
 *        There is a different variant for each ISA.
 *
 * Target machine code is typically generated from EIR.
 * Alternatively, we generate FIR and simulate it,
 * thus sparing disassembling of machine code.
 *
 * @author Bernd Mathiske
 */
package com.sun.max.vm.compiler;

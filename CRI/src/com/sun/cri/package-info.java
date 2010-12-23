/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
 * A virtual machine compiler-runtime interface (CRI).
 * <p>
 * Specifically, this package defines an interface between the compiler and the runtime system of a virtual machine for
 * the instruction set defined in {@link com.sun.cri.bytecode.Bytecodes}. The interface has three components:
 * <ol>
 * <li>the {@link com.sun.cri.ci compiler-provided interface} that must be used by the runtime.
 * <li>the {@link com.sun.cri.ri runtime-provided interface} that must be used by the compiler.
 * <li>the {@link com.sun.cri.xir XIR interface} for translating object operations.
 * </ol>
 *
 * The interface is independent of any particular compiler or runtime implementation.
 * <p>
 * For more details see <a href="http://wikis.sun.com/download/attachments/173802383/vee2010.pdf">Improving Compiler-Runtime Separation with XIR</a>.
 */
package com.sun.cri;

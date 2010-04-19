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
 * The runtime-provided part of the bi-directional interface between the compiler and the runtime system of a virtual machine for the
 * instruction set defined in {@link com.sun.cri.bytecode.Bytecodes}.
 * <p>
 * Unlike the {@link com.sun.cri.ci compiler-provided interface}, the runtime-provided interface is specified largely
 * using interfaces, that must be implemented by classes provided by a specific runtime implementation.
 * <p>
 * {@link com.sun.cri.ri.RiRuntime} encapsulates the main functionality of the runtime for the compiler.
 * <p>
 * Types (i.e., primitives, classes and interfaces}, fields and methods are represented by {@link com.sun.cri.ri.RiType}, {@link com.sun.cri.ri.RiField} and {@link com.sun.cri.ri.RiMethod}, respectively, with additional support from
 * {@link com.sun.cri.ri.RiSignature} and {@link com.sun.cri.ri.RiExceptionHandler}. Access to the runtime constant pool
 * is through {@link com.sun.cri.ri.RiConstantPool}.
 */
package com.sun.cri.ri;

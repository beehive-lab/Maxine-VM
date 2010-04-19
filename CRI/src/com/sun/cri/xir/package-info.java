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
 * XIR defines a domain specific instruction set for expressing the lowering of bytecode operations. The details of the
 * lowering operations are entirely encapsulated in the runtime and are provided to the compiler on request using
 * {@link com.sun.cri.xir.XirSnippet XIR snippets}. A snippet is a combination of a {@link com.sun.cri.xir.XirTemplate
 * template}, which is a sequence of {@link com.sun.cri.xir.CiXirAssembler.XirInstruction XIR instructions} that has
 * unbound {@link com.sun.cri.xir.CiXirAssembler.XirParameter parameters}, and site-specific
 * {@link com.sun.cri.xir.XirArgument arguments} that are bound to the parameters.
 * <p>
 * The runtime is responsible for creating the {@link com.sun.cri.xir.XirTemplate templates} and provides these to the
 * compiler as part of the initialization process.
 * <p>
 * The XIR instruction set has no textual representation, and therefore no parser. An assembly is represented by an
 * instance of {@link com.sun.cri.xir.CiXirAssembler}, which provides methods to create instructions and operands.
 */
package com.sun.cri.xir;

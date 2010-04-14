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
 * The compiler-provided part of the bi-directional interface between a compiler and a virtual machine (runtime) for the instruction set defined in
 * {@link com.sun.cri.bytecode.Bytecodes}.
 *
 * The target hardware architecture is represented by {@link com.sun.cri.ci.CiArchitecture} and the specific target machine
 * environment for a compiler instance is represented by {@link com.sun.cri.ci.CiTarget}.
 * <p>
 * A compiler instance is represented by {@link com.sun.cri.ci.CiCompiler} which can compile a single method,
 * but with possible inlining, and returns a {@link com.sun.cri.ci.CiResult}. A {@code CiResult} encapsulates
 * {@linkplain com.sun.cri.ci.CiStatistics compilation statistics}, possible {@linkplain com.sun.cri.ci.CiBailout error state}
 * and the {@linkplain com.sun.cri.ci.CiTargetMethod compiled code and metadata}.
 * {@link com.sun.cri.ci.CiCodePos} and {@link com.sun.cri.ci.CiDebugInfo} provide detailed information to the
 * virtual machine to support debugging and deoptimization of the compiled code.
 * <p>
 * The compiler manipulates {@link com.sun.cri.ci.CiValue} instances that have a {@link com.sun.cri.ci.CiKind}, and are
 * immutable. A concrete {@link com.sun.cri.ci.CiValue value} is one of the following subclasses:
 * <ul>
 * <li>{@link com.sun.cri.ci.CiConstant}: a constant value.
 * <li>{@link com.sun.cri.ci.CiRegisterValue}: a value stored in a {@linkplain com.sun.cri.ci.CiRegister target machine register}.
 * <li>{@link com.sun.cri.ci.CiStackSlot}: a spill slot or an outgoing stack-based argument in a method's frame.
 * <li>{@link com.sun.cri.ci.CiAddress}: an address in target machine memory.
 * <li>{@link com.sun.cri.ci.CiVariable}: a value (cf. virtual register) that is yet to be bound to a target machine location (physical register or memory address).
 *</ul>
 *
 * @author Ben Titzer
 */
package com.sun.cri.ci;


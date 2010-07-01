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
 * Direct style Intermediate Representation (DIR).
 * 
 * This IR has a platform-independent control flow graph structure with an infinite register set.
 * It reuses the canonical compiler builtins by wrapping them into DirBuiltin objects.
 * (In principle, platform-dependent builtins can be used as well,
 *  we just have not gotten to that yet.)
 * 
 * In principle, DIR could be used for optimizations,
 * but we only use it as an intermediate step to get from CIR to EIR.
 * 
 * A compilation unit of DIR consists in a sequence of DIR blocks.
 * Each block contains a sequence of EIR instructions.
 * The first instruction of the first block is the call entry point.
 * Each block has references to its successors and predecessors.
 * The usual block roles are represented by an enum field:
 * there are "normal blocks" and "catch blocks", the latter implementing exception dispatchers.
 * 
 * There are only a few different kinds of instructions:
 * assignment, goto, return, throw, switch, safepoint, call.
 * Calls are further differentiated as method calls and builtin calls.
 * Whereas safepoints may be builtins in other IRs,
 * they are separated out here to carry an extra Java frame descriptor field,
 * which would otherwise be overhead for every builtin call.
 * 
 * Instructions carry operand values of type DirValue.
 * Each value can be either a constant or a variable.
 * 
 * @author Hiroshi Yamauchi
 * @author Bernd Mathiske
 */
package com.sun.max.vm.cps.dir;

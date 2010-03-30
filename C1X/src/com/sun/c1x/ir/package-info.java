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
package com.sun.c1x.ir;

/**
 * @author Ben Titzer
 *
 *         Classes that define the (High-level) Intermediate Representation (HIR) if the C1X compiler.
 *
 *         HIR instances are created by processing Java bytecodes and are Directed Acyclic Graphs (DAGs). All nodes in
 *         an HIR graph are concrete subclasses of the abstract class {@link Value}. This indicates a property of HIR,
 *         namely that everything is a value, including instructions. This allows an operand for an instruction node to
 *         refer directly to the node that generated the value, which might, for example, be another instruction.
 */

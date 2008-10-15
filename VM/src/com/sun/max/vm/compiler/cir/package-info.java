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
 * Continuation passing style Intermediate Representation (CIR).
 * 
 * This IR is used for all kinds of optimizations.
 * It is based on CPS (Continuation Passing Style).
 * 
 * CPS is used in many imperative-functional language compilers
 * (e.g. for languages like Scheme, ML, Haskell).
 * 
 * The general idea to use this category of IR for an object-oriented language
 * can be traced back to experiences (with a Smalltalk backend and)
 * with the Tycoon and TooL systems, pioneered by Andreas Gawecki
 * at the University of Hamburg, 1991-96.
 * 
 * 
 * Keywords:
 * ---------
 *  { }    bracket a closure
 *  [ ]    bracket a parameter list
 *  ( )    bracket an argument list
 *  > <    expresses a wrapper object around another object
 *  PROC   declares a closure abstraction
 *  CONT   declares a continuation closure abstraction
 *  BLOCK  declares an "block", i.e. a basic block closure abstraction
 *  < >    denote a list with zero or more terms of the same kind:
 * 
 *  <X> =
 *  <X> = X <X>
 * 
 * 
 * Quasi-Terminals:
 * ----------------
 * 
 * variable, constant, builtin, nativeMethod
 * 
 * 
 * Non-Terminals aka Rules:
 * ------------------------
 * 
 * Node = Call | Value
 * 
 * Call = Value (<Value>)
 * 
 * Value = Procedure | variable | constant
 * 
 * Procedure = Closure | Block | Method | builtin
 * 
 * Closure = Continuation | {PROC [<variable> variable variable] Call}
 * 
 * Continuation = {CONT [] Call} | {CONT [variable] Call}
 * 
 * Block = >Closure<
 * 
 * Method = CompiledMethod | nativeMethod
 * 
 * CompiledMethod = >Closure<
 *
 *
 * A {@linkplain CirMethod CompiledMethod} is a wrapper for a {@linkplain CirClosure closure} that denotes the method's code.
 * 
 * A {@linkplain CirBlock Block} is a wrapper for a closure that can be called more than once within the compilation unit.
 * We use this construct instead of the common Y-combinator (or any other fixpoint operator) to express recursive declarations and loops.
 * After some initial transformations that immediately follow translation to CIR,
 * all blocks are fully parameterized, i.e. they have no free variables.
 *
 * Normal closures and builtins have two continuation parameters:
 *     cc - the normal continuation
 *     ce - the exception continuation
 *
 * @author Bernd Mathiske
 */
package com.sun.max.vm.compiler.cir;

/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
package com.sun.max.vm.cps.cir;

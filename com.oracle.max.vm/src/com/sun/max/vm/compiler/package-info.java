/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.max.vm.compiler;

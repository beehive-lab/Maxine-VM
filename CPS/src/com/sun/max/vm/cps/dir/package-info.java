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

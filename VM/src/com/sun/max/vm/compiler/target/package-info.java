/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
 * Representations of target machine code and associated objects.
 * 
 * A "target method" is a heap object aggregating all the parts and pieces constituting an executable binary of a Java method.
 * It is reachable via the corresponding class method actor's method dock.
 * 
 * Each target method has references to co-located objects with fixed addresses in a code region.
 * These objects contain: machine code, literals, auxiliary exception handling data and auxiliary trampoline data.
 * The code region allocation unit (cell) containing these objects is called a "target bundle".
 * 
 * @author Bernd Mathiske
 */
package com.sun.max.vm.compiler.target;

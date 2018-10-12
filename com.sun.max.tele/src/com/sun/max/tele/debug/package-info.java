/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Facilities for remote debugging of the Maxine VM, including:
 * <ul>
 * <li>extraction of thread, register, stack, thread local blocks, and native library state;</li>
 * <li>basic process controls;</li>
 * <li>"target breakpoint (machine-level) creation and management;</li>
 * <li>"bytecode" breakpoint (deferred, set on unloaded methods) creation and management;</li>
 * <li>worker thread for dequeuing process execution requests; and</li>
 * <li>watchpoints, both "native" (on a fixed memory location) and "object" (on a field in a specific object instance,
 * following object relocation).</li>
 * </ul>
 * <p>
 * Depends partially on the VM class {@link com.sun.max.vm.tele.InspectableCompilationInfo}.
 * <p>
 * The abstract class {@link com.sun.max.tele.debug.TeleProcess} manages most of these services, with platform-specific
 * concrete classes supplying very minor specializations.
 */
package com.sun.max.tele.debug;

/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Access to the top-level view of memory allocations in the VM.
 * <p>
 * The class {@link com.sun.max.tele.memory.VmAddressSpace} maintains a sorted list of
 * memory regions known to have been allocated from the OS, both by the running VM itself
 * and by the OS, for example for dynamically loaded libraries.  Other areas of inspection
 * are expected to report all such allocations, as soon as they become known, using method
 * {@link com.sun.max.tele.memory.VmAddressSpace#add(com.sun.max.tele.MaxEntityMemoryRegion)}.
 */
package com.sun.max.tele.memory;

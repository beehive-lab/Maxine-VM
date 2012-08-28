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
 * Access to compiled code, mainly Java method compilations managed by the VM, but also including dynamically linked
 * <em>native methods</em> as they are discovered. Depends partially on the VM class
 * {@link com.sun.max.vm.tele.InspectableCodeInfo}.
 * <p>
 * Access to specific method code is provided for Maxine VM methods whose source declarations have been annotated with
 * {@linkplain com.sun.max.annotate.INSPECTED @INSPECTED}. The class {@link com.sun.max.tele.method.VmMethodAccess},
 * when run as an application, modifies itself by the addition of a specific method accessor for each such method.
 * Accessors are subclasses of {@link com.sun.max.tele.method.TeleMethodAccess}, specialized for the kind of method
 * (instance vs. static).
 * <p>
 * A subset of the specific, annotated methods are called out as particularly interesting. A list of those that might be
 * useful for users are made available by the method
 * {@link com.sun.max.tele.method.VmMethodAccess#clientInspectableMethods()}. Methods of specific interest to other
 * system services, for example compilation start/stop and GC phase changes, are made available via specific
 * {@link com.sun.max.tele.method.VmMethodAccess} methods.
 * <p>
 * Access to <em>all</em> method compilations, including dynamically located <em>native methods</em> is made available
 * by the singleton instance of class {@link com.sun.max.tele.method.VmCodeCacheAccess}. The code cache consists of
 * several parts, currently three, each of which is managed differently for different purposes:
 * <ul>
 * <li>the <em>boot code cache</em> is part of the VM's binary image and contains (at minimum) all of the method
 * compilations needed to start up the VM and bring it to the point where further classes can be loaded and method
 * compiled;</li>
 * <li>the <em>dynamic optimized code cache</em> (those compiled by the optimizing compiler); and</li>
 * <li>the <em>dynamic baseline code cache</em> (those compiled by the fast baseline compiler), which, unlike the other
 * two code cache regions, is <em>managed</em> and subject to <em>code eviction</em>.</li>
 * </ul>
 * An instance of {@link com.sun.max.tele.method.VmCodeCacheRegion} is created to model the contents of each code cache
 * region by aggressively cataloging the contents of each upon every refresh. This permits the location of method
 * compilations by memory address in the VM. Each instance also produces an instance of
 * {@link com.sun.max.tele.method.RemoteCodePointerManager} that manages
 * {@linkplain com.sun.max.tele.method.RemoteCodePointer remote code pointers}, which can refer to specific machine code
 * locations in the particular code cache. In the case of a <em>managed</em> code cache region, remote code pointers
 * automatically follow relocated methods and become inactive when methods are evicted completely from the cache.
 * <p>
 * Although the contents of each code cache region are proactively <em>cataloged</em> by the instance of
 * {@link com.sun.max.tele.method.VmCodeCacheRegion}, only a minimal amount of information about each method compilation
 * is kept in the catalog: memory location, reference to the {@link com.sun.max.vm.actor.member.MethodActor}, and
 * instance of {@link com.sun.max.tele.object.TeleTargetMethod} (in the cases of code that is produced by VM
 * compilation). Specific details about the machine code in a compilation, for example disassembly and a variety of code
 * maps) are produced only on demand and are cached).
 */
package com.sun.max.tele.method;

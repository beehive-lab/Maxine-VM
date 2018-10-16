/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Access to the state of the VM's heap, which consists of:
 * <ul>
 * </li>specialized heaps, such as the <em>boot heap</em> that is part of the VM's binary image and the
 * <em>immortal heap</em>, and </li>
 * <li>the <em>dynamic heap</em>, which is implemented by the particular implementation of the VM's
 * {@link com.sun.max.vm.heap.HeapScheme} interface.
 * <p>
 * Depends partially on the VM class {@link com.sun.max.vm.tele.InspectableHeapInfo}.
 * <p>
 * Extracting detailed information about the dynamic heap requires specialized knowledge of the specific
 * {@link com.sun.max.vm.heap.HeapScheme} implementation included with a particular binary image build. This knowledge
 * is embedded in implementations of the interface {@link com.sun.max.tele.heap.RemoteHeapScheme}, which are selected
 * via a naming convention in which the local implementation class is named after the heap implementation class with the
 * string "Remote" prepended. Thus, the class {@link com.sun.max.tele.heap.RemoteSemiSpaceHeapScheme} contains the
 * specialized design knowledge needed to extract detailed heap information from a VM running with the
 * {@link com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme}.
 * <p>
 * Among the responsibilities of each remote heap scheme support class is to report the regions of memory allocated by
 * the OS that comprise the dynamic heap. Each is represented by an implementation of the interface
 * {@link com.sun.max.tele.heap.VmHeapRegion}, through which the memory location and extent is reported, and which can
 * provide an instance of the interface {@link com.sun.max.tele.object.RemoteObjectReferenceManager} for managing
 * {@linkplain com.sun.max.tele.reference.RemoteReference remote references} to objects in the region.
 */
package com.sun.max.tele.heap;

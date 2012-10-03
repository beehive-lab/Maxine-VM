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
 * Special reference schemes used for remote access to a VM, only implemented at present for the case of VM builds using
 * the {@link com.sun.max.vm.reference.direct.DirectReferenceScheme}.
 * <p>
 * Considerable code reuse, notably from the {@link com.sun.max.vm.reference.Reference} and
 * {@link com.sun.max.vm.layout.Layout} classes, is obtained by instantiating in the inspection process the same
 * implementation of {@link com.sun.max.vm.layout.LayoutScheme} used in the VM build, along with a special
 * implementation of {@link com.sun.max.vm.reference.ReferenceScheme} designed to substitute
 * <em>remote object references</em> when reusing VM code. The special implementation
 * {@link com.sun.max.tele.reference.direct.RemoteReferenceScheme} plays this role for VM builds using the
 * {@link com.sun.max.vm.reference.direct.DirectReferenceScheme}, as noted above.
 * <p>
 * Remote references are intended to be canonical with respect to the identity of objects in the VM.
 * <p>
 * The singleton instance of {@link com.sun.max.tele.reference.VmReferenceManager}, provides for the centralized
 * creation and management of remote object references. The actual management of remote references is delegated to the
 * objects that model the state of each region of VM memory that can contain objects in Maxine object format, typically
 * both heap and code cache regions. Each such object must implement the interface
 * {@link com.sun.max.tele.object.VmObjectHoldingRegion}, which obliges the object to produce an implementation of
 * {@link com.sun.max.tele.object.RemoteObjectReferenceManager} for the creation and management (including
 * canonicalization) of remote references that point to objects in that region.
 * <p>
 * The abstract class {@link com.sun.max.tele.reference.RemoteReference} extends the VM reference class, providing
 * general support for indirection of VM code to use remote references. Concrete subclasses are created by the instance
 * of {@link com.sun.max.tele.object.RemoteObjectReferenceManager}, suitable for the particular kind of object
 * management, if any, that the VM uses for that region.
 */
package com.sun.max.tele.reference;

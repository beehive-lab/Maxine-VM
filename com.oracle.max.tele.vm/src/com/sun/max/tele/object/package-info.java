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
 * Remote access to objects in the VM heap.
 * <p>
 * Remote object instances in the VM are represented by subclasses of {@link com.sun.max.tele.object.TeleObject}.
 * <p>
 * Remote object instances are intended to be <em>canonical</em> with respect to object identity in the VM.  The
 * {@link com.sun.max.tele.object.TeleObjectFactory} maintains a map from {@link com.sun.max.tele.reference.RemoteReference}s
 * to instances of {@link com.sun.max.tele.object.TeleObject}, and ensure that there is never more than one remote object
 * per remote reference.
 * <p>
 * The three specific kinds of object in the Maxine VM are:
 * <ul>
 * <li><b>Tuples</b>: represented by instances of {@link com.sun.max.tele.object.TeleTupleObject} and its subclasses;</li>
 * <li><b>Arrays</b>: represented by instances of {@link com.sun.max.tele.object.TeleArrayObject} and;</li>
 * <li><b>Hybrids</b>: represented by instances of the two concrete subtypes of {@link com.sun.max.tele.object.TeleHybridObject}
 * ({@link com.sun.max.tele.object.TeleDynamicHub} and {@link com.sun.max.tele.object.TeleStaticHub}) which correspond to
 * the only two concrete hybrids that occur in the VM.</li>
 * </ul>
 * The several dozen concrete subtypes of {@link com.sun.max.tele.object.TeleTupleObject} are instantiated to represent precisely
 * those types of object in VM memory that hold metadata needed to interpret other aspects of the VM's runtime state. These
 * subtypes include methods designed to extract, model, and possibly cache information about those specific kinds of remote objects.
 * These concrete subtypes
 * are registered with the {@link com.sun.max.tele.object.TeleObjectFactory}, both statically in this package and dynamically by
 * other Inspection services as needed.  The object factory automatically instantiates the most specific concrete subclass of
 * {@link com.sun.max.tele.object.TeleTupleObject} that is registered corresponding to an instance discovered in the VM.
 * <p>
 * Classes that model VM memory regions that can hold data in Maxine object format (typically heap regions and code cache regions)
 * are obliged to implement the interface {@link com.sun.max.tele.object.VmObjectHoldingRegion}.
 */
package com.sun.max.tele.object;

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
 * Access to classes loaded in the VM, and thus to the types of objects that might be found in VM memory. Depends
 * partially on the VM class {@link com.sun.max.vm.tele.InspectableClassInfo}.
 * <p>
 * The singleton instance of {@link com.sun.max.tele.type.VmClassAccess} proactively builds and maintains a list of all
 * classes loaded in the VM, along with a number of specialized <em>maps</em>. VM classes are also loaded locally for
 * reflective access and sometimes actual reuse.
 * <p>
 * At inspection startup, the instance of {@link com.sun.max.vm.type.ClassRegistry} in the boot image, which describes
 * the classes preloaded in the boot image, in order to initialize the local list and maps. This operation is especially
 * delicate because of the VM's circularity; none of the type-based access to object fields (see
 * {@link com.sun.max.tele.field.VmFieldAccess}) can be used until class information is available.
 * <p>
 * Subsequent updates are implemented differently. Dynamically loaded classes in the VM are recorded via
 * {@link com.sun.max.vm.tele.InspectableClassInfo} in such a way that it can be quickly determined at refresh time.
 * <p>
 * <strong>Limitation:</strong> This implementation does not explicitly model class loaders and in particular does not
 * distinguish well among instances of a single class loaded by two class loaders.
 */
package com.sun.max.tele.type;

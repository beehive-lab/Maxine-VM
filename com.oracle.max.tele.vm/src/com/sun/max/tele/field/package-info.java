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
 * Partially type-safe access to object fields in Maxine VM objects whose source declarations have been annotated with
 * {@linkplain com.sun.max.annotate.INSPECTED @INSPECTED}.
 * <p>
 * The class {@link com.sun.max.tele.field.VmFieldAccess}, when run as an application, modifies itself by the addition
 * of a specific field accessor for each such field. All accessors are subclasses of
 * {@link com.sun.max.tele.field.TeleFieldAccess}, where each is specialized for the kind of tuple object (instance vs.
 * static) and the basic Maxine data {@linkplain com.sun.max.vm.type.Kind kinds}. Accessors for JDK class fields and for
 * Maxine fields injected into JDK classes are also available, manually defined in this class.
 */
package com.sun.max.tele.field;

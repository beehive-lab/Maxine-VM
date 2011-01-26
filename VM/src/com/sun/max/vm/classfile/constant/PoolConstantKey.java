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
package com.sun.max.vm.classfile.constant;

/**
 * A key that represents a constant pool constant in a map.
 * <p>
 * The recursive generic type definition in this type and in {@link PoolConstant} basically ensure that each "real" pool
 * constant type (i.e. a non-generic type that extends or implements {@link PoolConstant}) defines its own key type.
 * <p>
 * A useful paper describing these kind of recursive types can be found at http://www.ejournal.unam.mx/compuysistemas/vol07-02/CYS07205.pdf
 * 
 * @author Doug Simon
 */
public interface PoolConstantKey<PoolConstant_Type extends PoolConstant<PoolConstant_Type>> {
}

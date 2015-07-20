/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.jdwp.vm.proxy;

/**
 * An interface that allows accessing an array object in the VM.
 *
 */
public interface ArrayProvider extends ObjectProvider {

    /**
     * Allows access to the actual length of the array as stored in the VM.
     *
     * @return the length of the array
     */
    int length();

    /**
     * Allows access to an element of the array. The return value is given as wrapped in a VMValue object.
     *
     * @param i index of the accessed array element
     * @return the value of the array at the specified index
     */
    VMValue getValue(int i);

    /**
     * Sets the element of the array.
     *
     * @param i index of the element to set
     * @param value the value to set at the specified index
     */
    void setValue(int i, VMValue value);

    /**
     * The type of an array is always of type ArrayTypeProvider.
     *
     * @return the type of the array
     */
    ArrayTypeProvider getArrayType();
}

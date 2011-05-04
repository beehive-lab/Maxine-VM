/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.*;
/**
 * Variable length array. Accessing elements of such an array with a positive index
 * will never throw an {@link IndexOutOfBoundsException}. Specifically,
 * {@linkplain #set(int, Object) inserting} an element will expand the array if necessary.
 *
 * @see ClassID
 * @see ClassDependencyManager
 */
class VariableLengthArray<E> {
    /*
     * Simple first implementation. Backing Storage for the array is made of a
     * fixed size initial prefix and a variable tail that is resized automatically
     * when trying to add an out of bound element.
     */

    private final E[] prefix;
    private E[] variable;

    public VariableLengthArray(int initialCapacity) {
        final Class<E []> type = null;
        prefix =  Utils.newArray(type, initialCapacity);
        variable = Utils.newArray(type, 0);
    }

    private void ensureCapacity(int minOverflowCapacity) {
        // FIXME: need to make sure that capacity doesn't go beyond max int.
        int newCapacity = (variable.length * 3) / 2 + 1;
        if (newCapacity < minOverflowCapacity) {
            newCapacity = minOverflowCapacity;
        }
        E [] newOverflow = Arrays.copyOf(variable, newCapacity);
        variable = newOverflow;
    }

    /**
     * Sets the element at a given index, expanding the array if necessary first so that {@code this.length() > index}.
     */
    public E set(int index, E element) {
        final int pl = prefix.length;
        if (index < pl) {
            E oldValue = prefix[index];
            prefix[index] = element;
            return oldValue;
        }
        final int oindex = index - pl;

        if (oindex >= variable.length) {
            ensureCapacity(oindex + 1);
        }
        E oldValue = variable[oindex];
        variable[oindex] = element;
        return oldValue;
    }

    public E get(int index) {
        final int pl = prefix.length;
        if (index < pl) {
            return prefix[index];
        }
        final int oindex = index - pl;
        if (oindex < variable.length) {
            return variable[oindex];
        }
        return null;
    }

    public int length() {
        return prefix.length + variable.length;
    }
    // TODO:
    // add trimming methods and support sparse array.
}

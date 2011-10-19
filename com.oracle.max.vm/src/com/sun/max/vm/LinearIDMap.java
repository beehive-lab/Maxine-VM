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
package com.sun.max.vm;

import java.util.*;

import com.sun.max.*;
/**
 * Map from positive identifiers to values.
 */
public class LinearIDMap<T> {
    /*
     * Simple first implementation. Backing Storage for the array is made of a
     * fixed size initial prefix and a variable tail that is resized automatically
     * when trying to add an out of bound element.
     */

    private final T[] prefix;
    private T[] variable;

    private int maxID = -1;

    public LinearIDMap(int initialCapacity) {
        final Class<T[]> type = null;
        prefix =  Utils.newArray(type, initialCapacity);
        variable = Utils.newArray(type, 0);
    }

    private void ensureCapacity(int minOverflowCapacity) {
        // FIXME (ld) need to make sure that capacity doesn't go beyond max int.
        int newCapacity = (variable.length * 3) / 2 + 1;
        if (newCapacity < minOverflowCapacity) {
            newCapacity = minOverflowCapacity;
        }
        T [] newOverflow = Arrays.copyOf(variable, newCapacity);
        variable = newOverflow;
    }

    /**
     * Sets the value for a given identifier.
     */
    public T set(int id, T element) {
        final int pl = prefix.length;
        if (id > maxID) {
            maxID = id;
        }
        if (id < pl) {
            T oldValue = prefix[id];
            prefix[id] = element;
            return oldValue;
        }
        final int oindex = id - pl;

        if (oindex >= variable.length) {
            ensureCapacity(oindex + 1);
        }
        T oldValue = variable[oindex];
        variable[oindex] = element;
        return oldValue;
    }

    /**
     * Gets the value for a given identifier.
     *
     * @return {@code null} if there is no value associated with {@code id}
     */
    public T get(int id) {
        final int pl = prefix.length;
        if (id < pl) {
            return prefix[id];
        }
        final int oindex = id - pl;
        if (oindex < variable.length) {
            return variable[oindex];
        }
        return null;
    }

    /**
     * Gets the highest identifier seen by {@link #set(int, Object)}.
     *
     * @return {@code -1} if {@link #set(int, Object)} has never been called for this map
     */
    public int maxID() {
        return maxID;
    }

    public int capacity() {
        return prefix.length + variable.length;
    }

    // TODO:
    // add trimming methods and support sparse array.
}

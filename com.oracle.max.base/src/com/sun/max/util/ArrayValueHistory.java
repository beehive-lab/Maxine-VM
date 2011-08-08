/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.util;

import java.util.*;

import com.sun.max.program.*;

/**
 * An array-based recording of the history of a value, with
 * time expressed as the number of generations back from the current generation (0).
 */
public class ArrayValueHistory<E> {

    private final ArrayDeque<E> generations;
    private final int limit;
    private int age = -1;

    public ArrayValueHistory(int limit) {
        this.generations = new ArrayDeque<E>();
        this.limit = limit;
    }

    public ArrayValueHistory() {
        this (Integer.MAX_VALUE);
    }

    /**
     * Adds a new value, which becomes the current generation.
     * The generation of all previously recorded values increases by 1.
     */
    public void add(E newValue) {
        if (generations.size() > 0) {
            if (newValue.equals(generations.getFirst())) {
                if (age >= 0) {
                    age++;
                }
            } else {
                age = 0;
            }
        }
        generations.addFirst(newValue);
        if (generations.size() > limit) {
            generations.removeLast();
        }
    }

    /**
     * @return the "current" value (at generation 0).
     * Error if no values have been recorded.
     */
    public E get() {
        if (generations.size() > 0) {
            return generations.getFirst();
        }
        throw ProgramError.unexpected("empty history");
    }

    /**
     * @return The value at a specified generation.
     * Error if generation does not exist.
     */
    public E get(int generation) {
        final Iterator<E> iterator = generations.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            if (index == generation) {
                return iterator.next();
            }
            index++;
        }
        throw ProgramError.unexpected("exceeded history");
    }

    /**
     * @return the age, in generations, of the current value, since recording began.
     * 0 if different from immediate predecessor; -1 if no different value ever recorded
     * Comparison uses {@linkplain Object#equals(Object) equals}.
     */
    public int getAge() {
        return age;
    }

    /**
     * @return the maximum number of generations that can be recorded.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return the number of generations recorded; initially 0.
     */
    public int getSize() {
        return generations.size();
    }

    /**
     * @return iteration of the values recorded in the history, starting with the current
     * generation and proceeding backward in time.
     */
    public Iterator<E> values() {
        return generations.iterator();
    }

}

/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.collect;

import java.util.*;

/**
 * An identity hash set backed by java.util.IdentityHashMap.
 *
 * @author Hiroshi Yamauchi
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class IdentityHashSet<T> extends AbstractSet<T> implements Cloneable {

    private final Map<T, T> internalMap;

    public IdentityHashSet() {
        internalMap = new IdentityHashMap<T, T>();
    }

    /**
     * Adds a specified element to this set.
     *
     * @param element the element to add
     * @return true if {@code element} was already in this set
     */
    @Override
    public boolean add(T element) {
        return internalMap.put(element, element) != null;
    }

    /**
     * Adds all the elements in a given Iterable to this set. The addition is always done by calling
     * {@link #add(Object)}.
     *
     * @param iterable the collection of elements to add
     */
    public final void addAll(Iterable<? extends T> iterable) {
        for (T element : iterable) {
            add(element);
        }
    }

    /**
     * Adds all the elements in a given array to this set. The addition is always done by calling
     * {@link #add(Object)}.
     *
     * @param elements the collection of elements to add
     */
    public final void addAll(T[] elements) {
        for (T element : elements) {
            add(element);
        }
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public void clear() {
        internalMap.clear();
    }

    /**
     * Remove an element from the set.
     *
     * @param element the element to remove.
     */
    @Override
    public boolean remove(Object element) {
        return internalMap.remove(element) != null;
    }

    /**
     * Removes all the elements in a given Iterable from this set. The removal is always done by calling
     * {@link #remove(Object)}.
     *
     * @param iterable the collection of elements to remove
     */
    public final void removeAll(Iterable<? extends T> iterable) {
        for (T element : iterable) {
            remove(element);
        }
    }

    /**
     * Removes all the elements in a given array from this set. The removal is always done by calling
     * {@link #remove(Object)}.
     *
     * @param elements the collection of elements to remove
     */
    public final void removeAll(T[] elements) {
        for (T element : elements) {
            remove(element);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return internalMap.keySet().iterator();
    }

    @Override
    public IdentityHashSet<T> clone() {
        final IdentityHashSet<T> copy = new IdentityHashSet<T>();
        copy.addAll(this);
        return copy;
    }

    @Override
    public boolean contains(Object element) {
        return internalMap.containsKey(element);
    }

    @Override
    public int size() {
        return internalMap.size();
    }

    public final IdentityHashSet<T> union(IdentityHashSet<T> other) {
        for (T element : other) {
            add(element);
        }
        return this;
    }

    public boolean isSuperSetOf(IdentityHashSet<T> other) {
        if (size() < other.size()) {
            return false;
        }
        for (T element : other) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStrictSuperSetOf(IdentityHashSet<T> other) {
        if (size() <= other.size()) {
            return false;
        }
        for (T element : other) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isSubSetOf(IdentityHashSet<T> other) {
        if (size() > other.size()) {
            return false;
        }
        for (T element : this) {
            if (!other.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStrictSubSetOf(IdentityHashSet<T> other) {
        if (size() >= other.size()) {
            return false;
        }
        for (T element : this) {
            if (!other.contains(element)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return internalMap.keySet().toArray(a);
    }

    @Override
    public String toString() {
        String string = "[ ";
        for (T element : internalMap.keySet()) {
            string += element.toString() + " ";
        }
        string += "]";
        return string;
    }
}

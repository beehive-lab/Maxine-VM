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

import com.sun.max.*;
import com.sun.max.collect.*;

/**
 * This is an {@link IdentityHashSet} with a predictable iteration order based on the order in which elements are
 * {@linkplain #add(Object) inserted} (<i>a la</i>{@link LinkedHashMap}). In particular, insertion order is not
 * affected if a key is <i>re-inserted</i> into the map.
 * <p>
 * Note that {@linkplain #add(Object) insertion} and {@linkplain #iterator() iteration} should have the same performance
 * as for a standard {@code IdentityHashSet}. However, {@linkplain #remove(Object) deletion} is a linear operation due
 * to the linked list use to record insertion order.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public class LinkedIdentityHashSet<T> extends IdentityHashSet<T> implements IterableWithLength<T> {

    public LinkedIdentityHashSet() {
    }

    private final LinkedList<T> order = new LinkedList<T>();

    @Override
    public boolean add(T element) {
        if (!super.add(element)) {
            order.add(element);
            return false;
        }
        return true;
    }

    /**
     * Gets an iterator over the elements in the order they were (originally) inserted.
     */
    @Override
    public Iterator<T> iterator() {
        assert order.size() == size() : order.size() + " != " + size();
        return order.iterator();
    }

    @Override
    public boolean remove(Object element) {
        order.remove(element);
        return super.remove(element);
    }

    @Override
    public void clear() {
        order.clear();
        super.clear();
    }

    public T getOne() {
        return order.getFirst();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LinkedIdentityHashSet) {
            final LinkedIdentityHashSet set = (LinkedIdentityHashSet) other;
            if (order.size() != set.order.size()) {
                return false;
            }
            final Iterator iterator = set.order.iterator();
            for (T element : order) {
                if (element != iterator.next()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return order.hashCode();
    }

    @Override
    public <E> E[] toArray(E[] a) {
        return order.toArray(a);
    }

    @Override
    public String toString() {
        String string = "[ ";
        for (T element : order) {
            string += element.toString() + " ";
        }
        string += "]";
        return string;
    }

    @Override
    public LinkedIdentityHashSet<T> clone() {
        return Utils.cast(super.clone());
    }

    public Collection<T> toCollection() {
        return order;
    }
}

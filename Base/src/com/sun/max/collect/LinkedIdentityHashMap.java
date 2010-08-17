/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.collect;

import java.util.*;

import com.sun.max.*;

/**
 * @author Bernd Mathiske
 */
public class LinkedIdentityHashMap<K, V> extends IdentityHashMap<K, V> implements Iterable<K> {

    private final LinkedList<K> order = new LinkedList<K>();

    public LinkedIdentityHashMap() {
    }

    public LinkedIdentityHashMap(int expectedMaxSize) {
        super(expectedMaxSize);
    }

    @Override
    public V put(K key, V value) {
        final V oldValue = super.put(key, value);
        if (oldValue == null) {
            if (value != null) {
                order.add(key);
            }
        } else {
            if (value == null) {
                order.remove(key);
            }
        }
        return oldValue;
    }

    public Iterator<K> iterator() {
        return order.iterator();
    }

    public K first() {
        return order.getFirst();
    }

    public K last() {
        return order.getLast();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LinkedIdentityHashMap) {
            final LinkedIdentityHashMap map = (LinkedIdentityHashMap) other;
            if (order.size() != map.order.size()) {
                return false;
            }
            final Iterator iterator = map.order.iterator();
            for (K key : order) {
                if (key != iterator.next() || !get(key).equals(map.get(key))) {
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
    public LinkedIdentityHashMap<K, V> clone() {
        return Utils.cast(super.clone());
    }

    public Collection<K> toCollection() {
        return keySet();
    }
}

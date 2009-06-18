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

import com.sun.max.lang.*;

/**
 * @author Bernd Mathiske
 */
public class LinkedIdentityHashMap<Key_Type, Value_Type> extends IdentityHashMap<Key_Type, Value_Type> implements DeterministicMap<Key_Type, Value_Type> {

    private final LinkedList<Key_Type> order = new LinkedList<Key_Type>();

    public LinkedIdentityHashMap() {
    }

    public LinkedIdentityHashMap(int expectedMaxSize) {
        super(expectedMaxSize);
    }

    @Override
    public Value_Type put(Key_Type key, Value_Type value) {
        final Value_Type oldValue = super.put(key, value);
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

    public Iterator<Key_Type> iterator() {
        return order.iterator();
    }

    public int length() {
        return order.size();
    }

    public Key_Type first() {
        return order.getFirst();
    }

    public Key_Type last() {
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
            for (Key_Type key : order) {
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
    public LinkedIdentityHashMap<Key_Type, Value_Type> clone() {
        return StaticLoophole.cast(super.clone());
    }

    public Collection<Key_Type> toCollection() {
        return keySet();
    }
}

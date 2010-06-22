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

/**
 * @author Bernd Mathiske
 */
public class IdentityHashMapping<K, V> implements Mapping<K, V> {

    private final Map<K, V> delegate;

    public IdentityHashMapping() {
        delegate = new IdentityHashMap<K, V>();
    }

    public IdentityHashMapping(int expectedMaxSize) {
        delegate = new IdentityHashMap<K, V>(expectedMaxSize);
    }

    public synchronized V put(K key, V value) {
        return delegate.put(key, value);
    }

    public synchronized V get(K key) {
        final V value = delegate.get(key);
        return value;
    }

    public synchronized boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    public int length() {
        return delegate.size();
    }

    public void clear() {
        delegate.clear();
    }

    public V remove(K key) {
        return delegate.remove(key);
    }

    public IterableWithLength<K> keys() {
        return new IterableWithLength<K>() {

            public int size() {
                return delegate.size();
            }

            public Iterator<K> iterator() {
                return delegate.keySet().iterator();
            }
        };
    }

    public IterableWithLength<V> values() {
        return new IterableWithLength<V>() {

            public int size() {
                return delegate.size();
            }

            public Iterator<V> iterator() {
                return delegate.values().iterator();
            }
        };
    }

}

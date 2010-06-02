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

/**
 * A chained hash table where the value type also doubles as an {@linkplain ChainedHashMapping.Entry entry} in a chain.
 * Such tables are more memory efficient than a chained hash table which uses separate objects for keys, values and
 * buckets.
 *
 * @author Doug Simon
 */
public class ChainingValueChainedHashMapping<K, V extends ChainedHashMapping.Entry<K, V>> extends ChainedHashMapping<K, V> {

    /**
     * Creates a chained hash table with {@linkplain HashEquality equality} key semantics and whose values double as the
     * {@linkplain ChainedHashMapping.Entry entry} in a chain.
     *
     * @param initialCapacity
     *            the initial capacity of the table
     */
    public ChainingValueChainedHashMapping(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a chained hash table with {@linkplain HashEquality equality} key semantics and an initial capacity of
     * {@value ChainedHashMapping#DEFAULT_INITIAL_CAPACITY} whose values double as the
     * {@linkplain ChainedHashMapping.Entry entry} in a chain.
     */
    public ChainingValueChainedHashMapping() {
    }

    @Override
    protected Entry<K, V> createEntry(int hashForKey, K key, V value, Entry<K, V> next) {
        value.setNext(next);
        return value;
    }
}

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
/*VCSID=f49f6132-3060-4377-afac-c82fc0597aa7*/
package com.sun.max.collect;

/**
 * A chained hash table whose {@linkplain HashEntry entries} record the hash of the key. This can provide better
 * performance improvement when the cost of computing the key's hash code is high.
 * 
 * @author Doug Simon
 */
public class HashEntryChainedHashMapping<Key_Type, Value_Type> extends ChainedHashMapping<Key_Type, Value_Type> {

    public static class HashEntry<Key_Type, Value_Type> extends DefaultEntry<Key_Type, Value_Type> {

        final int _hashOfKey;

        public HashEntry(int hashOfKey, Key_Type key, Value_Type value, Entry<Key_Type, Value_Type> next) {
            super(key, value, next);
            _hashOfKey = hashOfKey;
        }
    }

    /**
     * Creates a chained hash table whose entries record the hash of the key.
     * 
     * @param equivalence
     *            the semantics of key comparison and hashing. If {@code null}, then {@link HashEquality} is used.
     * @param initialCapacity
     *            the initial capacity of the table
     */
    public HashEntryChainedHashMapping(HashEquivalence<Key_Type> equivalence, int initialCapacity) {
        super(equivalence, initialCapacity);
    }

    /**
     * Creates a chained hash table with {@linkplain HashEquality equality} key semantics whose entries record the hash
     * of the key.
     * 
     * @param initialCapacity
     *            the initial capacity of the table
     */
    public HashEntryChainedHashMapping(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a chained hash table with an initial capacity of {@value ChainedHashMapping#DEFAULT_INITIAL_CAPACITY}
     * whose entries record the hash of the key.
     * 
     * @param equivalence
     *            the semantics of key comparison and hashing. If {@code null}, then {@link HashEquality} is used.
     */
    public HashEntryChainedHashMapping(HashEquivalence<Key_Type> equivalence) {
        super(equivalence);
    }

    /**
     * Creates a chained hash table with {@linkplain HashEquality equality} key semantics and an initial capacity of
     * {@value ChainedHashMapping#DEFAULT_INITIAL_CAPACITY} whose entries record the hash of the key.
     */
    public HashEntryChainedHashMapping() {
        super();
    }

    @Override
    protected Entry<Key_Type, Value_Type> createEntry(int hashOfKey, Key_Type key, Value_Type value, Entry<Key_Type, Value_Type> next) {
        return new HashEntryChainedHashMapping.HashEntry<Key_Type, Value_Type>(hashOfKey, key, value, next);
    }

    @Override
    protected boolean matches(Entry<Key_Type, Value_Type> entry, Key_Type key, int hashForKey) {
        final Key_Type entryKey = entry.key();
        return entryKey == key || (hashForKey == ((HashEntryChainedHashMapping.HashEntry) entry)._hashOfKey && key.equals(entryKey));
    }
}

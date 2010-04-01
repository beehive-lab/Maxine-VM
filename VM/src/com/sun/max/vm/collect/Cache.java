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
package com.sun.max.vm.collect;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;

/**
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class Cache<Key_Type, Value_Type> implements Mapping<Key_Type, Value_Type> {

    private final class Entry {
        Value_Type value;
        Size size;
        Size costInMilliSeconds;
        int numberOfUses;

        Entry(Value_Type value, Size size, Size costInMilliSeconds) {
            this.value = value;
            this.size = size;
            this.costInMilliSeconds = costInMilliSeconds;
            this.numberOfUses = 1;
        }
    }

    private final VariableMapping<Key_Type, Entry> mapping;

    protected Cache(HashEquivalence<Key_Type> equivalence) {
        mapping = HashMapping.createVariableMapping(equivalence);
    }

    public static <Key_Type, Value_Type> Cache<Key_Type, Value_Type> createIdentityCache() {
        final Class<HashIdentity<Key_Type>> type = null;
        final HashIdentity<Key_Type> equivalence = HashIdentity.instance(type);
        return new Cache<Key_Type, Value_Type>(equivalence);
    }

    public static <Key_Type, Value_Type> Cache<Key_Type, Value_Type> createEqualityCache() {
        final Class<HashEquality<Key_Type>> type = null;
        final HashEquality<Key_Type> equivalence = HashEquality.instance(type);
        return new Cache<Key_Type, Value_Type>(equivalence);
    }

    public int length() {
        return mapping.length();
    }

    public synchronized boolean containsKey(Key_Type key) {
        return mapping.containsKey(key);
    }

    public synchronized Value_Type get(Key_Type key) {
        final Entry entry = mapping.get(key);
        if (entry == null) {
            return null;
        }
        entry.numberOfUses++;
        return entry.value;
    }

    /**
     * Associates a value with a key and records how much space and time the value costs.
     * Based on these user-provided criteria, the cache may evict some of its key/value bindings
     * from time to time to meet an overall space budget that it infers automatically
     * from the frequency of its usage and its occupancy.
     *
     * @param key the key to the value to be stored
     * @param value the value to be associated with the key
     * @param size tells the cache how much space is occupied by the value
     * @param costInMilliSeconds tells the cache how long it took to create the value
     * @return the previous value or null if none existed
     */
    public synchronized Value_Type put(Key_Type key, Value_Type value, Size size, Size costInMilliSeconds) {
        final Entry entry = mapping.put(key, new Entry(value, size, costInMilliSeconds));
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    public synchronized void remove(Key_Type key) {
        mapping.remove(key);
    }

    public synchronized void clear() {
        mapping.clear();
    }

    public IterableWithLength<Key_Type> keys() {
        return mapping.keys();
    }

    public IterableWithLength<Value_Type> values() {
        return new IterableWithLength<Value_Type>() {
            public int length() {
                return Cache.this.length();
            }

            public Iterator<Value_Type> iterator() {
                return new Iterator<Value_Type>() {
                    final Iterator<Entry> entryIterator = mapping.values().iterator();

                    public boolean hasNext() {
                        return entryIterator.hasNext();
                    }

                    public Value_Type next() {
                        return entryIterator.next().value;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }
        };
    }
}

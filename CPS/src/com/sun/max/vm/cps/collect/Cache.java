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

    private final Mapping<Key_Type, Entry> mapping;

    protected Cache(HashEquivalence<Key_Type> equivalence) {
        mapping = HashMapping.createVariableMapping(equivalence);
    }

    public static <Key_Type, Value_Type> Cache<Key_Type, Value_Type> createIdentityCache() {
        final Class<HashIdentity<Key_Type>> type = null;
        final HashIdentity<Key_Type> equivalence = HashIdentity.instance(type);
        return new Cache<Key_Type, Value_Type>(equivalence);
    }

    public Value_Type put(Key_Type key, Value_Type value) {
        throw new UnsupportedOperationException();
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

    public synchronized Value_Type remove(Key_Type key) {
        Entry entry = mapping.remove(key);
        if (entry != null) {
            return entry.value;
        }
        return null;
    }

    public synchronized void clear() {
        mapping.clear();
    }

    public IterableWithLength<Key_Type> keys() {
        return mapping.keys();
    }

    public IterableWithLength<Value_Type> values() {
        return new IterableWithLength<Value_Type>() {
            public int size() {
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

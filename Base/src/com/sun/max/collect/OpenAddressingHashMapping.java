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
 * An open addressing hash table with liner probe hash collision resolution.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class OpenAddressingHashMapping<Key_Type, Value_Type> extends HashMapping<Key_Type, Value_Type> implements VariableMapping<Key_Type, Value_Type> {

    // Note: this implementation is partly derived from java.util.IdentityHashMap in the standard JDK

    /**
     * The initial capacity used by the no-args constructor.
     * MUST be a power of two.  The value 32 corresponds to the
     * (specified) expected maximum size of 21, given a load factor
     * of 2/3.
     */
    private static final int DEFAULT_CAPACITY = 32;

    /**
     * The minimum capacity, used if a lower value is implicitly specified
     * by either of the constructors with arguments.  The value 4 corresponds
     * to an expected maximum size of 2, given a load factor of 2/3.
     * MUST be a power of two.
     */
    private static final int MINIMUM_CAPACITY = 4;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<29.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 29;

    /**
     * The table, resized as necessary. Length MUST always be a power of two.
     */
    private Object[] _table;

    /**
     * Returns the appropriate capacity for the specified expected maximum
     * size.  Returns the smallest power of two between MINIMUM_CAPACITY
     * and MAXIMUM_CAPACITY, inclusive, that is greater than
     * (3 * expectedMaxSize)/2, if such a number exists.  Otherwise
     * returns MAXIMUM_CAPACITY.  If (3 * expectedMaxSize)/2 is negative, it
     * is assumed that overflow has occurred, and MAXIMUM_CAPACITY is returned.
     */
    private static int capacity(int expectedMaxSize) {
        // Compute minimum capacity for expectedMaxSize given a load factor of 2/3
        final int minimumCapacity = (3 * expectedMaxSize) >> 1;

        // Compute the appropriate capacity
        int result;
        if (minimumCapacity > MAXIMUM_CAPACITY || minimumCapacity < 0) {
            result = MAXIMUM_CAPACITY;
        } else {
            result = MINIMUM_CAPACITY;
            while (result < minimumCapacity) {
                result <<= 1;
            }
        }
        return result;
    }

    /**
     * Constructs a new, empty open addressing hash table with {@linkplain HashEquality equality} key semantics and a
     * default expected maximum size of 21.
     */
    public OpenAddressingHashMapping() {
        this(null);
    }

    /**
     * Constructs a new, empty open addressing hash table with a default expected maximum size of 21.
     *
     * @param equivalence
     *            the semantics to be used for comparing keys. If {@code null} is provided, then {@link HashEquality} is
     *            used.
     */
    public OpenAddressingHashMapping(HashEquivalence<Key_Type> equivalence) {
        this(equivalence, (DEFAULT_CAPACITY * 2) / 3);
    }

    /**
     * Constructs a new, empty open addressing hash table with {@linkplain HashEquality equality} key semantics.
     *
     * @param expectedMaximumSize
     *            the expected maximum size of the map
     */
    public OpenAddressingHashMapping(int expectedMaximumSize) {
        this(null, expectedMaximumSize);
    }

    /**
     * Constructs a new, empty open addressing hash table with the specified expected maximum size. Putting more than
     * the expected number of key-value mappings into the map may cause the internal data structure to grow, which may
     * be somewhat time-consuming.
     *
     * @param equivalence
     *            the semantics to be used for comparing keys. If {@code null} is provided, then {@link HashEquality} is
     *            used.
     * @param expectedMaximumSize
     *            the expected maximum size of the map
     * @throws IllegalArgumentException
     *             if {@code expectedMaximumSize} is negative
     */
    public OpenAddressingHashMapping(HashEquivalence<Key_Type> equivalence, int expectedMaximumSize) {
        super(equivalence);
        if (expectedMaximumSize < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + expectedMaximumSize);
        }

        // Find a power of 2 >= initialCapacity
        final int capacity = capacity(expectedMaximumSize);
        _threshold = (capacity * 2) / 3;
        _table = new Object[capacity * 2];
    }

    private int _numberOfEntries;
    private int _threshold;

    public int length() {
        return _numberOfEntries;
    }

    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because BuckHashMapping uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits.
     */
    static int hash(int hash) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        final int h = hash ^ ((hash >>> 20) ^ (hash >>> 12));
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Returns index for hash code h.
     */
    static int indexFor(int h, int length) {
        // Multiply by -127, and left-shift to use least bit as part of hash
        final int index = ((h << 1) - (h << 8)) & (length - 1);
        assert (index & 1) == 0 : "index must be even";
        return index;
    }

    /**
     * Circularly traverses table of size {@code length}.
     */
    private static int nextKeyIndex(int i, int length) {
        return i + 2 < length ? i + 2 : 0;
    }

    public Value_Type get(Key_Type key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        final Object[] table = _table;
        final int length = table.length;
        final int hash = hash(hashCode(key));
        int index = indexFor(hash, table.length);
        while (true) {
            final Object item = table[index];
            final Class<Key_Type> keyType = null;
            final Key_Type entryKey = StaticLoophole.cast(keyType, item);
            if (equivalent(entryKey, key)) {
                final Class<Value_Type> valueType = null;
                return StaticLoophole.cast(valueType, table[index + 1]);
            }
            if (item == null) {
                return null;
            }
            index = nextKeyIndex(index, length);
        }
    }

    public Value_Type put(Key_Type key, Value_Type value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        final Object[] table = _table;
        final int length = table.length;
        final int hash = hash(hashCode(key));
        int index = indexFor(hash, table.length);

        Object item = table[index];
        while (item != null) {
            final Class<Key_Type> keyType = null;
            final Key_Type entryKey = StaticLoophole.cast(keyType, item);
            if (equivalent(entryKey, key)) {
                final Class<Value_Type> valueType = null;
                final Value_Type oldValue = StaticLoophole.cast(valueType, table[index + 1]);
                table[index + 1] = value;
                return oldValue;
            }
            index = nextKeyIndex(index, length);
            item = table[index];
        }

        table[index] = key;
        table[index + 1] = value;
        if (_numberOfEntries++ >= _threshold) {
            resize(length); // length == 2 * current capacity.
        }
        return null;

    }

    public Value_Type remove(Key_Type key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        final Object[] table = _table;
        final int length = table.length;
        final int hash = hash(hashCode(key));
        int index = indexFor(hash, table.length);

        while (true) {
            final Object item = table[index];
            final Class<Key_Type> keyType = null;
            final Key_Type entryKey = StaticLoophole.cast(keyType, item);
            if (equivalent(entryKey, key)) {
                _numberOfEntries--;
                final Class<Value_Type> valueType = null;
                final Value_Type oldValue =  StaticLoophole.cast(valueType, table[index + 1]);
                table[index + 1] = null;
                table[index] = null;
                return oldValue;
            }
            if (item == null) {
                return null;
            }
            index = nextKeyIndex(index, length);
        }
    }

    public void clear() {
        for (int i = 0; i != _table.length; ++i) {
            _table[i] = null;
        }
        _numberOfEntries = 0;
    }

    /**
     * Resize the table to hold given capacity.
     *
     * @param newCapacity the new capacity, must be a power of two.
     */
    private void resize(int newCapacity) {
        assert Ints.isPowerOfTwoOrZero(newCapacity) : "newCapacity must be a power of 2";
        final int newLength = newCapacity * 2;

        final Object[] oldTable = _table;
        final int oldLength = oldTable.length;
        if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
            if (_threshold == MAXIMUM_CAPACITY - 1) {
                throw new IllegalStateException("Capacity exhausted.");
            }
            _threshold = MAXIMUM_CAPACITY - 1;  // Gigantic map!
            return;
        }
        if (oldLength >= newLength) {
            return;
        }

        final Object[] newTable = new Object[newLength];
        _threshold = newLength / 3;

        for (int i = 0; i < oldLength; i += 2) {
            final Class<Key_Type> keyType = null;
            final Key_Type key = StaticLoophole.cast(keyType, oldTable[i]);
            if (key != null) {
                final Object value = oldTable[i + 1];
                oldTable[i] = null;
                oldTable[i + 1] = null;
                final int hash = hash(hashCode(key));
                int index = indexFor(hash, newLength);
                while (newTable[index] != null) {
                    index = nextKeyIndex(index, newLength);
                }
                newTable[index] = key;
                newTable[index + 1] = value;
            }
        }
        _table = newTable;
    }

    private abstract class HashIterator<Type> implements Iterator<Type> {

        /**
         * Current slot.
         */
        int _index = _numberOfEntries != 0 ? 0 : _table.length;

        /**
         * To avoid unnecessary next computation.
         */
        boolean _indexIsValid;

        public boolean hasNext() {
            for (int i = _index; i < _table.length; i += 2) {
                final Object key = _table[i];
                if (key != null) {
                    _index = i;
                    _indexIsValid = true;
                    return true;
                }
            }
            _index = _table.length;
            return false;
        }

        protected int nextIndex() {
            if (!_indexIsValid && !hasNext()) {
                throw new NoSuchElementException();
            }

            _indexIsValid = false;
            final int lastReturnedIndex = _index;
            _index += 2;
            return lastReturnedIndex;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class KeyIterator extends HashIterator<Key_Type> {
        public Key_Type next() {
            final Class<Key_Type> keyType = null;
            return StaticLoophole.cast(keyType, _table[nextIndex()]);
        }
    }

    private class ValueIterator extends HashIterator<Value_Type> {
        public Value_Type next() {
            final Class<Value_Type> valueType = null;
            return StaticLoophole.cast(valueType, _table[nextIndex() + 1]);
        }
    }

    @Override
    public IterableWithLength<Key_Type> keys() {
        return new HashMappingIterable<Key_Type>() {
            public Iterator<Key_Type> iterator() {
                return new KeyIterator();
            }
        };
    }

    @Override
    public IterableWithLength<Value_Type> values() {
        return new HashMappingIterable<Value_Type>() {
            public Iterator<Value_Type> iterator() {
                return new ValueIterator();
            }
        };
    }
}

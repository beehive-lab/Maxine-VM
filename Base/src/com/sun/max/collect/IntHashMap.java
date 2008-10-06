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
/*VCSID=f6a3092a-f164-42d0-8ca8-eb34522944ab*/
package com.sun.max.collect;

/**
 * Similar to java.util.HashMap, but with plain primitive ints as keys.
 * Unsynchronized.
 *
 * @author Bernd Mathiske
 */
public class IntHashMap<Value_Type> {

    private static final int INITIAL_SIZE = 4;

    private int[] _keys;
    private MutableSequence<Value_Type> _values;
    private int _numberOfValues;
    private int _threshold;
    private static final float LOAD_FACTOR = 0.75f;

    public IntHashMap() {
    }

    /**
     * Constructs an empty {@code IntHashMap} with the specified initial capacity.
     *
     * @param  initialCapacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public IntHashMap(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }

        // Find a power of 2 >= initialCapacity
        int capacity = Integer.highestOneBit(Math.max(initialCapacity, 1));
        if (capacity < initialCapacity) {
            capacity <<= 1;
        }

        _keys = new int[capacity];
        _values = new ArraySequence<Value_Type>(capacity);
        setThreshold();
    }

    /**
     * Applies a hash function to a given integer key. This is critical
     * because ArrayHashMapping uses power-of-two length hash tables, that
     * otherwise encounter collisions for integer keys that do not differ
     * in lower bits.
     */
    static int hash(int key) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        final int hash = key ^ ((key >>> 20) ^ (key >>> 12));
        return hash ^ (hash >>> 7) ^ (hash >>> 4);
    }

    /**
     * Returns index for an integer key.
     */
    static int indexFor(int key, int length) {
        return key & (length - 1);
    }

    public Value_Type get(int key) {
        if (_keys == null) {
            return null;
        }
        int index = key % _keys.length;
        if (index < 0) {
            index *= -1;
        }
        final int start = index;
        do {
            final Value_Type value = _values.get(index);
            if (value == null) {
                return null;
            }
            if (_keys[index] == key) {
                return _values.get(index);
            }
            index++;
            index %= _keys.length;
        } while (index != start);
        return null;
    }

    private void setThreshold() {
        assert _keys.length == _values.length();
        _threshold = (int) (_keys.length * LOAD_FACTOR);
    }

    public void grow() {
        if (_keys == null) {
            _keys = new int[INITIAL_SIZE];
            _values = new ArraySequence<Value_Type>(INITIAL_SIZE);
            setThreshold();
        } else {
            final int[] keys = _keys;
            final IndexedSequence<Value_Type> values = _values;
            final int length = _keys.length * 2;
            _keys = new int[length];
            _values = new ArraySequence<Value_Type>(length);
            _numberOfValues = 0;
            setThreshold();
            for (int i = 0; i < keys.length; i++) {
                final Value_Type value = values.get(i);
                if (value != null) {
                    put(keys[i], value);
                }
            }
        }
    }

    public Value_Type put(int key, Value_Type value) {
        assert value != null;
        if (_numberOfValues >= _threshold) {
            grow();
        }
        int index = key % _keys.length;
        if (index < 0) {
            index *= -1;
        }
        final int start = index;
        while (_values.get(index) != null) {
            if (_keys[index] == key) {
                return _values.set(index, value);
            }
            index++;
            index %= _keys.length;
            assert index != start;
        }
        _keys[index] = key;
        _values.set(index, value);
        _numberOfValues++;
        return null;
    }

    public Sequence<Value_Type> toSequence() {
        if (_values == null) {
            final Class<Value_Type> type = null;
            return Sequence.Static.empty(type);
        }
        final MutableSequence<Value_Type> sequence = new ArraySequence<Value_Type>(_numberOfValues);
        int n = 0;
        for (int i = 0; i < _values.length(); i++) {
            final Value_Type value = _values.get(i);
            if (value != null) {
                sequence.set(n, value);
                n++;
            }
        }
        return sequence;
    }

    public int count() {
        return _numberOfValues;
    }
}

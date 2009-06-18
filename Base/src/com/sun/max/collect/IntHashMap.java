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
 * Similar to java.util.HashMap, but with plain primitive ints as keys.
 * Unsynchronized.
 *
 * @author Bernd Mathiske
 */
public class IntHashMap<Value_Type> {

    private static final int INITIAL_SIZE = 4;

    private int[] keys;
    private MutableSequence<Value_Type> values;
    private int numberOfValues;
    private int threshold;
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

        keys = new int[capacity];
        values = new ArraySequence<Value_Type>(capacity);
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
        if (keys == null) {
            return null;
        }
        int index = key % keys.length;
        if (index < 0) {
            index *= -1;
        }
        final int start = index;
        do {
            final Value_Type value = values.get(index);
            if (value == null) {
                return null;
            }
            if (keys[index] == key) {
                return values.get(index);
            }
            index++;
            index %= keys.length;
        } while (index != start);
        return null;
    }

    private void setThreshold() {
        assert keys.length == values.length();
        threshold = (int) (keys.length * LOAD_FACTOR);
    }

    public void grow() {
        if (keys == null) {
            keys = new int[INITIAL_SIZE];
            values = new ArraySequence<Value_Type>(INITIAL_SIZE);
            setThreshold();
        } else {
            final int[] ks = this.keys;
            final IndexedSequence<Value_Type> vs = this.values;
            final int length = ks.length * 2;
            this.keys = new int[length];
            this.values = new ArraySequence<Value_Type>(length);
            numberOfValues = 0;
            setThreshold();
            for (int i = 0; i < ks.length; i++) {
                final Value_Type value = vs.get(i);
                if (value != null) {
                    put(ks[i], value);
                }
            }
        }
    }

    public Value_Type put(int key, Value_Type value) {
        assert value != null;
        if (numberOfValues >= threshold) {
            grow();
        }
        int index = key % keys.length;
        if (index < 0) {
            index *= -1;
        }
        final int start = index;
        while (values.get(index) != null) {
            if (keys[index] == key) {
                return values.set(index, value);
            }
            index++;
            index %= keys.length;
            assert index != start;
        }
        keys[index] = key;
        values.set(index, value);
        numberOfValues++;
        return null;
    }

    public Sequence<Value_Type> toSequence() {
        if (values == null) {
            final Class<Value_Type> type = null;
            return Sequence.Static.empty(type);
        }
        final MutableSequence<Value_Type> sequence = new ArraySequence<Value_Type>(numberOfValues);
        int n = 0;
        for (int i = 0; i < values.length(); i++) {
            final Value_Type value = values.get(i);
            if (value != null) {
                sequence.set(n, value);
                n++;
            }
        }
        return sequence;
    }

    public int count() {
        return numberOfValues;
    }
}

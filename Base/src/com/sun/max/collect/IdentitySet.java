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
 * Similar to IdentityHashSet, but not recording 'null' as a set member
 * and not providing any element removal operations.
 *
 * (IdentityHashSet's iterator seems to return 'null' as a member even if it has never been entered.)
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class IdentitySet<Element_Type> implements Iterable<Element_Type> {

    /**
     * The default initial capacity - MUST be a power of two.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    public static final int MAXIMUM_CAPACITY = 1 << 30;

    // Note: this implementation is partly derived from java.util.HashMap in the standard JDK
    // In particular, it uses a table whose length is guaranteed to be a power of 2.

    /**
     * The number of key-value mappings contained in this map.
     */
    private int _numberOfElements;

    /**
     * Gets the number of key-value mappings contained in this map.
     */
    public int numberOfElements() {
        return _numberOfElements;
    }

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    private Element_Type[] _table;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    private int _threshold;

    private void setThreshold() {
        if (_table.length == 0) {
            _threshold = -1;
        } else {
            _threshold = (_table.length / 4) * 3;
        }
    }

    /**
     * Constructs a new, empty IdentitySet.
     */
    public IdentitySet() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructs a new, empty IdentitySet.
     * @param initialCapacity  the initial capacity of the Set
     * @throws  {@code IllegalArgumentException} if {@code initialCapacity} is less than zero}
     */
    public IdentitySet(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        // Find a power of 2 >= initialCapacity
        int capacity = Integer.highestOneBit(Math.max(Math.min(MAXIMUM_CAPACITY, initialCapacity), 1));
        if (capacity < initialCapacity) {
            capacity <<= 1;
        }

        final Class<Element_Type[]> type = null;
        _table = StaticLoophole.cast(type, new Object[initialCapacity]);
        setThreshold();
    }

    private void resize(int newTableLength) {
        final Element_Type[] oldTable = _table;
        final Class<Element_Type[]> type = null;
        _table = StaticLoophole.cast(type, new Object[newTableLength]);
        setThreshold();
        _numberOfElements = 0;
        for (int i = 0; i < oldTable.length; i++) {
            final Element_Type oldValue = oldTable[i];
            if (oldValue != null) {
                add(oldValue);
            }
        }
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
        return h & (length - 1);
    }

    /**
     * Adds a given element to this set.
     *
     * @param element the element to add
     * @throws IllegalArgumentException if {@code element == null}
     */
    public void add(Element_Type element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        if (_numberOfElements > _threshold) {
            resize(_table.length * 2);
        }
        final int start = indexFor(System.identityHashCode(element), _table.length);
        int i = start;
        do {
            final Element_Type entry = _table[i];
            if (entry == null) {
                _table[i] = element;
                _numberOfElements++;
                return;
            }
            if (entry == element) {
                return;
            }
            if (++i == _table.length) {
                i = 0;
            }
        } while (i != start);
    }

    /**
     * Determines if a given element is a member of this set.
     *
     * @param element the element to test for membership in this set
     * @return {@code true} if {@code element} is in this set, {@code false} otherwise
     * @throws IllegalArgumentException if {@code element == null}
     */
    public boolean contains(Element_Type element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        if (_numberOfElements == 0) {
            return false;
        }
        final int start = indexFor(System.identityHashCode(element), _table.length);
        int i = start;
        while (true) {
            final Element_Type entry = _table[i];
            if (entry == element) {
                return true;
            }
            if (entry == null) {
                return false;
            }
            if (++i == _table.length) {
                i = 0;
            }
            assert i != start;
        }
    }

    public Iterator<Element_Type> iterator() {
        final Class<Element_Type[]> type = null;
        final Element_Type[] array = StaticLoophole.cast(type, new Object[numberOfElements()]);
        int j = 0;
        for (int i = 0; i < _table.length; i++) {
            final Element_Type element = _table[i];
            if (element != null) {
                array[j++] = element;
            }
        }
        return com.sun.max.lang.Arrays.iterator(array);
    }
}

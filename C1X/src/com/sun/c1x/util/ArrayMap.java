/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.util;

/**
 * The <code>ArrayMap</code> class implements an efficient one-level map which is implemented
 * as an array. Note that because of the one-level array inside, this data structure performs best
 * when the range of integer keys is small and densely used. Note that the implementation can
 * handle arbitrary intervals, including negative numbers, up to intervals of size 2^31 - 1.
 *
 * @author Ben L. Titzer
 */
public class ArrayMap<T> {

    private static final int INITIAL_SIZE = 5; // how big the initial array should be
    private static final int EXTRA = 2; // how far on the left or right of a new element to grow

    Object[] map;
    int low;

    /**
     * Constructs a new <code>ArrayMap</code> with no initial assumptions.
     */
    public ArrayMap() {
    }

    /**
     * Constructs a new <code>ArrayMap</code> that initially covers the specified interval.
     * Note that this map will automatically expand if necessary later.
     * @param low the low index, inclusive
     * @param high the high index, exclusive
     */
    public ArrayMap(int low, int high) {
        this.low = low;
        this.map = new Object[high - low + 1];
    }

    /**
     * Puts a new value in the map at the specified index.
     * @param i the index at which to store the value
     * @param value the value to store at the specified index
     */
    public void put(int i, T value) {
        int index = i - low;
        if (map == null) {
            // no map yet
            map = new Object[INITIAL_SIZE];
            low = index - 2;
            map[INITIAL_SIZE / 2] = value;
        } else if (index < 0) {
            // grow backwards
            growBackward(i, value);
        } else if (index >= map.length) {
            // grow forwards
            growForward(i, value);
        } else {
            // no growth necessary
            map[index] = value;
        }
    }

    /**
     * Gets the value at the specified index in the map.
     * @param i the index
     * @return the value at the specified index; <code>null</code> if there is no value at the specified index,
     * or if the index is out of the currently stored range
     */
    public T get(int i) {
        int index = i - low;
        if (map == null || index < 0 || index >= map.length) {
            return null;
        }
        Class<T> type = null;
        return Util.uncheckedCast(type, map[index]);
    }

    public int length() {
        return map.length;
    }

    private void growBackward(int i, T value) {
        int nlow = i - EXTRA;
        Object[] nmap = new Object[low - nlow + map.length];
        System.arraycopy(map, 0, nmap, low - nlow, map.length);
        map = nmap;
        low = nlow;
        map[i - low] = value;
    }

    private void growForward(int i, T value) {
        int nlen = i - low + 1 + EXTRA;
        Object[] nmap = new Object[nlen];
        System.arraycopy(map, 0, nmap, 0, map.length);
        map = nmap;
        map[i - low] = value;
    }
}

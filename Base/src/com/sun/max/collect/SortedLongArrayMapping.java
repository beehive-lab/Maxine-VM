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

import com.sun.max.*;

/**
 * Two arrays: one holding sorted long keys,
 * the parallel other one holding lookup values corresponding to those keys.
 * Insertion/deletion moderately costly, lookup by binary search pretty fast.
 *
 * @author Bernd Mathiske
 */
public class SortedLongArrayMapping<V> {

    public SortedLongArrayMapping() {
    }

    private long[] keys;
    private Object[] values;

    private int findIndex(long key) {
        if (keys == null) {
            return 0;
        }
        int left = 0;
        int right = keys.length;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final long middleKey = keys[middle];
            if (middleKey == key) {
                return middle;
            } else if (middleKey > key) {
                right = middle;
            } else {
                left = middle + 1;
            }
        }
        return left;
    }

    /**
     * Binary search in the key array.
     * @return the value corresponding to the key or null if the key is not found
     */
    public V get(long key) {
        if (keys == null) {
            return null;
        }
        final int index = findIndex(key);
        if (index < keys.length && keys[index] == key) {
            final Class<V> type = null;
            return Utils.cast(type, values[index]);
        }
        return null;
    }

    public void put(long key, V value) {
        final int index = findIndex(key);
        if (keys != null && index < keys.length && keys[index] == key) {
            values[index] = value;
        } else {
            long[] newKeys = new long[keys.length + 1];
            System.arraycopy(keys, 0, newKeys, 0, index);
            System.arraycopy(keys, index, newKeys, index + 1, keys.length - index);
            newKeys[index] = key;
            keys = newKeys;

            Object[] newValues = new Object[values.length + 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index, newValues, index + 1, values.length - index);
            newValues[index] = key;
            values = newValues;
        }
    }

    public void remove(long key) {
        if (keys == null) {
            return;
        }
        final int index = findIndex(key);
        if (keys[index] == key) {
            int newLength = keys.length - 1;
            long[] newKeys = new long[newLength];
            System.arraycopy(keys, 0, newKeys, 0, index);
            System.arraycopy(keys, index + 1, newKeys, index, newLength - index);
            keys = newKeys;

            Object[] newValues = new Object[newLength];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index + 1, newValues, index, newLength - index);
            values = newValues;
        }
    }
}

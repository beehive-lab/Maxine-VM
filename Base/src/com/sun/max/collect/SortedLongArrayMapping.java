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

import com.sun.max.lang.*;

/**
 * Two arrays: one holding sorted long keys,
 * the parallel other one holding lookup values corresponding to those keys.
 * Insertion/deletion moderately costly, lookup by binary search pretty fast.
 *
 * @author Bernd Mathiske
 */
public class SortedLongArrayMapping<Value_Type> {

    public SortedLongArrayMapping() {
    }

    private long[] _keys;
    private Object[] _values;

    private int findIndex(long key) {
        if (_keys == null) {
            return 0;
        }
        int left = 0;
        int right = _keys.length;
        while (right > left) {
            final int middle = left + ((right - left) >> 1);
            final long middleKey = _keys[middle];
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
    public Value_Type get(long key) {
        if (_keys == null) {
            return null;
        }
        final int index = findIndex(key);
        if (index < _keys.length && _keys[index] == key) {
            final Class<Value_Type> type = null;
            return StaticLoophole.cast(type, _values[index]);
        }
        return null;
    }

    public void put(long key, Value_Type value) {
        final int index = findIndex(key);
        if (_keys != null && index < _keys.length && _keys[index] == key) {
            _values[index] = value;
        } else {
            _keys = Longs.insert(_keys, index, key);
            _values = Arrays.insert(Object.class, _values, index, value);
        }
    }

    public void remove(long key) {
        if (_keys == null) {
            return;
        }
        final int index = findIndex(key);
        if (_keys[index] == key) {
            _keys = Longs.remove(_keys, index);
            _values = Arrays.remove(Object.class, _values, index);
        }
    }
}

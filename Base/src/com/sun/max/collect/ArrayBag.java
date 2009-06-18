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
import com.sun.max.lang.Arrays;

/**
 * A bag that keeps the values corresponding to the same key in a array.
 * This is not very performant when adding or removing elements,
 * but it provides quick and easy multi-valued lookup
 * that even the Maxine Inspector interpreter can perform.
 *
 * @author Bernd Mathiske
 */
public class ArrayBag<Key_Type, Value_Type> {

    private final Class<Value_Type> valueType;

    private final Map<Key_Type, Value_Type[]> map;

    public enum MapType {
        SORTED,
        HASHED,
        IDENTITY;
    }

    private final Value_Type[] empty;

    public ArrayBag(Class<Value_Type> valueType, MapType mapType) {
        this.valueType = valueType;
        final Map<Key_Type, Value_Type[]> m;
        if (mapType == MapType.SORTED) {
            m = new TreeMap<Key_Type, Value_Type[]>();
        } else if (mapType == MapType.HASHED) {
            m = new HashMap<Key_Type, Value_Type[]>();
        } else {
            m = new IdentityHashMap<Key_Type, Value_Type[]>();
        }
        this.map = m;
        final Class<Value_Type[]> arrayType = null;
        empty = StaticLoophole.cast(arrayType, Arrays.newInstance(valueType, 0));
    }

    public Value_Type[] get(Key_Type key) {
        final Value_Type[] result = map.get(key);
        if (result == null) {
            return empty;
        }
        return map.get(key);
    }

    public boolean containsKey(Key_Type key) {
        return map.containsKey(key);
    }

    public void add(Key_Type key, Value_Type value) {
        final Value_Type[] oldValues = map.get(key);
        if (oldValues == null) {
            final Value_Type[] newValues = Arrays.newInstance(valueType, 1);
            newValues[0] = value;
            map.put(key, newValues);
        } else if (!Arrays.contains(oldValues, value)) {
            final Value_Type[] newValues = Arrays.newInstance(valueType, oldValues.length + 1);
            Arrays.copy(oldValues, newValues);
            newValues[oldValues.length] = value;
            map.put(key, newValues);
        }
    }

    public void remove(Key_Type key, Value_Type value) {
        final Value_Type[] oldValues = map.get(key);
        if (oldValues == null || !Arrays.contains(oldValues, value)) {
            return;
        }
        if (oldValues.length == 1) {
            map.remove(key);
        } else {
            final Value_Type[] newValues = Arrays.newInstance(valueType, oldValues.length - 1);
            int i = 0;
            for (Value_Type v : oldValues) {
                if (v != value) {
                    newValues[i++] = v;
                }
            }
            map.put(key, newValues);
        }
    }

    public Set<Key_Type> keys() {
        return map.keySet();
    }

}

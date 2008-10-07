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
/*VCSID=87367d3d-d3a1-4199-928d-f397b9959c3b*/
package com.sun.max.collect;

import java.util.*;

import com.sun.max.annotate.*;
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

    private final Class<Value_Type> _valueType;

    private final Map<Key_Type, Value_Type[]> _map;

    public enum MapType {
        SORTED,
        HASHED,
        IDENTITY;
    }

    private final Value_Type[] _empty;

    public ArrayBag(Class<Value_Type> valueType, MapType mapType) {
        _valueType = valueType;
        @JdtSyntax("JDT compiler bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=151153): invalid stackmap generated if this code is replaced by a ternary operator")
        final Map<Key_Type, Value_Type[]> map;
        if (mapType == MapType.SORTED) {
            map = new TreeMap<Key_Type, Value_Type[]>();
        } else if (mapType == MapType.HASHED) {
            map = new HashMap<Key_Type, Value_Type[]>();
        } else {
            map = new IdentityHashMap<Key_Type, Value_Type[]>();
        }
        _map = map;
        final Class<Value_Type[]> arrayType = null;
        _empty = StaticLoophole.cast(arrayType, Arrays.create(_valueType, 0));
    }

    public Value_Type[] get(Key_Type key) {
        final Value_Type[] result = _map.get(key);
        if (result == null) {
            return _empty;
        }
        return _map.get(key);
    }

    public boolean containsKey(Key_Type key) {
        return _map.containsKey(key);
    }

    public void add(Key_Type key, Value_Type value) {
        final Value_Type[] oldValues = _map.get(key);
        if (oldValues == null) {
            final Value_Type[] newValues = Arrays.create(_valueType, 1);
            newValues[0] = value;
            _map.put(key, newValues);
        } else if (!Arrays.contains(oldValues, value)) {
            final Value_Type[] newValues = Arrays.create(_valueType, oldValues.length + 1);
            Arrays.copy(oldValues, newValues);
            newValues[oldValues.length] = value;
            _map.put(key, newValues);
        }
    }

    public void remove(Key_Type key, Value_Type value) {
        final Value_Type[] oldValues = _map.get(key);
        if (oldValues == null || !Arrays.contains(oldValues, value)) {
            return;
        }
        if (oldValues.length == 1) {
            _map.remove(key);
        } else {
            final Value_Type[] newValues = Arrays.create(_valueType, oldValues.length - 1);
            int i = 0;
            for (Value_Type v : oldValues) {
                if (v != value) {
                    newValues[i++] = v;
                }
            }
            _map.put(key, newValues);
        }
    }

    public Set<Key_Type> keys() {
        return _map.keySet();
    }

}

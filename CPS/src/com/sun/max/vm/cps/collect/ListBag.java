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
package com.sun.max.vm.cps.collect;

import java.util.*;

/**
 * Implementation of a bag where the multi-values are stored and retrieved
 * in lists and the underlying map is a {@link TreeMap} (sorted), a {@link HashMap} or an
 * {@link IdentityHashMap}, depending on the argument to the {@link #ListBag(MapType) constructor}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ListBag<Key_Type, Value_Type> {

    private final Map<Key_Type, List<Value_Type>> map;

    public enum MapType {
        SORTED,
        HASHED,
        IDENTITY;
    }

    public ListBag(MapType mapType) {
        if (mapType == MapType.SORTED) {
            this.map = new TreeMap<Key_Type, List<Value_Type>>();
        } else if (mapType == MapType.HASHED) {
            this.map = new HashMap<Key_Type, List<Value_Type>>();
        } else {
            this.map = new IdentityHashMap<Key_Type, List<Value_Type>>();
        }
    }

    public List<Value_Type> get(Key_Type key) {
        final List<Value_Type> result = map.get(key);
        if (result == null) {
            return Collections.emptyList();
        }
        return map.get(key);
    }

    public boolean containsKey(Key_Type key) {
        return map.containsKey(key);
    }

    private List<Value_Type> makeList(Key_Type key) {
        List<Value_Type> list = map.get(key);
        if (list == null) {
            list = new ArrayList<Value_Type>();
            map.put(key, list);
        }
        return list;
    }

    public void add(Key_Type key, Value_Type value) {
        makeList(key).add(value);
    }

    public Set<Key_Type> keys() {
        return map.keySet();
    }

    public Collection<List<Value_Type>> collections() {
        return map.values();
    }
}

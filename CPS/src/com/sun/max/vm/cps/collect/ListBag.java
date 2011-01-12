/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

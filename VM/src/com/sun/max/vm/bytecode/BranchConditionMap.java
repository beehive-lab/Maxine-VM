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
package com.sun.max.vm.bytecode;

import java.util.*;

import com.sun.max.*;

/**
 * An efficient, meta-circular safe enum map for {@link BranchCondition} keys. Unlike {@link EnumMap}, this data
 * structure does not support differentiating between a key that is mapped to null from a key that has no mapping.
 *
 * @author Doug Simon
 */
public class BranchConditionMap<Value_Type> implements Cloneable {

    /**
     * Defined at build image time so not subject to the meta-circular issues involved in calling the
     * {@code values()} on {@link BranchCondition}.
     */
    private static final Object[] VALUES_PROTOTYPE = new Object[BranchCondition.VALUES.size()];

    /**
     * The values for the {@link BranchCondition} keys. The value corresponding to a given key is at index {@code i} in
     * this array where {@code i} is the ordinal of the key.
     */
    protected final Value_Type[] values;

    public BranchConditionMap() {
        values = Utils.cast(VALUES_PROTOTYPE.clone());
    }

    /**
     * Returns the value to which {@code branchCondition} is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
    public Value_Type get(BranchCondition branchCondition) {
        return values[branchCondition.ordinal()];
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously contained a mapping for
     * this key, the old value is replaced.
     *
     * @param branchCondition the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     *
     * @throws NullPointerException if {@code branchCondition} is null
     */
    public void put(BranchCondition branchCondition, Value_Type value) {
        values[branchCondition.ordinal()] = value;
    }
}

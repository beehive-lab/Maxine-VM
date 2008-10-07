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
package com.sun.max.vm.bytecode;

import com.sun.max.lang.*;

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
    private static final Object[] VALUES_PROTOTYPE = new Object[BranchCondition.VALUES.length()];

    /**
     * The values for the {@link BranchCondition} keys. The value corresponding to a given key is at index {@code i} in
     * this array where {@code i} is the ordinal of the key.
     */
    protected final Value_Type[] _values;

    public BranchConditionMap() {
        _values = StaticLoophole.cast(VALUES_PROTOTYPE.clone());
    }

    /**
     * Returns the value to which {@code branchCondition} is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
    public Value_Type get(BranchCondition branchCondition) {
        return _values[branchCondition.ordinal()];
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
        _values[branchCondition.ordinal()] = value;
    }
}

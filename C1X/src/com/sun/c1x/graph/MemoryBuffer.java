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
package com.sun.c1x.graph;

import com.sun.c1x.ir.*;
import com.sun.c1x.ci.CiField;
import com.sun.c1x.value.ValueType;

import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * The <code>MemoryBuffer</code> is an abstract representation of memory that is used redundant load and
 * store elimination. In C1, tracking of fields of new objects' fields was precise,
 * while tracking of other fields is managed at the offset granularity (i.e. a write of a field with offset
 * <code>off</code> will "overwrite" all fields with the offset <code>off</code>. However, C1X distinguishes all
 * loaded fields as separate locations. Static fields have just one location, while instance fields are
 * tracked for at most one instance object. Loads or stores of unloaded fields kill all memory locations.
 * An object is no longer "new" if it is stored into a field or array.
 *
 * @author Ben L. Titzer
 */
public class MemoryBuffer {

    private final HashMap<CiField, Instruction> _objectMap = new HashMap<CiField, Instruction>();
    private final HashMap<CiField, Instruction> _valueMap = new HashMap<CiField, Instruction>();
    private final IdentityHashMap<Instruction, Instruction> _newObjects = new IdentityHashMap<Instruction, Instruction>();

    /**
     * Kills all memory locations.
     */
    public void kill() {
        _objectMap.clear();
        _valueMap.clear();
        _newObjects.clear();
    }

    /**
     * The specified instruction has just escaped, it can no longer be considered a "new object".
     * @param x the instruction that just escaped
     */
    public void storeValue(Instruction x) {
        _newObjects.remove(x);
    }

    /**
     * Record a newly allocated object.
     * @param n the instruction generating the new object
     */
    public void newInstance(NewInstance n) {
        _newObjects.put(n, n);
    }

    /**
     * Look up a load for load elimination, and put this load into the load elimination map.
     * @param load the instruction representing the load
     * @return a reference to the previous instruction that already loaded the value, if it is available; the
     * <code>load</code> parameter otherwise
     */
    public Instruction load(LoadField load) {
        if (!load.isLoaded()) {
            // the field is not loaded, kill everything, because it will need to be resolved
            kill();
            return load;
        }
        CiField field = load.field();
        if (load.isStatic()) {
            // the field is static, look in the static map
            Instruction r = _valueMap.get(field);
            if (r != null) {
                return r;
            }
            _valueMap.put(field, load);
        } else {
            // see if the value for this object for this field is in the map
            if (_objectMap.get(field) == load.object()) {
                return _valueMap.get(field);
            }
            _objectMap.put(field, load.object());
            _valueMap.put(field, load);
        }

        return load; // load cannot be eliminated
    }

    /**
     * Look up a store for store elimination, and put this store into the load elimination map.
     * @param store the store instruction to put into the map
     * @return <code>null</code> if the store operation is redundant; the <code>store</code> parameter
     * otherwise
     */
    public StoreField store(StoreField store) {
        if (!store.isLoaded()) {
            // the field is not loaded, kill everything, because it will need to be resolved
            kill();
            return store;
        }
        CiField field = store.field();
        Instruction value = store.value();
        if (store.isStatic()) {
            // the field is static, overwrite it into the static map
            _valueMap.put(field, value);
        } else {
            if (_newObjects.containsKey(store.object())) {
                // this is a store to a new object's field
                ValueType vt = value.type();
                if (fieldHasNoStores(field) && vt.isConstant() && vt.asConstant().isDefaultValue()) {
                    // this is a redundant initialization of a new object's field that has not been assigned to
                    return null;
                }
            }
            Instruction obj = _objectMap.get(field);
            if (obj == store.object()) {
                // is this a redundant store?
                if (value == _valueMap.get(field) && !field.isVolatile()) {
                    return null;
                }
            }
            _objectMap.put(field, store.object());
            _valueMap.put(field, value);
        }
        storeValue(value); // the value stored just escaped
        return store; // the store cannot be eliminated
    }

    private boolean fieldHasNoStores(CiField field) {
        return _objectMap.get(field) == null;
    }
}

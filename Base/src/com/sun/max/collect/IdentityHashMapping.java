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
/*VCSID=0efe92cd-8c05-40b0-a799-cba3754c3e10*/
package com.sun.max.collect;

import java.util.*;

/**
 * @author Bernd Mathiske
 */
public class IdentityHashMapping<Key_Type, Value_Type> implements VariableMapping<Key_Type, Value_Type> {

    private final Map<Key_Type, Value_Type> _delegate;

    public IdentityHashMapping() {
        _delegate = new IdentityHashMap<Key_Type, Value_Type>();
    }

    public IdentityHashMapping(int expectedMaxSize) {
        _delegate = new IdentityHashMap<Key_Type, Value_Type>(expectedMaxSize);
    }

    public synchronized Value_Type put(Key_Type key, Value_Type value) {
        return _delegate.put(key, value);
    }

    public synchronized Value_Type get(Key_Type key) {
        final Value_Type value = _delegate.get(key);
        return value;
    }

    public synchronized boolean containsKey(Key_Type key) {
        return _delegate.containsKey(key);
    }

    public int length() {
        return _delegate.size();
    }

    public void clear() {
        _delegate.clear();
    }

    public Value_Type remove(Key_Type key) {
        return _delegate.remove(key);
    }

    public IterableWithLength<Key_Type> keys() {
        return new IterableWithLength<Key_Type>() {

            public int length() {
                return _delegate.size();
            }

            public Iterator<Key_Type> iterator() {
                return _delegate.keySet().iterator();
            }
        };
    }

    public IterableWithLength<Value_Type> values() {
        return new IterableWithLength<Value_Type>() {

            public int length() {
                return _delegate.size();
            }

            public Iterator<Value_Type> iterator() {
                return _delegate.values().iterator();
            }
        };
    }

}

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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;

/**
 * Implementation of the {@link Bag} interface where the multi-values are stored and retrieved
 * in sequences and the underlying map is a {@link TreeMap} (sorted), a {@link HashMap} or an
 * {@link IdentityHashMap}, depending on the argument to the {@link #SequenceBag(MapType) constructor}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class SequenceBag<Key_Type, Value_Type> implements Bag<Key_Type, Value_Type, Sequence<Value_Type>> {

    private final Map<Key_Type, VariableSequence<Value_Type>> _map;

    public enum MapType {
        SORTED,
        HASHED,
        IDENTITY;
    }

    public SequenceBag(MapType mapType) {
        @JdtSyntax("JDT compiler bug (https://bugs.eclipse.org/bugs/show_bug.cgi?id=151153): invalid stackmap generated if this code is replaced by a ternary operator")
        final Map<Key_Type, VariableSequence<Value_Type>> map;
        if (mapType == MapType.SORTED) {
            map = new TreeMap<Key_Type, VariableSequence<Value_Type>>();
        } else if (mapType == MapType.HASHED) {
            map = new HashMap<Key_Type, VariableSequence<Value_Type>>();
        } else {
            map = new IdentityHashMap<Key_Type, VariableSequence<Value_Type>>();
        }
        _map = map;
    }

    public Sequence<Value_Type> get(Key_Type key) {
        final Sequence<Value_Type> result = _map.get(key);
        if (result == null) {
            final Class<Value_Type> type = null;
            return Sequence.Static.empty(type);
        }
        return _map.get(key);
    }

    public boolean containsKey(Key_Type key) {
        return _map.containsKey(key);
    }

    private VariableSequence<Value_Type> makeSequence(Key_Type key) {
        VariableSequence<Value_Type> sequence = _map.get(key);
        if (sequence == null) {
            sequence = new ArrayListSequence<Value_Type>();
            _map.put(key, sequence);
        }
        return sequence;
    }

    public void add(Key_Type key, Value_Type value) {
        makeSequence(key).append(value);
    }

    public void addAll(Key_Type key, Sequence<Value_Type> values) {
        AppendableSequence.Static.appendAll(makeSequence(key), values);
    }

    public void remove(Key_Type key, Value_Type value) {
        final VariableSequence<Value_Type> sequence = _map.get(key);
        if (sequence != null) {
            ShrinkableSequence.Static.removeAllEqual(sequence, value);
            if (sequence.isEmpty()) {
                _map.remove(key);
            }
        }
    }

    public Set<Key_Type> keys() {
        return _map.keySet();
    }

    public Iterable<Sequence<Value_Type>> collections() {
        final Class<Collection<Sequence<Value_Type>>> type = null;
        return StaticLoophole.cast(type, _map.values());
    }

    public Iterator<Value_Type> iterator() {
        final Collection<VariableSequence<Value_Type>> sequences = _map.values();
        assert Iterable.class.isAssignableFrom(Collection.class);
        assert Iterable.class.isAssignableFrom(ArrayListSequence.class);
        final Class<Iterable<Iterable<Value_Type>>> type = null;
        final Iterable<Iterable<Value_Type>> iterable = StaticLoophole.cast(type, sequences);
        return Iterables.flatten1(iterable).iterator();
    }

}

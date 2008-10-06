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
/*VCSID=6b91e61c-4bed-4e7e-9f5a-14fc8d20898f*/
package com.sun.max.collect;

import java.util.*;

import com.sun.max.lang.*;

/**
 * This is an {@link IdentityHashSet} with a predictable iteration order based on the order in which elements are
 * {@linkplain #add(Object) inserted} (<i>a la</i>{@link LinkedHashMap}). In particular, insertion order is not
 * affected if a key is <i>re-inserted</i> into the map.
 * <p>
 * Note that {@linkplain #add(Object) insertion} and {@linkplain #iterator() iteration} should have the same performance
 * as for a standard {@code IdentityHashSet}. However, {@linkplain #remove(Object) deletion} is a linear operation due
 * to the linked list use to record insertion order.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public class LinkedIdentityHashSet<Element_Type> extends IdentityHashSet<Element_Type> implements VariableDeterministicSet<Element_Type> {

    public LinkedIdentityHashSet() {
        super();
    }

    public LinkedIdentityHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedIdentityHashSet(Element_Type element) {
        super();
        add(element);
    }

    public LinkedIdentityHashSet(Element_Type[] elements) {
        super();
        GrowableDeterministicSet.Static.addAll(this, elements);
    }

    private final LinkedList<Element_Type> _order = new LinkedList<Element_Type>();

    @Override
    public boolean add(Element_Type element) {
        if (!super.add(element)) {
            _order.add(element);
            return false;
        }
        return true;
    }

    /**
     * Gets an iterator over the elements in the order they were (originally) inserted.
     */
    @Override
    public Iterator<Element_Type> iterator() {
        assert _order.size() == length() : _order.size() + " != " + length();
        return _order.iterator();
    }

    @Override
    public void remove(Element_Type element) {
        _order.remove(element);
        super.remove(element);
    }

    @Override
    public void clear() {
        _order.clear();
        super.clear();
    }

    @Override
    public Element_Type first() {
        return _order.getFirst();
    }

    public Element_Type getOne() {
        return first();
    }

    public Element_Type last() {
        return _order.getLast();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LinkedIdentityHashSet) {
            final LinkedIdentityHashSet set = (LinkedIdentityHashSet) other;
            if (_order.size() != set._order.size()) {
                return false;
            }
            final Iterator iterator = set._order.iterator();
            for (Element_Type element : _order) {
                if (element != iterator.next()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _order.hashCode();
    }

    @Override
    public LinkedIdentityHashSet<Element_Type> clone() {
        return StaticLoophole.cast(super.clone());
    }
}

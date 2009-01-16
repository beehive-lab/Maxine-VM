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

/**
 * Similar to IdentityHashSet, but not recording 'null' as a set member
 * and not providing any element removal operations.
 * 
 * (IdentityHashSet's iterator seems to return 'null' as a member even if it has never been entered.)
 *
 * @author Bernd Mathiske
 */
public class IdentitySet<Element_Type> implements Iterable<Element_Type> {

    private int _numberOfElements;

    public int numberOfElements() {
        return _numberOfElements;
    }

    private final Class<Element_Type> _elementType;
    private Element_Type[] _table;
    private int _threshold;

    private void setThreshold() {
        if (_table.length == 0) {
            _threshold = -1;
        } else {
            _threshold = (_table.length / 4) * 3;
        }
    }

    /**
     * Constructs a new, empty IdentitySet.
     * @param elementType the type of elements to be held by the set
     */
    public IdentitySet(Class<Element_Type> elementType) {
        _elementType = elementType;
        _table = com.sun.max.lang.Arrays.newInstance(elementType, 16);
        setThreshold();
    }

    /**
     * Constructs a new, empty IdentitySet.
     * @param elementType the type of elements to be held by the set
     * @param initialCapacity  the initial capacity of the Set
     * @throws  {@code IllegalArgumentException} if {@code initialCapacity} is less than zero}
     */
    public IdentitySet(Class<Element_Type> elementType, int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException();
        }
        _elementType = elementType;
        _table = com.sun.max.lang.Arrays.newInstance(elementType, initialCapacity);
        setThreshold();
    }

    private void grow() {
        final Element_Type[] oldTable = _table;
        _table = com.sun.max.lang.Arrays.newInstance(_elementType, Math.max(_table.length * 2, 1));
        setThreshold();
        _numberOfElements = 0;
        for (int i = 0; i < oldTable.length; i++) {
            add(oldTable[i]);
        }
    }

    public void add(Element_Type element) {
        if (element == null) {
            return;
        }
        if (_numberOfElements > _threshold) {
            grow();
        }
        final int start = System.identityHashCode(element) % _table.length;
        int i = start;
        do {
            final Element_Type entry = _table[i];
            if (entry == null) {
                _table[i] = element;
                _numberOfElements++;
                return;
            }
            if (entry == element) {
                return;
            }
            if (++i == _table.length) {
                i = 0;
            }
        } while (i != start);
    }

    public boolean contains(Element_Type element) {
        if (element == null) {
            return false;
        }
        final int start = System.identityHashCode(element) % _table.length;
        int i = start;
        while (true) {
            final Element_Type entry = _table[i];
            if (entry == element) {
                return true;
            }
            if (entry == null) {
                return false;
            }
            if (++i == _table.length) {
                i = 0;
            }
            assert i != start;
        }
    }

    public Iterator<Element_Type> iterator() {
        final Element_Type[] array = com.sun.max.lang.Arrays.newInstance(_elementType, numberOfElements());
        int j = 0;
        for (int i = 0; i < _table.length; i++) {
            final Element_Type element = _table[i];
            if (element != null) {
                array[j++] = element;
            }
        }
        return com.sun.max.lang.Arrays.iterator(array);
    }
}

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
 * An identity hash set backed by java.util.IdentityHashMap.
 *
 * @author Hiroshi Yamauchi
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public class IdentityHashSet<Element_Type> implements Iterable<Element_Type>, Cloneable {

    private final Map<Element_Type, Element_Type> _internalMap;

    public IdentityHashSet() {
        _internalMap = new IdentityHashMap<Element_Type, Element_Type>();
    }

    public IdentityHashSet(int initialCapacity) {
        _internalMap = new IdentityHashMap<Element_Type, Element_Type>(initialCapacity);
    }

    public IdentityHashSet(Element_Type[] elements) {
        this();
        addAll(elements);
    }

    public IdentityHashSet(Iterable<Element_Type> elements) {
        this ();
        addAll(elements);
    }

    /**
     * Adds a specified element to this set.
     *
     * @param element the element to add
     * @return true if {@code element} was already in this set
     */
    public boolean add(Element_Type element) {
        return _internalMap.put(element, element) != null;
    }

    /**
     * Adds all the elements in a given Iterable to this set. The addition is always done by calling
     * {@link #add(Object)}.
     *
     * @param iterable the collection of elements to add
     */
    public final void addAll(Iterable<? extends Element_Type> iterable) {
        for (Element_Type element : iterable) {
            add(element);
        }
    }

    /**
     * Adds all the elements in a given array to this set. The addition is always done by calling
     * {@link #add(Object)}.
     *
     * @param elements the collection of elements to add
     */
    public final void addAll(Element_Type[] elements) {
        for (Element_Type element : elements) {
            add(element);
        }
    }

    public boolean isEmpty() {
        return _internalMap.isEmpty();
    }

    public void clear() {
        _internalMap.clear();
    }

    /**
     * Remove an element from the set.
     *
     * @param element the element to remove.
     */
    public void remove(Element_Type element) {
        _internalMap.remove(element);
    }

    /**
     * Removes all the elements in a given Iterable from this set. The removal is always done by calling
     * {@link #remove(Object)}.
     *
     * @param iterable the collection of elements to remove
     */
    public final void removeAll(Iterable<? extends Element_Type> iterable) {
        for (Element_Type element : iterable) {
            remove(element);
        }
    }

    /**
     * Removes all the elements in a given array from this set. The removal is always done by calling
     * {@link #remove(Object)}.
     *
     * @param elements the collection of elements to remove
     */
    public final void removeAll(Element_Type[] elements) {
        for (Element_Type element : elements) {
            remove(element);
        }
    }

    /**
     * @return the first element of the set returned by the iterator if present
     * @throws NoSuchElementException if the set is empty
     */
    public Element_Type first() {
        final Iterator<Element_Type> iterator = _internalMap.keySet().iterator();
        return iterator.next();
    }

    public Iterator<Element_Type> iterator() {
        return _internalMap.keySet().iterator();
    }

    @Override
    public IdentityHashSet<Element_Type> clone() {
        final IdentityHashSet<Element_Type> copy = new IdentityHashSet<Element_Type>();
        copy.addAll(this);
        return copy;
    }

    public boolean contains(Element_Type element) {
        return _internalMap.containsKey(element);
    }

    public int length() {
        return _internalMap.size();
    }

    public final IdentityHashSet<Element_Type> union(IdentityHashSet<Element_Type> other) {
        for (Element_Type element : other) {
            add(element);
        }
        return this;
    }

    public boolean isSuperSetOf(IdentityHashSet<Element_Type> other) {
        if (length() < other.length()) {
            return false;
        }
        for (Element_Type element : other) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStrictSuperSetOf(IdentityHashSet<Element_Type> other) {
        if (length() <= other.length()) {
            return false;
        }
        for (Element_Type element : other) {
            if (!contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isSubSetOf(IdentityHashSet<Element_Type> other) {
        if (length() > other.length()) {
            return false;
        }
        for (Element_Type element : this) {
            if (!other.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStrictSubSetOf(IdentityHashSet<Element_Type> other) {
        if (length() >= other.length()) {
            return false;
        }
        for (Element_Type element : this) {
            if (!other.contains(element)) {
                return false;
            }
        }
        return true;
    }

    public Element_Type[] toArray(Element_Type[] a) {
        return _internalMap.keySet().toArray(a);
    }

    @Override
    public String toString() {
        String string = "[ ";
        for (Element_Type element : _internalMap.keySet()) {
            string += element.toString() + " ";
        }
        string += "]";
        return string;
    }
}

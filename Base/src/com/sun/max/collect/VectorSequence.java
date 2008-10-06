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
/*VCSID=7fd1bc71-5e5c-496a-9bfd-10d6320f6302*/
package com.sun.max.collect;

import java.util.*;

/**
 * An implementation of a {@link VariableSequence} based on {@link Vector} which is synchronized .
 * 
 * @author Bernd Mathiske
 */
public class VectorSequence<Element_Type> extends Vector<Element_Type> implements VariableSequence<Element_Type> {

    public VectorSequence() {
    }

    public VectorSequence(int initialCapacity) {
        super(initialCapacity);
    }

    public VectorSequence(Collection<? extends Element_Type> collection) {
        super(collection);
    }

    public VectorSequence(Iterable<? extends Element_Type> elements) {
        for (Element_Type element : elements) {
            add(element);
        }
    }

    public VectorSequence(Element_Type element) {
        super();
        add(element);
    }

    public VectorSequence(Element_Type[] array) {
        super(java.util.Arrays.asList(array));
    }

    public int length() {
        return size();
    }

    public Element_Type first() {
        return get(0);
    }

    public Element_Type last() {
        return get(length() - 1);
    }

    public Element_Type removeFirst() {
        return remove(0);
    }

    public Element_Type removeLast() {
        return remove(length() - 1);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Sequence)) {
            return false;
        }
        final Sequence sequence = (Sequence) other;
        return Sequence.Static.equals(this, sequence);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public void append(Element_Type element) {
        add(element);
    }

    public void prepend(Element_Type element) {
        add(0, element);
    }

    public void insert(int index, Element_Type element) {
        add(index, element);
    }

    @Override
    public Sequence<Element_Type> clone() {
        return new VectorSequence<Element_Type>((Sequence<? extends Element_Type>) this);
    }

    public static <From_Type, To_Type> AppendableSequence<To_Type> map(Sequence<From_Type> from, Class<To_Type> toType, MapFunction<From_Type, To_Type> mapFunction) {
        final AppendableSequence<To_Type> to = new VectorSequence<To_Type>();
        for (From_Type element : from) {
            to.append(mapFunction.map(element));
        }
        return to;
    }

    @Override
    public String toString() {
        return "<" + Sequence.Static.toString(this, null, ", ") + ">";
    }
}

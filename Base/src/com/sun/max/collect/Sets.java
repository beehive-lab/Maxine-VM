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

import com.sun.max.util.*;

/**
 * Set operations that one could expect in java.util.
 *
 * @author Bernd Mathiske
 */
public final class Sets {

    private Sets() {

    }

    /**
     * Gets an immutable empty set.
     */
    public static <Element_Type> Set<Element_Type> empty(Class<Element_Type> elementType) {
        return Collections.emptySet();
    }

    public static <Element_Type> Set<Element_Type> from(Element_Type... elements) {
        return new HashSet<Element_Type>(java.util.Arrays.asList(elements));
    }

    public static <Element_Type> Set<Element_Type> from(Class<Element_Type> elementType, Element_Type... elements) {
        return new HashSet<Element_Type>(java.util.Arrays.asList(elements));
    }

    public static <Element_Type> Set<Element_Type> addAll(Set<Element_Type> set, Element_Type... elements) {
        for (Element_Type element : elements) {
            set.add(element);
        }
        return set;
    }

    public static <Element_Type> Set<Element_Type> addAll(Set<Element_Type> set, Iterable<? extends Element_Type> elements) {
        for (Element_Type element : elements) {
            set.add(element);
        }
        return set;
    }

    public static <Element_Type> Set<Element_Type> union(Set<Element_Type> set1, Set<Element_Type> set2) {
        final Set<Element_Type> result = new HashSet<Element_Type>(set1);
        result.addAll(set2);
        return result;
    }

    public static <Element_Type> Set<Element_Type> filter(Iterable<Element_Type> elements, Predicate<Element_Type> predicate) {
        final Set<Element_Type> result = new HashSet<Element_Type>();
        for (Element_Type element : elements) {
            if (predicate.evaluate(element)) {
                result.add(element);
            }
        }
        return result;
    }

    public static <From_Type, To_Type> Set<To_Type> map(Set<From_Type> from, Class<To_Type> toType, MapFunction<From_Type, To_Type> mapFunction) {
        final Set<To_Type> to = new HashSet<To_Type>();
        for (From_Type element : from) {
            to.add(mapFunction.map(element));
        }
        return to;
    }
}

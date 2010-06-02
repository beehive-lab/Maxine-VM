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
package com.sun.max.util;

import java.util.*;

import com.sun.max.program.*;

/**
 * An array-based recording of the history of a value, with
 * time expressed as the number of generations back from the current generation (0).
 *
 * @author Michael Van De Vanter
 */
public class ArrayValueHistory<E> {

    private final ArrayDeque<E> generations;
    private final int limit;
    private int age = -1;

    public ArrayValueHistory(int limit) {
        this.generations = new ArrayDeque<E>();
        this.limit = limit;
    }

    public ArrayValueHistory() {
        this (Integer.MAX_VALUE);
    }

    /**
     * Adds a new value, which becomes the current generation.
     * The generation of all previously recorded values increases by 1.
     */
    public void add(E newValue) {
        if (generations.size() > 0) {
            if (newValue.equals(generations.getFirst())) {
                if (age >= 0) {
                    age++;
                }
            } else {
                age = 0;
            }
        }
        generations.addFirst(newValue);
        if (generations.size() > limit) {
            generations.removeLast();
        }
    }

    /**
     * @return the "current" value (at generation 0).
     * Error if no values have been recorded.
     */
    public E get() {
        if (generations.size() > 0) {
            return generations.getFirst();
        }
        ProgramError.unexpected("empty history");
        return null;
    }

    /**
     * @return The value at a specified generation.
     * Error if generation does not exist.
     */
    public E get(int generation) {
        final Iterator<E> iterator = generations.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            if (index == generation) {
                return iterator.next();
            }
            index++;
        }
        ProgramError.unexpected("exceeded history");
        return null;
    }

    /**
     * @return the age, in generations, of the current value, since recording began.
     * 0 if different from immediate predecessor; -1 if no different value ever recorded
     * Comparison uses {@linkplain Object#equals(Object) equals}.
     */
    public int getAge() {
        return age;
    }

    /**
     * @return the maximum number of generations that can be recorded.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return the number of generations recorded; initially 0.
     */
    public int getSize() {
        return generations.size();
    }

    /**
     * @return iteration of the values recorded in the history, starting with the current
     * generation and proceeding backward in time.
     */
    public Iterator<E> values() {
        return generations.iterator();
    }

}

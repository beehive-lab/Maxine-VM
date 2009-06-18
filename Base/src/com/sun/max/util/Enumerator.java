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

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.program.*;

/**
 * @see Enumerable
 *
 * @author Bernd Mathiske
 */
public class Enumerator<Enumerable_Type extends Enum<Enumerable_Type> & Enumerable<Enumerable_Type>>
    implements Symbolizer<Enumerable_Type>, IndexedSequence<Enumerable_Type> {

    private final Class<Enumerable_Type> type;
    private final Enumerable_Type[] ordinalMap;
    private final Enumerable_Type[] valueMap;
    private final int lowestValue;

    public Enumerator(Class<Enumerable_Type> type) {
        this.type = type;
        ordinalMap = type.getEnumConstants();

        int lowValue = 0;
        int highestValue = ordinalMap.length - 1;
        boolean valuesAreSameAsOrdinals = true;
        for (Enumerable_Type e : ordinalMap) {
            final int value = e.value();
            if (value != e.ordinal()) {
                valuesAreSameAsOrdinals = false;
            }
            if (value < lowValue) {
                lowValue = value;
            } else if (value > highestValue) {
                highestValue = value;
            }
        }

        if (valuesAreSameAsOrdinals) {
            this.lowestValue = 0;
            valueMap = ordinalMap;
        } else {
            final int valueMapLength = (highestValue - lowValue) + 1;
            final Class<Enumerable_Type[]> arrayType = null;
            this.lowestValue = lowValue;
            valueMap = StaticLoophole.cast(arrayType, new Enum[valueMapLength]);
            for (Enumerable_Type e : ordinalMap) {
                final int value = e.value();
                // The enumerable with the lowest ordinal is stored in the value map:
                if (valueMap[value] == null) {
                    valueMap[value] = e;
                }
            }
        }
    }

    public Class<Enumerable_Type> type() {
        return type;
    }

    public int numberOfValues() {
        return ordinalMap.length;
    }

    /**
     * Adds all the enumerable constants in this enumerator to a given set.
     *
     * @param set
     *                the set to which the enumerable constants are to be added
     */
    public void addAll(Set<Enumerable_Type> set) {
        for (Enumerable_Type e : this) {
            set.add(e);
        }
    }

    public Enumerable_Type first() {
        return ordinalMap[0];
    }

    public boolean isEmpty() {
        return ordinalMap.length == 0;
    }

    public Enumerable_Type last() {
        return ordinalMap[length() - 1];
    }

    public int length() {
        return ordinalMap.length;
    }

    public Iterator<Enumerable_Type> iterator() {
        return Arrays.iterator(ordinalMap);
    }

    @Override
    public Sequence<Enumerable_Type> clone() {
        try {
            return StaticLoophole.cast(super.clone());
        } catch (CloneNotSupportedException e) {
            ProgramError.unexpected();
            return null;
        }
    }

    public Collection<Enumerable_Type> toCollection() {
        return java.util.Arrays.asList(ordinalMap);
    }

    /**
     * Gets the enumerable constant denoted by a given ordinal. Note that this differs from {@link #fromValue(int)} in
     * that the latter retrieves an enumerable constant matching a given {@linkplain Enumerable#value() value}. An
     * enumerable's value is not necessarily the same as its ordinal.
     *
     * @throws IndexOutOfBoundsException
     *                 if {@code 0 < ordinal || ordinal >= length()}
     */
    public Enumerable_Type get(int ordinal) throws IndexOutOfBoundsException {
        return ordinalMap[ordinal];
    }

    /**
     * Gets the enumerable constant matching a given value. That is, this method gets an enumerable from this enumerator
     * whose {@linkplain Enumerable#value() value} is equal to {@code value}. Note that the given value may not match
     * any enumerable in this enumerator in which case null is returned. Additionally, there may be more than one
     * enumerable with a matching value in which case the matching enumerable with the lowest
     * {@linkplain Enum#ordinal() ordinal} is returned.
     */
    public Enumerable_Type fromValue(int value) {
        final int index = value - lowestValue;
        if (index >= 0 && index < valueMap.length) {
            return valueMap[index];
        }
        return null;
    }
}

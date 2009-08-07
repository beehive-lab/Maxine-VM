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
 * Additional dealings with Enum types.
 *
 * @author Bernd Mathiske
 */
public final class Enums {

    private Enums() {
    }

    public static String fullName(Enum e) {
        return e.getClass().getCanonicalName() + "." + e.name();
    }

    public static <Enum_Type extends Enum<Enum_Type>> Enum_Type fromString(final Class<Enum_Type> enumClass, String name) {
        for (Enum_Type enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(name)) {
                return enumConstant;
            }
        }
        return null;
    }

    /**
     * A "power sequence" is like a "power set" (the set of all possible subsets),
     * but with Set replaced by Sequence, using "natural" ordering.
     * Each sequence representing a subset is ordered according to ascending enum ordinals.
     * The sequence of sequences is ordered as if sorting an array of integers derived from enum bit sets.
     * 
     * For example, "powerSequence(Enum_Type.class).get(11)",
     * i.e. querying a power sequence with index 11 (having bits 1, 2 and 8 set)
     * will return the sequence containing the enum values with the ordinals 1, 2 and 8, in that order.
     * 
     * @see #powerSequenceIndex(Enum)
     */
    public static <Enum_Type extends Enum<Enum_Type>> IndexedSequence<IndexedSequence<Enum_Type>> powerSequence(Class<Enum_Type> enumType) {
        final EnumSet<Enum_Type> values = EnumSet.allOf(enumType);
        final int nSubSets = (int) Math.pow(2, values.size());
        final MutableSequence<IndexedSequence<Enum_Type>> result = new ArraySequence<IndexedSequence<Enum_Type>>(nSubSets);
        for (int subSetIndex = 0; subSetIndex < nSubSets; subSetIndex++) {
            final MutableSequence<Enum_Type> subSet = new ArraySequence<Enum_Type>(Integer.bitCount(subSetIndex));
            result.set(subSetIndex, subSet);
            int i = 0;
            for (Enum_Type value : values) {
                if ((subSetIndex & (1 << value.ordinal())) != 0) {
                    subSet.set(i, value);
                    i++;
                }
            }
        }
        return result;
    }

    /**
     * @return the index into the "power sequence" of the enum set's Enum type that corresponds to the given enum set
     * 
     * @see #powerSequence(Class)
     */
    public static <Enum_Type extends Enum<Enum_Type>> int powerSequenceIndex(EnumSet<Enum_Type> enumSet) {
        int result = 0;
        for (Enum_Type value : enumSet) {
            result |= 1 << value.ordinal();
        }
        return result;
    }

    /** @return the index of the singleton set containing {@code value} within
     * the "power sequence of the complete EnumSet of the Enum type.
     */
    public static int powerSequenceIndex(Enum value) {
        return 1 << value.ordinal();
    }

    /**
     * Test whether the given constant is among the allowed constants.
     * If so, return the given constant, otherwise return the first allowed constant.
     * If no constant is allowed return null.
     */
    public static <Enum_Type extends Enum<Enum_Type>> Enum_Type constrain(Enum_Type constant, Enum_Type... allowedConstants) {
        if (allowedConstants.length == 0) {
            return null;
        }
        for (Enum_Type e : allowedConstants) {
            if (e == constant) {
                return constant;
            }
        }
        return allowedConstants[0];
    }

}

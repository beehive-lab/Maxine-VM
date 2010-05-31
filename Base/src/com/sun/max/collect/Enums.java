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

    public static <T extends Enum<T>> T fromString(final Class<T> enumClass, String name) {
        for (T enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equals(name)) {
                return enumConstant;
            }
        }
        return null;
    }

    /**
     * @return the index into the "power sequence" of the enum set's Enum type that corresponds to the given enum set
     *
     * @see #powerSequence(Class)
     */
    public static <T extends Enum<T>> int powerSequenceIndex(EnumSet<T> enumSet) {
        int result = 0;
        for (T value : enumSet) {
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
    public static <T extends Enum<T>> T constrain(T constant, T... allowedConstants) {
        if (allowedConstants.length == 0) {
            return null;
        }
        for (T e : allowedConstants) {
            if (e == constant) {
                return constant;
            }
        }
        return allowedConstants[0];
    }

}

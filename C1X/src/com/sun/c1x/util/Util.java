/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.util;

import java.util.List;

/**
 * The <code>Util</code> class contains a number of utility methods used throughout the compiler.
 *
 * @author Ben L. Titzer
 */
public class Util {

    public static RuntimeException unimplemented() {
        throw new Error("unimplemented");
    }

    public static RuntimeException shouldNotReachHere() {
        throw new Error("should not reach here");
    }

    public static <T> boolean replaceInList(T a, T b, List<T> list) {
        final int max = list.size();
        for (int i = 0; i < max; i++) {
            if (list.get(i) == a) {
                list.set(i, b);
                return true;
            }
        }
        return false;
    }

    public static boolean isPowerOf2(int val) {
        return val != 0 && (val & val - 1) == 0;
    }

    public static boolean isPowerOf2(long val) {
        return val != 0 && (val & val - 1) == 0;
    }

    public static int log2(int val) {
        assert val > 0 && isPowerOf2(val);
        return 32 - Integer.numberOfLeadingZeros(val);
    }

    public static int log2(long val) {
        assert val > 0 && isPowerOf2(val);
        return 64 - Long.numberOfLeadingZeros(val);
    }

    /**
     * Statically cast an object to an arbitrary Object type WITHOUT eliminating dynamic erasure checks.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Class<T> type, Object object) {
        return (T) object;
    }

    /**
     * Statically cast an object to an arbitrary Object type WITHOUT eliminating dynamic erasure checks.
     */
    @SuppressWarnings("unchecked")
    public static <T> T uncheckedCast(Object object) {
        return (T) object;
    }

}

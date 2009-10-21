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
package com.sun.max.vm.object.host;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * This class implements access routines that are used when running in bootstrapping mode.
 * They use the underlying VM's reflective mechanisms to implement array accesses.
 * 
 * @author Bernd Mathiske
 */
@HOSTED_ONLY
public final class HostArrayAccess {
    private HostArrayAccess() {
    }

    /**
     * Checks the index into an array against the bounds of the array.
     * @param array the array object
     * @param index the index into the array
     */
    public static void checkIndex(Object array, int index) {
        if (index < 0 || index >= HostObjectAccess.getArrayLength(array)) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Performs an array store check and throws an {@code ArrayStoreException} if the check fails.
     * @param array the object array
     * @param value the value to store into the object array
     */
    public static void checkSetObject(Object array, Object value) {
        final Class arrayClass = array.getClass();
        final Class componentType = arrayClass.getComponentType();
        if (value != null) {
            if (!componentType.isInstance(value)) {
                throw new ArrayStoreException("cannot store a " + value.getClass().getName() + " to a " + componentType.getSimpleName() + " array");
            }
        } else {
            if (Word.class.isAssignableFrom(componentType)) {
                throw new ArrayStoreException("cannot store null to a " + componentType.getSimpleName() + " array");
            }
        }
    }

    public static byte getByte(Object array, int index) {
        if (array instanceof boolean[]) {
            final boolean[] booleanArray = (boolean[]) array;
            return booleanArray[index] ? (byte) 1 : (byte) 0;
        }
        assert array instanceof byte[];
        final byte[] byteArray = (byte[]) array;
        return byteArray[index];
    }

    public static void setByte(Object array, int index, byte value) {
        if (array instanceof boolean[]) {
            final boolean[] booleanArray = (boolean[]) array;
            booleanArray[index] = value != 0;
        } else {
            final byte[] byteArray = (byte[]) array;
            byteArray[index] = value;
        }
    }
}

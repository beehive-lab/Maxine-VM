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
package com.sun.max.vm.object;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.reference.*;

/**
 * This class implements a facade for the {@link Layout Layout} class, which
 * accepts only {@code Reference} objects.
 *
 * @author Bernd Mathiske
 */
public final class ObjectAccess {

    protected ObjectAccess() {
    }

    /**
     * Checks whether the specified object is {@code zero} (i.e. is {@code null}).
     *
     * @param object the object to check
     * @return {@code true} if the object reference is {@code zero}; {@code false} otherwise
     */
    @INLINE
    public static boolean isZero(Object object) {
        return Reference.fromJava(object).isZero();
    }


    public static boolean canBeValid(Object o) {
        return canBeValid(Reference.fromJava(o).toOrigin());
    }

    /**
     * Conservatively checks whether the given parameter points to an object in the heap.
     * Gives a false-positive in case of the pointer pointing to a field that contains an object.
     * @param origin the pointer to be checked
     * @return whether the pointer points to an object
     */
    public static boolean canBeValid(Pointer origin) {

        if (origin.isZero()) {
            return true;
        }

        // Checks for 4 hops, because of the two cases:
        // Pointing to an object: object -> dynamic hub -> hub hub -> hub hub -> hub hub
        // Pointing to a static hub: static tuple -> static hub -> dynamic hub -> hub hub -> hub hub

        Pointer current = origin;
        Pointer last = Pointer.zero();
        int i = 0;
        for (; i < 4 && (Heap.contains(current) || Code.contains(current)); i++) {
            last = current;
            current = Layout.generalLayout().readHubReferenceAsWord(current).asPointer();
        }

        if (i == 4 && current.equals(last)) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether the specified object is an array.
     *
     * @param object the object to check
     * @return {@code true} if the object is an array; {@code false} otherwise
     */
    public static boolean isArray(Object object) {
        return Layout.isArray(Reference.fromJava(object));
    }

    /**
     * Converts an object reference to a pointer to the object's <i>origin</i>.
     *
     * @param object the object
     * @return a pointer to the specified object's origin
     */
    @INLINE
    public static Pointer toOrigin(Object object) {
        return Reference.fromJava(object).toOrigin();
    }

    /**
     * Reads the hub of an object reference.
     *
     * @param object the object for which to read the hub
     * @return a reference to the hub of the object
     */
    @INLINE
    public static Hub readHub(Object object) {
        return UnsafeLoophole.asHub(Reference.fromJava(object).readHubReference().toJava());
    }

    /**
     * Reads the hub of an object and uses the hub to get the class actor for the specified object.
     *
     * @param object the object for which to read the hub
     * @return a reference to the class actor, which represents the object's type
     */
    @INLINE
    public static ClassActor readClassActor(Object object) {
        final Hub hub = UnsafeLoophole.asHub(Reference.fromJava(object).readHubReference().toJava());
        return hub.classActor;
    }

    /**
     * Reads the "misc" word from an object's header. The misc word encodes lock state, hash code, etc.
     *
     * @param object the object for which to read the misc word
     * @return the misc word for the specified object
     */
    @INLINE
    public static Word readMisc(Object object) {
        return Layout.readMisc(Reference.fromJava(object));
    }

    /**
     * Write the "misc" word into an object's header.
     *
     * @param object the object for which to write the misc word
     * @param value the new value for the misc word
     */
    @INLINE
    public static void writeMisc(Object object, Word value) {
        Layout.writeMisc(Reference.fromJava(object), value);
    }

    /**
     * Atomically compare and swap the misc word of an object header.
     *
     * @param object the object in whose header to compare and swap
     * @param suspectedValue the value to compare the misc word against
     * @param newValue the new value to be written into the object header
     * @return the old value of the misc word, if the comparison succeeds; the new value if it fails
     */
    @INLINE
    public static Word compareAndSwapMisc(Object object, Word suspectedValue, Word newValue) {
        return Layout.compareAndSwapMisc(Reference.fromJava(object), suspectedValue, newValue);
    }

    /**
     * Computes a new default hashcode for the specified object.
     *
     * @param object the object for which to calculate the hash code
     * @return a new identity hashcode for the specified object
     */
    public static int makeHashCode(Object object) {
        return Monitor.makeHashCode(object);
    }

    /**
     * Compute the cell size of an object using the appropriate layout.
     *
     * @param object the object for which to compute the size
     * @return the size of the objects in bytes
     */
    public static Size size(Object object) {
        return Layout.size(Reference.fromJava(object));
    }

}

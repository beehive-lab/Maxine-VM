/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.object;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.vm.MaxineVM.*;

import java.lang.reflect.*;
import java.nio.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jdk.*;
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
        if (isHosted()) {
            if (object instanceof StaticTuple) {
                final StaticTuple staticTuple = (StaticTuple) object;
                return staticTuple.classActor().staticHub();
            }
            return ClassActor.fromJava(object.getClass()).dynamicHub();
        }
        return UnsafeCast.asHub(Reference.fromJava(object).readHubReference().toJava());
    }

    /**
     * Reads the hub of an object and uses the hub to get the class actor for the specified object.
     *
     * @param object the object for which to read the hub
     * @return a reference to the class actor, which represents the object's type
     */
    @INLINE
    public static ClassActor readClassActor(Object object) {
        final Hub hub = UnsafeCast.asHub(Reference.fromJava(object).readHubReference().toJava());
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
     * @param expectedValue the value to compare the misc word against
     * @param newValue the new value to be written into the object header
     * @return the old value of the misc word, if the comparison succeeds; the new value if it fails
     */
    @INLINE
    public static Word compareAndSwapMisc(Object object, Word expectedValue, Word newValue) {
        return Layout.compareAndSwapMisc(Reference.fromJava(object), expectedValue, newValue);
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
        if (isHosted()) {
            Hub hub = readHub(object);
            if (object.getClass().isArray()) {
                final ArrayLayout arrayLayout = (ArrayLayout) hub.specificLayout;
                return arrayLayout.getArraySize(Array.getLength(object));
            }
            if (object instanceof Hybrid) {
                final Hybrid hybrid = (Hybrid) object;
                final HybridLayout hybridLayout = (HybridLayout) hub.specificLayout;
                return hybridLayout.getArraySize(hybrid.length());
            }
            return hub.tupleSize;
        }
        return Layout.size(Reference.fromJava(object));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native ObjectAccess asThis(Object buffer);

    @ALIAS(declaringClassName = "java.nio.DirectByteBuffer", name = "<init>")
    private native void init(long addr, int capacity);

    /**
     * Creates a new instance of the package private class java.nio.DirectByteBuffer.
     */
    public static ByteBuffer createDirectByteBuffer(long address, int capacity) {
        Object buffer = Heap.createTuple(JDK.java_nio_DirectByteBuffer.classActor().dynamicHub());
        asThis(buffer).init(address, capacity);
        ByteBuffer directBuffer = (ByteBuffer) buffer;
        return directBuffer;
    }
}

/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.reference.*;


public class JVMTIUtil {
    /**
     * The offset of the byte array data from the byte array object's origin.
     */
    static final Offset byteDataOffset = VMConfiguration.vmConfig().layoutScheme().byteArrayLayout.getElementOffsetFromOrigin(0);

    static Pointer getByteArrayStart(byte[] data) {
        return Reference.fromJava(data).toOrigin().plus(JVMTIUtil.byteDataOffset);
    }

    /**
     * A tagged "union" type for basic types.
     */
    static class TypedData {
        static final int DATA_NONE = 0;
        static final int DATA_INT = 'I';
        static final int DATA_LONG = 'J';
        static final int DATA_FLOAT = 'F';
        static final int DATA_DOUBLE = 'D';
        static final int DATA_OBJECT = 'L';
        static final int DATA_WORD = 'W';

        TypedData() {
        }

        TypedData(int tag) {
            this.tag = tag;
        }

        TypedData(int tag, int value) {
            this.tag = tag;
            this.intValue = value;
        }

        TypedData(int tag, long value) {
            this.tag = tag;
            this.longValue = value;
        }

        TypedData(int tag, float value) {
            this.tag = tag;
            this.floatValue = value;
        }

        TypedData(int tag, double value) {
            this.tag = tag;
            this.doubleValue = value;
        }

        TypedData(int tag, Object value) {
            this.tag = tag;
            this.objectValue = value;
        }

        int tag;
        int intValue;
        long longValue;
        float floatValue;
        double doubleValue;
        Object objectValue;
        Word wordValue;
    }

    /**
     * gets to private internals of {@link ClassActor} and avoids placing any JVMTI dependency there, but that could be
     * revisited.
     */
    static class ClassActorProxy {

        @INTRINSIC(UNSAFE_CAST)
        public static native ClassActorProxy asClassActorProxy(Object object);

        @ALIAS(declaringClass = ClassActor.class)
        Object initializationState;
        @ALIAS(declaringClass = ClassActor.class)
        static Object INITIALIZED;
        @ALIAS(declaringClass = ClassActor.class)
        static Object PREPARED;
        @ALIAS(declaringClass = ClassActor.class)
        static Object VERIFIED_;
        @ALIAS(declaringClass = ClassActor.class)
        Class javaClass;
        @ALIAS(declaringClass = ClassActor.class)
        MethodActor[] methodActors;
    }

    static ClassMethodActor toClassMethodActor(MethodID methodID) {
        try {
            return (ClassMethodActor) MethodID.toMethodActor(methodID);
        } catch (ClassCastException ex) {
            return null;
        }
    }

}

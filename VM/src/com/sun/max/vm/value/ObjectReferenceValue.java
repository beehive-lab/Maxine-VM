/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.value;

import static com.sun.cri.bytecode.Bytecodes.*;

import java.io.*;

import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;

/**
 * Boxed representations of object references (and null) that use a reference of type 'Object'.
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 */
public final class ObjectReferenceValue extends ReferenceValue {

    @INSPECTED
    private final Object value;

    public static final ObjectReferenceValue NULL_OBJECT = new ObjectReferenceValue(null);

    public static ObjectReferenceValue from(Object object) {
        if (object == null) {
            return NULL_OBJECT;
        }
        return new ObjectReferenceValue(object);
    }

    private ObjectReferenceValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean isZero() {
        return value == null;
    }

    @Override
    public boolean isAllOnes() {
        return asReference().isAllOnes();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            // common case of reference equality
            return true;
        }
        if (other instanceof ObjectReferenceValue) {
            final ObjectReferenceValue referenceValue = (ObjectReferenceValue) other;
            return value == referenceValue.value;
        }
        return false;
    }

    @Override
    protected int compareSameKind(ReferenceValue other) {
        // TODO: It seems impossible to find a way to deterministically order the identities of two objects.
        //       So, Value should not implement Comparable!
        throw new IllegalArgumentException("Cannot perform comparison between values of kind " + kind());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }

    @Override
    public String toString() {
        if (value == null) {
            return "null";
        }
        if (!MaxineVM.isHosted() && ObjectAccess.readHub(value) instanceof StaticHub) {
            return "staticTuple-" + ObjectAccess.readHub(value).classActor.simpleName();
        }
        return value.toString();
    }

    @Override
    public Object asBoxedJavaValue() {
        return value;
    }

    @Override
    public Reference asReference() {
        return Reference.fromJava(value);
    }

    @Override
    public Object asObject() {
        return value;
    }

    @Override
    public Object unboxObject() {
        return value;
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native Word toWord(Object value);

    @Override
    public Word unboxWord() {
        return toWord(value);
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return Word.widthValue();
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return Word.widthValue();
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return dataModel.toBytes(Reference.fromJava(value).toOrigin());
    }

    @Override
    public ClassActor getClassActor() {
        return ClassActor.fromJava(value.getClass());
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        if (value == null) {
            Word.zero().write(stream);
        } else {
            super.write(stream);
        }
    }
}

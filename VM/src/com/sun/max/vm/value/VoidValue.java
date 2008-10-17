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
package com.sun.max.vm.value;

import java.io.*;

import com.sun.max.lang.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class VoidValue extends Value<VoidValue> {

    private VoidValue() {
        super();
    }

    public static final VoidValue VOID = new VoidValue();

    @Override
    public Kind<VoidValue> kind() {
        return Kind.VOID;
    }

    @Override
    public int hashCode() {
        return -1;
    }

    @Override
    public boolean isZero() {
        return true;
    }

    @Override
    public boolean isAllOnes() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    protected int compareSameKind(VoidValue other) {
        if (this == other) {
            return 0;
        }
        // There is only one canonical instance of this value type
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return "void";
    }

    @Override
    public Object asBoxedJavaValue() {
        throw new IllegalArgumentException("no void value");
    }

    @Override
    public WordWidth signedEffectiveWidth() {
        return null;
    }

    @Override
    public WordWidth unsignedEffectiveWidth() {
        return null;
    }

    @Override
    public byte[] toBytes(DataModel dataModel) {
        return new byte[0];
    }

    @Override
    public void write(DataOutput stream) throws IOException {
    }
}

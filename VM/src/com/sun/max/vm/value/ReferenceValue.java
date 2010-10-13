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

import com.sun.cri.ci.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * Abstract type for different boxed representations of object references (and null).
 *
 * @see Reference
 *
 * @author Bernd Mathiske
 * @author Athul Acharya
 */
public abstract class ReferenceValue extends Value<ReferenceValue> {

    protected ReferenceValue() {
    }

    @Override
    public Kind<ReferenceValue> kind() {
        return Kind.REFERENCE;
    }

    public static ReferenceValue from(Object object) {
        return ObjectReferenceValue.from(object);
    }

    public static ReferenceValue fromReference(Reference reference) {
        return ObjectReferenceValue.from(reference.toJava());
    }

    @Override
    public void write(DataOutput stream) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Boxed representation of 'null'.
     */
    public static final ReferenceValue NULL = ObjectReferenceValue.NULL_OBJECT;

    public abstract ClassActor getClassActor();

    @Override
    public CiConstant asCiConstant() {
        return CiConstant.forObject(asObject());
    }
}

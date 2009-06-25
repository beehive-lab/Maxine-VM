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
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class ReferenceFieldActor extends FieldActor<ReferenceValue> {

    public ReferenceFieldActor(Utf8Constant name, TypeDescriptor descriptor, int flags) {
        super(Kind.REFERENCE, name, descriptor, flags);
    }

    @INLINE
    public final Object readObject(Object object) {
        return TupleAccess.readObject(object, offset());
    }

    @INLINE
    public final void writeObject(Object object, Object value) {
        TupleAccess.writeObject(object, offset(), value);
    }

    @INLINE
    public final void writeStatic(Object value) {
        writeObject(holder().staticTuple(), value);
    }

    public final Object readStatic() {
        return readObject(holder().staticTuple());
    }

    @FOLD
    public static ReferenceFieldActor findInstance(Class javaClass, String name) {
        return (ReferenceFieldActor) FieldActor.findInstance(javaClass, name);
    }

    @FOLD
    public static ReferenceFieldActor findStatic(Class javaClass, String name) {
        return (ReferenceFieldActor) FieldActor.findStatic(javaClass, name);
    }
}

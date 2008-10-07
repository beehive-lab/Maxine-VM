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
/*VCSID=3aa5f876-4006-4484-aa05-156bcf909c17*/
package com.sun.max.vm.actor.member;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class BooleanFieldActor extends FieldActor<BooleanValue> {

    public BooleanFieldActor(Utf8Constant name, int flags) {
        super(Kind.BOOLEAN,
              name,
              JavaTypeDescriptor.BOOLEAN,
              flags);
    }

    @INLINE
    public final boolean readBoolean(Object object) {
        return TupleAccess.readBoolean(object, offset());
    }

    @INLINE
    public final void writeBoolean(Object object, boolean value) {
        TupleAccess.writeBoolean(object, offset(), value);
    }

    @FOLD
    public static BooleanFieldActor findDynamic(Class javaClass, String name) {
        return (BooleanFieldActor) FieldActor.findInstance(javaClass, name);
    }

    @FOLD
    public static BooleanFieldActor findStatic(Class javaClass, String name) {
        return (BooleanFieldActor) FieldActor.findStatic(javaClass, name);
    }

}

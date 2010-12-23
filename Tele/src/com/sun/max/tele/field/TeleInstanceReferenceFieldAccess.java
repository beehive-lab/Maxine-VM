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
package com.sun.max.tele.field;

import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class TeleInstanceReferenceFieldAccess extends TeleInstanceFieldAccess {

    public TeleInstanceReferenceFieldAccess(Class holder, String name, Class<?> type) {
        super(holder, name, Kind.REFERENCE);
        TeleError.check(ClassActor.fromJava(type).isAssignableFrom(fieldActor().descriptor().resolve(fieldActor().holder().classLoader)), "field has wrong type: " + name + " in class: " + holder);
        final Kind kind = fieldActor().descriptor().toKind();
        TeleError.check(kind != Kind.WORD, "Word field used as Reference field: " + fieldActor());
    }

    public TeleInstanceReferenceFieldAccess(Class holder, Class<?> type, InjectedReferenceFieldActor injectedReferenceFieldActor) {
        this(holder, injectedReferenceFieldActor.name.toString(), type);
    }

    public Reference readReference(Reference reference) {
        return reference.readReference(fieldActor().offset());
    }

    public static Reference readPath(Reference reference, TeleInstanceReferenceFieldAccess... fields) {
        Reference r = reference;
        for (TeleInstanceReferenceFieldAccess field : fields) {
            if (r.isZero()) {
                return r;
            }
            r = field.readReference(r);
        }
        return r;
    }

}

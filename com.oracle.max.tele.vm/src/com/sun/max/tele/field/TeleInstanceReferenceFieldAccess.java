/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.field;

import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * An accessor for reading object instance reference fields from VM memory, specified by class and field name.
 */
public final class TeleInstanceReferenceFieldAccess extends TeleInstanceFieldAccess {

    public TeleInstanceReferenceFieldAccess(Class holder, String name, Class<?> type) {
        super(holder, name, Kind.REFERENCE);
        TeleError.check(ClassActor.fromJava(type).isAssignableFrom(fieldActor().descriptor().resolve(fieldActor().holder().classLoader)), "field has wrong type: " + name + " in class: " + holder);
        final Kind kind = fieldActor().descriptor().toKind();
        TeleError.check(kind != Kind.WORD, "Word field used as Reference field: " + fieldActor());
    }

    public TeleInstanceReferenceFieldAccess(Class holder, Class<?> type, InjectedReferenceFieldActor injectedReferenceFieldActor) {
        this(holder, injectedReferenceFieldActor.name.toString(), type);
    }

    /**
     * Reads from the specified object in VM memory the reference held in the field encapsulated by this accessor,
     * traversing a forwarder if necessary; returns {@link RemoteReference#zero()} if the field's value does not point
     * at a live object.
     *
     * @return the value of the instance field in VM memory interpreted as a reference, unless it is a forwarder in
     *         which case the reference returned refers to the destination of the forwarder.
     */
    public RemoteReference readRemoteReference(RemoteReference remoteRef) {
        return remoteRef.readFieldAsRemoteReference(fieldActor());
    }

    public static RemoteReference readPath(RemoteReference reference, TeleInstanceReferenceFieldAccess... fields) {
        RemoteReference r = reference;
        for (TeleInstanceReferenceFieldAccess field : fields) {
            if (r.isZero()) {
                return r;
            }
            r = field.readRemoteReference(r);
        }
        return r;
    }

}

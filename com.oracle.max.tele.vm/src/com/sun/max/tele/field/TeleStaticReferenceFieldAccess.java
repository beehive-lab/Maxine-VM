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

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.type.*;

/**
 * An accessor for reading static reference fields from VM memory, specified by class and field name.
 */
public final class TeleStaticReferenceFieldAccess extends TeleStaticFieldAccess {

    public TeleStaticReferenceFieldAccess(Class holder, String name, Class type) {
        super(holder, name, Kind.fromJava(type));
        TeleError.check(ClassActor.fromJava(type).isAssignableFrom(fieldActor().descriptor().resolve(fieldActor().holder().classLoader)), "field has wrong type: " + name + " in class: " + holder);
    }

    /**
     * Reads a static reference field from VM memory,  following a forwarding pointer if necessary, or
     * {@link RemoteReference#zero()} if no live object can be found.
     *
     * @return the value of the field in VM memory interpreted as a reference, unless it is a forwarding pointer in
     * which case the reference returned refers to the destination of the forwarding pointer.
     */
    public RemoteReference readReference(MaxVM maxVM) {
        final TeleVM vm = (TeleVM) maxVM;
        final Address origin = staticTupleReference(vm).readWord(fieldActor().offset()).asAddress();
        final ObjectStatus status = vm.objects().objectStatusAt(origin);
        if (status.isLive()) {
            return vm.referenceManager().makeReference(origin);
        }
        if (status.isForwarder()) {
            final RemoteReference forwarderReference = vm.referenceManager().makeQuasiReference(origin);
            return vm.referenceManager().makeReference(forwarderReference.forwardedTo());
        }
        return vm.referenceManager().zeroReference();
    }

    /**
     * Reads a static reference field from VM memory either a live reference or quasi reference,
     * or {@link RemoteReference#zero()} if no live or quasi object can be found.
     *
     * @return the value of the field in VM memory interpreted as a reference
     */
    public RemoteReference readRawReference(MaxVM maxVM) {
        final TeleVM vm = (TeleVM) maxVM;
        final Address origin = staticTupleReference(vm).readWord(fieldActor().offset()).asAddress();
        final ObjectStatus status = vm.objects().objectStatusAt(origin);
        if (status.isLive()) {
            return vm.referenceManager().makeReference(origin);
        }
        if (status.isQuasi()) {
            return vm.referenceManager().makeQuasiReference(origin);
        }
        return vm.referenceManager().zeroReference();
    }

    /**
     * Read a reference as a Word from the field in the VM.
     * ATTENTION: an implicit type cast happens via remote access.
     */
    public Word readWord(MaxVM vm) {
        return staticTupleReference(vm).readWord(fieldActor().offset());
    }

    /**
     * Write a word as a reference into the field in the VM.
     * ATTENTION: an implicit type cast happens via remote access.
     */
    public void writeWord(MaxVM vm, Word value) {
        staticTupleReference(vm).writeWord(fieldActor().offset(), value);
    }
}

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
package com.sun.max.tele.reference;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;

/**
 * An address in VM memory about which little is known, wrapped
 * as if it were a legitimate object {@link Reference}, in violation
 * of the invariant that a {@link Reference} refers to an object.
 * <p>
 * This instance is not canonicalized, not GC-safe, and
 * is intended <strong>only for temporary use</strong>.
 * <p>
 * Its memory status is permanently {@link ObjectStatus#DEAD}.
 */
public final class UnsafeRemoteReference extends ConstantTeleReference {

    UnsafeRemoteReference(TeleVM vm, Address raw) {
        super(vm, raw);
    }

    @Override
    public ObjectStatus status() {
        return ObjectStatus.DEAD;
    }
}

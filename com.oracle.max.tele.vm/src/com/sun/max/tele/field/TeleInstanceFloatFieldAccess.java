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
import com.sun.max.vm.type.*;

/**
 * An accessor for reading object instance fields of type {@code float} from VM memory, specified by class and field name.
 */
public final class TeleInstanceFloatFieldAccess extends TeleInstanceFieldAccess {

    public TeleInstanceFloatFieldAccess(Class holder, String name) {
        super(holder, name, Kind.FLOAT);
    }

    /**
     * Reads an object instance field, presumed to be of type {@code float}, from VM memory.
     *
     * @return the value of the field in VM memory interpreted as a {@code float}
     */
    public float readFloat(RemoteReference reference) {
        return reference.readFloat(fieldActor().offset());
    }
}

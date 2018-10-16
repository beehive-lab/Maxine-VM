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
import com.sun.max.unsafe.*;
import com.sun.max.vm.type.*;

/**
 * An accessor for reading static word-sized fields as raw words from VM memory, specified by class and field name.
 */
public final class TeleStaticWordFieldAccess extends TeleStaticFieldAccess {

    public TeleStaticWordFieldAccess(Class holder, String name) {
        super(holder, name, Kind.WORD);
    }

    /**
     * Reads a static word-sized field from VM memory.
     *
     * @return the value of the field in VM memory as a raw {@link Word}
     */
    public Word readWord(MaxVM vm) {
        return staticTupleReference(vm).readWord(fieldActor().offset());
    }
}

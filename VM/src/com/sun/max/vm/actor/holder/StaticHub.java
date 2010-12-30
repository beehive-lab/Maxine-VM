/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;

/**
 * @author Bernd Mathiske
 */
public class StaticHub extends Hub {

    StaticHub(Size tupleSize, ClassActor classActor, TupleReferenceMap referenceMap, int vTableLength) {
        super(tupleSize, classActor, referenceMap, vTableLength);
    }

    /**
     * Static Hub.
     */
    StaticHub expand(TupleReferenceMap referenceMap, int rootId) {
        final StaticHub hub = (StaticHub) expand();
        referenceMap.copyIntoHub(hub);
        // static hubs have a very special i-table and m-table that make it appear
        // as if it extends java.lang.Object (rootId)
        hub.setWord(hub.iTableStartIndex, Address.fromInt(rootId));
        hub.setInt(hub.mTableStartIndex, hub.iTableStartIndex);
        return hub;
    }

    @Override
    public FieldActor findFieldActor(int offset) {
        return classActor.findStaticFieldActor(offset);
    }
}

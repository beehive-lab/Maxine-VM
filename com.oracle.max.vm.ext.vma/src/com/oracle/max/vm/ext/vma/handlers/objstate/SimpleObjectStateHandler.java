/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.objstate;

import java.util.concurrent.atomic.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.layout.xohm.*;
import com.sun.max.vm.reference.*;

/**
 * A simple implementation that just uses a monotonically increasing value for an object id,
 * and does not support {@link DeadObjectHandler dead object handling}.
 *
 * Ids for objects whose creation was not observed (unseen) are negative and descend from {@code -1}.
 * Zero is not a valid id; instead it denotes "no id assigned" or the null object.
 *
 */
public class SimpleObjectStateHandler extends ObjectStateHandler {
    private static final AtomicLong nextUnseenId = new AtomicLong(0);
    private static final AtomicLong nextId = new AtomicLong(0);

    @Override
    public long assignId(Object obj) {
        return assignId(Reference.fromJava(obj));
    }

    @Override
    public long assignId(Reference objRef) {
        long id = nextId.incrementAndGet();
        writeId(objRef, id);
        return id;
    }

    @Override
    public long assignUnseenId(Object obj) {
        long id = nextUnseenId.decrementAndGet();
        writeId(Reference.fromJava(obj), id);
        return id;
    }

    @Override
    public long readId(Object obj) {
        return obj == null ? 0 : XOhmGeneralLayout.Static.readXtra(Reference.fromJava(obj)).asAddress().toLong();
    }

    @INLINE
    void writeId(Reference objRef, long id) {
        XOhmGeneralLayout.Static.writeXtra(objRef, Address.fromLong(id));
    }

    @Override
    public void gc(DeadObjectHandler rt) {
    }

}

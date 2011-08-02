/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vm.ext.vma.runtime;

import java.util.BitSet;

import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.Address;
import com.sun.max.unsafe.Pointer;
import com.sun.max.vm.layout.xohm.XOhmGeneralLayout;
import com.sun.max.vm.reference.Reference;

/**
 * This implementation uses two bitsets for tracking objects. The {@link #idSet} records the id of a live object, an id
 * simply being an index into the set. The set has a bit set for an object that is currently live.
 *
 * The ids of dead objects can be reused, which an analysis tool must cope with.
 *
 * The {@link #gcSet} records whether an object that was live at the start of a gc is still live at the end.
 *
 * The bitsets are sized according to the property {@value #MAX_IDS_PROPERTY} and expected to be allocated in bootstrap
 * memory. (Immortal memory would also work).
 *
 * Access to the bitsets is synchronized.
 *
 * Ids for objects whose creation was not tracked (unseen) are negative and descend from {@value UNSEEN_ID_BASE}.
 * Zero is not a valid id; instead it denotes "no id assigned" or the null object.
 *
 * @author Mick Jordan
 *
 */
public class BitSetObjectStateHandler extends ObjectStateHandler {
    private static final String MAX_IDS_PROPERTY = "max.vma.maxids";
    private static final int DEFAULT_MAX_IDS = 16 * 1024 * 1024;

    private BitSet idSet;
    private BitSet gcSet;
    private int lowestFreeBit = 1;
    private long nextUnseenId;
    private static final long UNSEEN_ID_BASE = -1;

    private BitSetObjectStateHandler(int size) {
        idSet = new BitSet(size);
        gcSet = new BitSet(size);
        nextUnseenId = UNSEEN_ID_BASE;
    }

    public static BitSetObjectStateHandler create() {
        final String prop = System.getProperty(MAX_IDS_PROPERTY);
        return new BitSetObjectStateHandler(prop == null ? DEFAULT_MAX_IDS : Integer.parseInt(prop));
    }

    /**
     * Returns a new id.
     * @return
     */
    @Override
    public synchronized long assignId(Object obj) {
        return assignId(Reference.fromJava(obj));
    }

    @Override
    public synchronized long assignId(Reference objRef) {
        int id = idSet.nextClearBit(lowestFreeBit);
        lowestFreeBit = id + 1;
        idSet.set(id);
        writeId(objRef, id);
        return id;
    }

    @Override
    public synchronized long assignUnseenId(Object obj) {
        long id = nextUnseenId--;
        writeId(Reference.fromJava(obj), id);
        return id;
    }

    @Override
    @INLINE(override = true)
    public long readId(Object obj) {
        return obj == null ? 0 : XOhmGeneralLayout.Static.readXtra(Reference.fromJava(obj)).asAddress().toLong();
    }

    @INLINE(override = true)
    void writeId(Reference objRef, long id) {
        XOhmGeneralLayout.Static.writeXtra(objRef, Address.fromLong(id));
    }

    @INLINE(override = true)
    @Override
    public void incrementLifetime(Pointer cell) {
        long id = XOhmGeneralLayout.Static.readXtra(cell).asAddress().toLong();
        if (id > 0) {
            safeSet(gcSet, (int) id);
        }
    }

    private synchronized void safeSet(BitSet set, int id) {
        set.set(id);
    }

    @Override
    public synchronized void gc(RemovalTracker rt) {
        for (int id = idSet.nextSetBit(1); id >= 0; id = idSet.nextSetBit(id + 1)) {
            if (!gcSet.get(id)) {
                rt.removed(id);
                idSet.clear(id);
                if (id < lowestFreeBit) {
                    lowestFreeBit = id;
                }
            }
        }
        gcSet.clear();
    }

}

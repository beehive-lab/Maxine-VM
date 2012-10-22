/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.max.vm.ext.vma.handlers.objstate.bitset;

import java.util.BitSet;

import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.sun.max.annotate.INLINE;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.xohm.XOhmGeneralLayout;
import com.sun.max.vm.reference.Reference;

/**
 * Now defunct implementation that tracked the liveness of objects using bitsets and advice from the
 * GC implementation on whether an object survived a GC. Not appropriate for a generational GC, for example,
 * without a lot of work in the GC. Could be re-purposed to use weak references, at considerable overhead to
 * the GC implementation.
 *
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

    /**
     * Create the instance.
     * If being used in a dynamically loaded context, the state must be allocated in
     * immortal memory as it is accessed while a GC is in progress.
     */
    public static BitSetObjectStateHandler create() {
        final String prop = System.getProperty(MAX_IDS_PROPERTY);
        try {
            if (!MaxineVM.isHosted()) {
                Heap.enableImmortalMemoryAllocation();
            }
            return new BitSetObjectStateHandler(prop == null ? DEFAULT_MAX_IDS : Integer.parseInt(prop));
        } finally {
            if (!MaxineVM.isHosted()) {
                Heap.disableImmortalMemoryAllocation();
            }
        }
    }

    /**
     * Returns a new id.
     */
    @Override
    public synchronized ObjectID assignId(Object obj) {
        return assignId(Reference.fromJava(obj));
    }

    @Override
    public synchronized ObjectID assignId(Reference objRef) {
        int id = idSet.nextClearBit(lowestFreeBit);
        lowestFreeBit = id + 1;
        idSet.set(id);
        ObjectID objID = ObjectID.fromWord(Address.fromLong(id));
        writeId(objRef, objID);
        return objID;
    }

    @Override
    public synchronized ObjectID assignUnseenId(Object obj) {
        ObjectID objID = ObjectID.fromWord(Address.fromLong(nextUnseenId--));
        writeId(Reference.fromJava(obj), objID);
        return objID;
    }

    @Override
    @INLINE
    public ObjectID readId(Object obj) {
        Word id = obj == null ? Word.zero() : XOhmGeneralLayout.Static.readXtra(Reference.fromJava(obj));
        return ObjectID.fromWord(id);
    }

    @INLINE
    void writeId(Reference objRef, ObjectID id) {
        XOhmGeneralLayout.Static.writeXtra(objRef, id);
    }

    @INLINE
//    @Override
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
    public synchronized void gc(DeadObjectHandler rt) {
        for (int id = idSet.nextSetBit(1); id >= 0; id = idSet.nextSetBit(id + 1)) {
            if (!gcSet.get(id)) {
                rt.dead(ObjectID.fromWord(Address.fromLong(id)));
                idSet.clear(id);
                if (id < lowestFreeBit) {
                    lowestFreeBit = id;
                }
            }
        }
        gcSet.clear();
    }

    @Override
    public int readBit(Object obj, int n) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeBit(Object obj, int n, int value) {
        // TODO Auto-generated method stub

    }

}

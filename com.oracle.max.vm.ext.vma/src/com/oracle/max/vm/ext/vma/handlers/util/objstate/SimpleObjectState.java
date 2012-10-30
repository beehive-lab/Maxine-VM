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
package com.oracle.max.vm.ext.vma.handlers.util.objstate;

import java.util.concurrent.atomic.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.layout.xohm.*;
import com.sun.max.vm.reference.*;

/**
 * A simple implementation that just uses a monotonically increasing value for an object id,
 *
 * Ids for objects whose creation was not observed (unseen) are negative and descend from {@code -1}.
 * Zero is not a valid id; instead it denotes "no id assigned" or the null object.
 *
 * Bits 56-63 are used for the bitmask. Mask bits may be set before the id and vice versa.
 *
 */
public class SimpleObjectState extends IdBitSetObjectState {
    private static final AtomicLong nextUnseenId = new AtomicLong(0);
    private static final AtomicLong nextId = new AtomicLong(0);
    private static final int BITMASK_SHIFT = 56;
    private static final int BITMASK_MASK = 0xFF;
    private static final long IDMASK =       0xFFFFFFFFFFFFFFL;
    private static final long SIGNEXTEND = 0xFF00000000000000L;
    private static final long SIGNBIT = 1L << (BITMASK_SHIFT - 1);

    @Override
    public ObjectID assignId(Object obj) {
        return assignId(Reference.fromJava(obj));
    }

    @Override
    public ObjectID assignId(Reference objRef) {
        long idAndMask = readIdAndMaskAsLong(objRef);
        long objId = nextId.incrementAndGet();
        writeId(objRef, Address.fromLong(idAndMask | objId));
        return ObjectID.fromWord(Address.fromLong(objId));
    }

    @Override
    public ObjectID assignUnseenId(Object obj) {
        long idAndMask = readIdAndMaskAsLong(obj);
        long objId = nextUnseenId.decrementAndGet();
        // have to create a 56 bit negative number
        writeId(Reference.fromJava(obj), Address.fromLong(objId & IDMASK | idAndMask));
        return ObjectID.fromWord(Address.fromLong(objId));
    }

    @Override
    public ObjectID readId(Object obj) {
        if (obj == null) {
            return ObjectID.fromWord(Word.zero());
        }
        long id =  readIdAndMaskAsLong(obj) & IDMASK;
        // check negative
        if ((id & SIGNBIT) != 0) {
            id = id | SIGNEXTEND;
        }
        return ObjectID.fromWord(Address.fromLong(id));
    }

    @INLINE
    void writeId(Reference objRef, Word id) {
        XOhmGeneralLayout.Static.writeXtra(objRef, id);
    }

    @INLINE
    private long readIdAndMaskAsLong(Object obj) {
        return readState(obj).asAddress().toLong();
    }

    @INLINE
    private long readIdAndMaskAsLong(Reference objRef) {
        return readState(objRef).asAddress().toLong();
    }

    @Override
    public int readBit(Object obj, int n) {
        if (obj == null) {
            return 0;
        }
        int mask = 1 << n;
        long idAndMask = readIdAndMaskAsLong(obj);
        return (int) (idAndMask >> BITMASK_SHIFT) & mask;
    }

    @Override
    public void writeBit(Object obj, int n, int value) {
        if (obj == null) {
            return;
        }
        Reference objRef = Reference.fromJava(obj);
        long idAndMask = readIdAndMaskAsLong(objRef);
        long mask = 1 << n;
        if (value == 0) {
            mask = ~mask & 0xFF;
            idAndMask = idAndMask & (mask << BITMASK_SHIFT);
        } else {
            idAndMask = idAndMask | (mask << BITMASK_SHIFT);
        }

        writeId(objRef, Address.fromLong(idAndMask));
    }

    @Override
    public void writeID(Object obj, ObjectID id) {
        writeId(Reference.fromJava(obj), id);
    }

}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.*;

import com.sun.max.vm.reference.*;

/**
 */
public abstract class TeleReference extends Reference implements TeleObjectMemory {

    private long refOID = 0;

    protected TeleReference forwardedTeleRef = null;

    protected boolean collectedByGC = false;

    private TeleObjectMemory.State state = TeleObjectMemory.State.LIVE;

    protected TeleReference() {
    }

    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the tele VM, assigned lazily
     */
    public synchronized long makeOID() {
        if (refOID == 0) {
            refOID = nextOID.incrementAndGet();
        }
        return refOID;
    }

    private static final AtomicLong nextOID = new AtomicLong(1);

    public boolean isLocal() {
        return false;
    }

    public final void setForwardedTeleReference(TeleReference forwardedMutableTeleRef) {
        this.forwardedTeleRef = forwardedMutableTeleRef;
    }

    public final TeleReference getForwardedTeleRef() {
        if (forwardedTeleRef != null) {
            return forwardedTeleRef.getForwardedTeleRef();
        }
        return this;
    }

    public abstract TeleObjectMemory.State getTeleObjectMemoryState();

    public boolean isLive() {
        return getTeleObjectMemoryState() == TeleObjectMemory.State.LIVE;
    }

    public boolean isObsolete() {
        return getTeleObjectMemoryState() == TeleObjectMemory.State.OBSOLETE;
    }

    public boolean isDead() {
        return getTeleObjectMemoryState() == TeleObjectMemory.State.DEAD;
    }

    public static final TeleReference ZERO = new TeleReference() {

        @Override
        public TeleObjectMemory.State getTeleObjectMemoryState() {
            return TeleObjectMemory.State.DEAD;
        }

        @Override
        public String toString() {
            return "null";
        }

        @Override
        public boolean equals(Reference other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };
}

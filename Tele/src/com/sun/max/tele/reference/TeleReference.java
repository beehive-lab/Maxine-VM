/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele.reference;

import java.util.concurrent.atomic.*;

import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 * @author Hannes Payer
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

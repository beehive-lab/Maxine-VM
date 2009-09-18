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
package com.sun.max.tele.grip;

import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 * @author Hannes Payer
 */
public abstract class TeleGrip extends Grip {

    private long gripOID = 0;

    protected TeleGrip forwardedTeleGrip = null;

    protected boolean collectedByGC = false;

    public enum State {
        LIVE,
        OBSOLETE,
        DEAD;
    }

    private State state = State.LIVE;

    protected TeleGrip() {
    }

    private Reference reference;

    public synchronized Reference makeReference(TeleReferenceScheme teleReferenceScheme) {
        if (reference == null) {
            reference = teleReferenceScheme.createReference(this);
        }
        return reference;
    }

    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the tele VM, assigned lazily
     */
    public synchronized long makeOID() {
        if (gripOID == 0) {
            gripOID = getNextOID();
        }
        return gripOID;
    }


    private static long nextOID = 1;

    private static synchronized long getNextOID() {
        return nextOID++;
    }

    public boolean isLocal() {
        return false;
    }

    public final void setForwardedTeleGrip(TeleGrip forwardedMutableTeleGrip) {
        this.forwardedTeleGrip = forwardedMutableTeleGrip;
    }

    public final TeleGrip getForwardedTeleGrip() {
        if (forwardedTeleGrip != null) {
            return forwardedTeleGrip.getForwardedTeleGrip();
        }
        return this;
    }

    public abstract State getState();

    public static final TeleGrip ZERO = new TeleGrip() {

        @Override
        public State getState() {
            return State.DEAD;
        }

        @Override
        public Reference makeReference(TeleReferenceScheme teleReferenceScheme) {
            return TeleReference.ZERO;
        }

        @Override
        public String toString() {
            return "null";
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };
}

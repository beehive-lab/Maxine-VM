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

import com.sun.max.unsafe.*;

/**
 * Raw bits may change due to tele GC.
 *
 * @author Bernd Mathiske
 * @author Hannes Payer
 */
public final class MutableTeleGrip extends RemoteTeleGrip {

    private int index;
    private Address lastValidPointer = Address.zero();

    int index() {
        if (forwardedTeleGrip != null) {
            if (forwardedTeleGrip instanceof MutableTeleGrip) {
                final MutableTeleGrip mutableTeleGrip = (MutableTeleGrip) getForwardedTeleGrip();
                return mutableTeleGrip.index();
            }
        }
        return index;
    }

    @Override
    public State getState() {
        if (forwardedTeleGrip != null) {
            MutableTeleGrip forwardedTeleGrip = (MutableTeleGrip) getForwardedTeleGrip();
            if (forwardedTeleGrip.index() == -1) {
                return State.DEAD;
            }
            return State.OBSOLETE;
        }
        if (index == -1) {
            return State.DEAD;
        }
        return State.LIVE;
    }

    @Override
    public Address raw() {
        if (index == -1) {
            return lastValidPointer;
        }
        Address tmp = teleGripScheme().getRawGrip(this);
        if (!tmp.equals(Address.zero())) {
            lastValidPointer = tmp;
            return tmp;
        }
        index = -1;
        return lastValidPointer;
    }

    MutableTeleGrip(TeleGripScheme teleGripScheme, int index) {
        super(teleGripScheme);
        this.index = index;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MutableTeleGrip) {
            final MutableTeleGrip mutableTeleGrip = (MutableTeleGrip) other;
            return index == mutableTeleGrip.index;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public void finalize() throws Throwable {
        if (getState() == State.LIVE) {
            teleGripScheme().finalizeMutableTeleGrip(index);
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return "<" + index + ">";
    }
}

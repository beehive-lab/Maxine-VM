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

import com.sun.max.vm.grip.*;

/**
 * A local object wrapped into a {@link Grip} for tele interpreter use.
 *
 * @author Bernd Mathiske
 */
public class LocalTeleGrip extends TeleGrip {

    private final Object object;

    public Object object() {
        return object;
    }

    @Override
    public State getState() {
        return State.LIVE;
    }

    private final TeleGripScheme teleGripScheme;

    LocalTeleGrip(TeleGripScheme teleGripScheme, Object object) {
        this.teleGripScheme = teleGripScheme;
        this.object = object;
    }

    @Override
    protected void finalize() throws Throwable {
        teleGripScheme.disposeCanonicalLocalGrip(object);
        super.finalize();
    }

    @Override
    public String toString() {
        return object.toString();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LocalTeleGrip) {
            final LocalTeleGrip localTeleGrip = (LocalTeleGrip) other;
            return object == localTeleGrip.object();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(object);
    }
}

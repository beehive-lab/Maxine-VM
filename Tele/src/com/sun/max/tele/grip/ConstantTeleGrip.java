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
 * The raw bits do not change.
 * 
 * @author Bernd Mathiske
 */
public abstract class ConstantTeleGrip extends RemoteTeleGrip {

    private final Address _raw;

    @Override
    public Address raw() {
        return _raw;
    }

    protected ConstantTeleGrip(TeleGripScheme teleGripScheme, Address rawGrip) {
        super(teleGripScheme);
        _raw = rawGrip;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConstantTeleGrip) {
            final ConstantTeleGrip constantTeleGrip = (ConstantTeleGrip) other;
            return _raw.equals(constantTeleGrip._raw);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _raw.toInt();
    }

    @Override
    public String toString() {
        return raw().toString();
    }

}

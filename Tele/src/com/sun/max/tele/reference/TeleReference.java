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

import com.sun.max.tele.grip.*;
import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 */
public abstract class TeleReference extends Reference {

    private final TeleGrip _grip;

    public TeleGrip grip() {
        return _grip;
    }

    protected TeleReference(TeleGrip grip) {
        _grip = grip;
    }

    @Override
    public int hashCode() {
        return _grip.hashCode();
    }

    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the tele VM
     */
    public long makeOID() {
        return _grip.makeOID();
    }

    public boolean isLocal() {
        return _grip.isLocal();
    }

    public static final TeleReference ZERO = new TeleReference(TeleGrip.ZERO) {};

    @Override
    public String toString() {
        return _grip.toString();
    }

}

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
/*VCSID=e2a44af1-28b6-4828-a007-ea02a65d1b06*/
package com.sun.max.tele.grip;

import com.sun.max.tele.reference.*;
import com.sun.max.vm.grip.*;
import com.sun.max.vm.reference.*;

/**
 * @author Bernd Mathiske
 */
public abstract class TeleGrip extends Grip {

    private long _gripOID = 0;

    protected TeleGrip() {
    }

    private Reference _reference;

    public synchronized Reference makeReference(TeleReferenceScheme teleReferenceScheme) {
        if (_reference == null) {
            _reference = teleReferenceScheme.createReference(this);
        }
        return _reference;
    }

    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the tele VM, assigned lazily
     */
    public synchronized long makeOID() {
        if (_gripOID == 0) {
            _gripOID = getNextOID();
        }
        return _gripOID;
    }


    private static long _nextOID = 1;

    private static synchronized long getNextOID() {
        return _nextOID++;
    }

    public static final TeleGrip ZERO = new TeleGrip() {
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

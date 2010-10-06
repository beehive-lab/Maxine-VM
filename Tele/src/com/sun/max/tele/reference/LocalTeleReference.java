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

import com.sun.max.vm.reference.*;

/**
 * A local object wrapped into a {@link Reference} for tele interpreter use.
 *
 * @author Bernd Mathiske
 */
public class LocalTeleReference extends TeleReference {

    private final Object object;

    public Object object() {
        return object;
    }

    @Override
    public TeleObjectMemory.State getTeleObjectMemoryState() {
        return TeleObjectMemory.State.LIVE;
    }

    private final TeleReferenceScheme teleReferenceScheme;

    LocalTeleReference(TeleReferenceScheme teleReferenceScheme, Object object) {
        this.teleReferenceScheme = teleReferenceScheme;
        this.object = object;
    }

    @Override
    protected void finalize() throws Throwable {
        teleReferenceScheme.disposeCanonicalLocalReference(object);
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
        if (other instanceof LocalTeleReference) {
            final LocalTeleReference localTeleRef = (LocalTeleReference) other;
            return object == localTeleRef.object();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(object);
    }
}

/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.unsafe.*;

/**
 * Raw bits may change due to tele GC.
 *
 * @author Bernd Mathiske
 * @author Hannes Payer
 */
public final class MutableTeleReference extends RemoteTeleReference {

    private int index;
    private Address lastValidPointer = Address.zero();

    int index() {
        if (forwardedTeleRef != null) {
            if (forwardedTeleRef instanceof MutableTeleReference) {
                final MutableTeleReference mutableTeleRef = (MutableTeleReference) getForwardedTeleRef();
                return mutableTeleRef.index();
            }
        }
        return index;
    }

    @Override
    public TeleObjectMemory.State getTeleObjectMemoryState() {
        if (forwardedTeleRef != null) {
            MutableTeleReference forwardedTeleRef = (MutableTeleReference) getForwardedTeleRef();
            if (forwardedTeleRef.index() == -1) {
                return TeleObjectMemory.State.DEAD;
            }
            return TeleObjectMemory.State.OBSOLETE;
        }
        if (index == -1) {
            return TeleObjectMemory.State.DEAD;
        }
        return TeleObjectMemory.State.LIVE;
    }

    @Override
    public Address raw() {
        if (index == -1 || forwardedTeleRef != null) {
            return lastValidPointer;
        }
        Address tmp = teleReferenceScheme().getRawReference(this);
        if (!tmp.equals(Address.zero())) {
            lastValidPointer = tmp;
            return tmp;
        }
        index = -1;
        return lastValidPointer;
    }

    MutableTeleReference(TeleReferenceScheme teleReferenceScheme, int index) {
        super(teleReferenceScheme);
        this.index = index;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MutableTeleReference) {
            final MutableTeleReference mutableTeleRef = (MutableTeleReference) other;
            return index == mutableTeleRef.index;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public void finalize() throws Throwable {
        if (isLive()) {
            teleReferenceScheme().finalizeMutableTeleReference(index);
        }
        super.finalize();
    }

    @Override
    public String toString() {
        return "<" + index + ">";
    }
}

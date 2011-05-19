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

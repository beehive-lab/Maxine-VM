/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.tele.object.*;

/**
 * The status of an object in the VM, or more precisely the status of the region of memory
 * in which this object's state is or was stored.  A new object is by definition {@linkplain #LIVE "LIVE"}.
 * <p>
 * If the object has been collected by the VM, it is no longer a legitimate object, the
 * state of this instance becomes {@linkplain #DEAD "DEAD"}, and it does not change again.
 * <p>
 * If the object has been relocated by the VM, the memory referred to by this instance
 * is no longer a legitimate object, the state of this instance becomes {@linkplain #OBSOLETE "OBSOLETE"},
 * and it does not change again.
 * <p>
 * Once an object is no longer live, the  memory most recently allocated to it might be
 * reused by the VM.  For debugging purposes, however, a {@link TeleObject}
 * continues to refer to the abandoned memory as if it were an object.
 */
public enum ObjectMemoryStatus {

    /**
     * The region of memory is in a live allocation area and represents an object
     * that is reachable as of the most recent collection.
     */
    LIVE("Live", "The region of memory is in a live allocation area and represents an object that is reachable as of the most recent collection"),

    /**
     * The region of memory formerly represented an object that has been moved to another location.
     */
    OBSOLETE("Obsolete", "The region of memory formerly represented an object that has been moved to another location"),

    /**
     * The region of memory formerly represented an object that has been collected.
     */
    DEAD("Dead", "The region of memory formerly represented an object that has been collected");

    private final String label;
    private final String description;

    private ObjectMemoryStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    /**
     * Does the memory represent a live object in the VM?
     *
     * @return whether the object's memory in the VM is still live
     */
    public boolean isLive() {
        return this == LIVE;
    }

    /**
     * Does the memory contain a copy of an object in the VM
     * that has been abandoned after relocation by GC.
     *
     * @return whether the object's memory in the VM has been relocated elsewhere.
     */
    public boolean isObsolete() {
        return this == OBSOLETE;
    }

    /**
     * Does the memory represent the state of
     * an object in the VM that has been collected.
     *
     * @return whether the object in the VM has been collected.
     */
    public boolean isDead() {
        return this == DEAD;
    }

    public static final List<ObjectMemoryStatus> VALUES = Arrays.asList(values());

}

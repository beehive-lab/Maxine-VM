/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.object;

import java.util.*;

import com.sun.max.tele.reference.*;
import com.sun.max.vm.heap.*;

/**
 * The status of an object represented in VM memory, from the perspective of a remove vantage that
 * may not have complete information about an object or even, in the case of GC, may have a slightly
 * different view of the object's status at certain times than does the VM itself.
 * <p>
 * There are two fixed states:  {@link #LIVE} and {@link #DEAD}.  The rest are considered <em>Quasi</em>
 * states: regions of VM memory formatted as objects (or nearly so) that
 * would be interesting and useful to view as objects, but which are not live objects from the
 * perspective of the VM.
 * <ul>
 * <li> {@link #LIVE}: Determined to be reachable as of the most recent collection, or <em>presumed</em>
 * to be reachable during certain GC phases when its reachability has not yet been determined.</li>
 * <li> {@link #DEAD}: Unreachable memory, usually at a site that formerly held a live or
 * quasi-object but which no longer does; no assumptions about memory may be made</li>
 * </ul>
 * <p>
 * {@linkplain RemoteReference Remote references} and thus {@linkplain TeleObject remote objects} can
 * change status in response to both changes by memory managers in the VM and by discovery of additional
 * information about the status of VM memory management.
 */
public enum RemoteObjectStatus {

    /**
     * The region of memory is either in a live allocation area (representing an object
     * that was determined to be reachable as of the most recent collection) or is in a region
     * where GC is underway and for which reachability has not yet been determined.
     */
    LIVE("Live", "Determined to be reachable as of the most recent collection"),

    @Deprecated
    UNKNOWN("Unknown", "During liveness analysis:  formerly live, not yet determined reachable"),

    /**
     * The region of memory formerly represented an object that has been collected.
     */
    DEAD("Dead", "The region of memory formerly represented an object that has been collected"),

    /**
     *
     */
    FORWARDER("Forwarder", "Old copy of a forwarded object, for the duration of the GC analyzing phase");

    private final String label;
    private final String description;

    private RemoteObjectStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    /**
     * Does the memory represent an object that is currently assumed to be reachable?
     *
     * @return {@code this == } {@link #LIVE}.
     */
    public boolean isLive() {
        return this == LIVE;
    }

    /**
     * Does the memory represent interesting information, represented in VM object format, that is not a live object?
     *
     * @return {@code this != } {@link #LIVE} {@code && this != } {@link #DEAD}
     */
    public boolean isQuasi() {
        return this != LIVE && this != DEAD;
    }

    /**
     * Does the memory represent an object that we treat provisionally as reachable: either {@link #LIVE} or
     * {@link #UNKNOWN}.
     *
     * @return {@code this != } {@link #DEAD}.
     */
    public boolean isNotDead() {
        return this != DEAD;
    }

    /**
     * Has the object represented by the memory been determined to be unreachable?
     *
     * @return {@code this == } {@link #DEAD}.
     */
    public boolean isDead() {
        return this == DEAD;
    }

    /**
     * Does the memory hold the old copy of a forwarded object?
     *
     * @return this == FORWARDER; always {@code false} when GC not {@linkplain HeapPhase#ANALYZING ANALYZING}.
     */
    public boolean isForwarder() {
        return this == FORWARDER;
    }

    public static final List<RemoteObjectStatus> VALUES = Arrays.asList(values());

}

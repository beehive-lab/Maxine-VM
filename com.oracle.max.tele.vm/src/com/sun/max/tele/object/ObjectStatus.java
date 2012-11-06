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
 * The status of an object represented in VM memory, from a remote perspective that may not have complete information
 * about an object. Moreover, in some cases, notably during GC, this remote perspective may have a slightly different
 * view of the object's status at certain times than does the VM itself.
 * <p>
 * There are two distinguished states ({@link #LIVE} and {@link #DEAD}), based on the conventional notion of object
 * reachability in the VM's heap. The other states are considered <em>Quasi</em> states; they describe regions of VM
 * memory formatted as objects (or nearly so) that would be interesting and useful to view as objects, but which are not
 * live objects from the perspective of the VM.
 *
 * <ul>
 * <li> {@link #LIVE}: Determined to be reachable as of the most recent collection, or <em>presumed</em> to be reachable
 * during certain GC phases when its reachability has not yet been determined.</li>
 * <li> {@link #FORWARDER}: A <em>Quasi-object</em> that represents the old copy of an object that has been forwarded by
 * a relocating collector.  Its lifetime starts when the new copy is created and ends when the GC
 * {@linkplain HeapPhase#ANALYZING analyzing} phase is complete.</li>
 * <li> {@link UNREACHABLE}: A <em>Quasi-object</em> that represents a formerly {@linkplain #LIVE live} object during
 * a GC {@linkplain HeapPhase#RECLAIMING reclaiming} phase, when that object has been determined by the immediately
 * preceding {@linkplain HeapPhase#ANALYZING analyzing phase} to be unreachable.
 * <li> {@link #FREE}: A <em>Quasi-object</em> that is formatted by the GC to explicitly span a segment of unallocated
 * memory that is available for allocation.</li>
 * <li> {@link #DARK}: A <em>Quasi-object</em> that is formatted by the GC to explicitly span a segment of unallocated
 * memory that is <em>not</em> available for allocation.</li>
 * <li> {@link #DEAD}: Unreachable memory, possibly at a site that formerly held a live or quasi-object but which no
 * longer does; no assumptions about memory may be made</li>
 * </ul>
 * <p>
 * {@linkplain RemoteReference Remote references} and thus {@linkplain TeleObject remote objects} can change states in
 * response to both changes by memory managers in the VM and by discovery of additional information about the status of
 * VM memory management.
 */
public enum ObjectStatus {

    /**
     * The region of memory is either in a live allocation area (representing an object
     * that was determined to be reachable as of the most recent collection) or is in a region
     * where GC is underway and for which reachability has not yet been determined.
     */
    LIVE("Live", "Determined to be reachable as of the most recent collection"),

    /**
     * A region of memory that formerly held a live object, but which has been copied by a relocating collector.
     * The lifetime of this status begins when the new copy is made, and a forwarding pointer inserted into
     * the old copy and ends when the GC's current {@linkplain HeapPhase#ANALYZING} is complete.
     */
    FORWARDER("Forwarder", "Old copy of a forwarded object, for the duration of the GC analyzing phase"),

    /**
     * A region of memory that formerly held a live object, but which is now known (to the inspector, at least, if not yet
     * to the GC implementation) to be unreachable and has not yet been reclaimed.
     */
    UNREACHABLE("Unreachable", "A formerly live object known to be unreachable but not reclaimed"),

    /**
     * A region of memory corresponding to an element of the GC's free space list.  It is formatted as an
     * object, even though it is never reachable as an object.
     */
    FREE("Free Space", "A GC-formatted quasi- object that spans a chunk of memory available for allocation"),

    /**
     * A region of unallocated memory that the GC chooses not to make available for allocation and so is not
     * on the GC's free space list.  It is formatted as an object, even though it is never reachable as an object.
     */
    DARK("Dark Matter", "A GC-formatted quasi- object that spans a chunk of unallocated memory that is not available for allocation"),

    /**
     * The region of memory formerly represented an object or quasi-object that has been released/collected.
     */
    DEAD("Dead", "The region of memory formerly represented an object that has been collected");

    private final String label;
    private final String description;

    private ObjectStatus(String label, String description) {
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
     * @return {@code this == } {@link #LIVE}
     */
    public boolean isLive() {
        return this == LIVE;
    }

    /**
     * Has the object represented by the memory been determined to be unreachable?
     *
     * @return {@code this == } {@link #DEAD}
     */
    public boolean isDead() {
        return this == DEAD;
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
     * Does the memory represent either a {@link #LIVE} or <em>quasi-object</em> that can be usefully viewed as
     * if it were an object.
     *
     * @return {@code this != } {@link #DEAD}
     */
    public boolean isNotDead() {
        return this != DEAD;
    }

    /**
     * Does the memory hold a <em>quasi-object</em> representing the old copy of an object being forwarded during the current GC cycle?
     *
     * @return {@code this ==} {@link #FORWARDER}; can only be {@code true} while the GC is {@linkplain HeapPhase#ANALYZING analyzing}.
     */
    public boolean isForwarder() {
        return this == FORWARDER;
    }

    /**
     * Does the memory hold a <em>quasi-object</em> representing an object that is known to be unreachable, but which has not been reclaimed.
     *
     * @return {@code this ==} {@link #UNREACHABLE}; can only be {@code true} when the GC is {@linkplain HeapPhase#RECLAIMING reclaiming}.
     */
    public boolean isUnreachable() {
        return this == UNREACHABLE;
    }

    /**
     * Does the memory hold a <em>quasi-object</em> formatted by the GC to represent free memory available for allocation?
     *
     * @return {@code this ==} {@link #FREE}
     */
    public boolean isFree() {
        return this == FREE;
    }

    /**
     * Does the memory hold a <em>quasi-object</em> formatted by the GC to represent free memory <em>not</em> available for allocation?
     *
     * @return {@code this ==} {@link #DARK}
     */
    public boolean isDark() {
        return this == DARK;
    }

    public static final List<ObjectStatus> VALUES = Arrays.asList(values());

}

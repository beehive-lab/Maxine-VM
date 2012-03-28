/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * Special implementations of VM {@link Reference}s to objects that permit reuse of many
 * VM classes and schemas in the context of non-local VM heap inspection.
 *
 * @see VmReferenceManager
 */
public abstract class RemoteReference extends Reference {

    private final TeleVM vm;

    private long refOID = 0;

    private static final AtomicLong nextOID = new AtomicLong(1);

    protected RemoteReference(TeleVM vm) {
        this.vm = vm;
    }

    public TeleVM vm() {
        return vm;
    }
    /**
     * @return a non-zero integer uniquely identifying the referred-to object in the VM, assigned lazily
     */
    public synchronized long makeOID() {
        if (refOID == 0) {
            refOID = nextOID.incrementAndGet();
        }
        return refOID;
    }

    /**
     * @return the status of the memory to which this instance refers: {@linkplain ObjectStatus#LIVE LIVE},
     * {@linkplain ObjectStatus#DEAD DEAD}, or (only possible when heap is {@linkplain HeapPhase#ANALYZING ANALYZING})
     * {@linkplain ObjectStatus#UNKNOWN UNKNOWN}.
     */
    public abstract ObjectStatus status();

    /**
     * Gets the absolute location of the object's <em>origin</em> in VM memory. This may or may not be the same as the
     * beginning of the object's storage, depending on the {@link LayoutScheme} being used.
     * <p>
     * When an object is <em>forwarded</em> during the {@linkplain HeapPhase#ANALYZING ANALYZING} phase of a GC, this is
     * the location of the new copy. The location of the old copy is available during that phase via the
     * {@link #forwardedFrom()} method.
     * <p>
     * Returns the last valid origin when the object has become <em>unreachable</em> and its status is
     * {@linkplain ObjectStatus#DEAD DEAD}.
     *
     * @return the VM memory location of the object
     */
    public abstract Address origin();

    /**
     * Returns {@code true} during the period when an object is being moved to a new location in VM memory by a
     * relocating GC, only possible when the heap phase is {@linkplain HeapPhase#ANALYZING ANALYZING}.  This
     * state can be characterized as the object existing in two places simultaneously, even if complete
     * information about its two locations has not been discovered.
     * <p>
     * During such a period:
     * <ul>
     * <li> {@link #origin()} returns the location of the <em>new</em> copy of the object;</li>
     * <li> {@link #forwardedFrom()}, if known, returns the <em>old</em> copy of the object, or
     * {@code null} if not yet discovered; and </li>
     * <li> {@link #status()} returns {@linkplain ObjectStatus#LIVE LIVE}.</li>
     * </ul>
     *
     * @return {@code true} iff the object has been copied to a new location during a heap's
     *         {@linkplain HeapPhase#ANALYZING ANALYZING} phase; always {@code false} during other heap phases
     * @see #forwardedFrom()
     */
    public abstract boolean isForwarded();

    /**
     * Returns the location of the <em>old</em> copy of the object in VM memory during any period of time when the
     * object is being <em>forwarded</em>. This is available <em>only</em> when the heap phase is
     * {@linkplain HeapPhase#ANALYZING ANALYZING} <em>and</em> the object has been copied to a new location. In all
     * other situations returns {@link Address#zero()}.
     *
     * @return the former address of an object while it is being <em>forwarded</em>.
     * @see #isForwarded()
     */
    public abstract Address forwardedFrom();

    /**
     * Generates a string describing the status of the object in VM memory with respect to memory management, designed
     * to provide useful information to a person:  information that the Inspector can't already deduce from the standard
     * interfaces. For example, the Inspector can identify the region into which the reference points and the basic
     * status of the object's {@linkplain ObjectStatus status}.
     *
     * @return an optional string with information useful to a person, null if unavailable.
     */
    public abstract String gcDescription();

    /**
     * @return is the reference a special temporary {@linkplain ObjectStatus#DEAD DEAD} reference that should not be allowed to
     *         persist past any VM execution?
     */
    public boolean isTemporary() {
        return false;
    }

    /**
     * @return is the reference a special temporary {@linkplain ObjectStatus#LIVE LIVE} reference that appears to refer to an
     *         object that is not in any known VM memory region?
     */
    public boolean isProvisional() {
        return false;
    }

    /**
     * @return is the reference a local value dressed up to look like a remote reference
     */
    public boolean isLocal() {
        return false;
    }

}

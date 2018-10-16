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
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Special implementations of VM {@link Reference}s to objects that permit reuse of many VM classes and schemas in the
 * context of non-local VM heap inspection.
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
     * @return the status of the object representation in memory to which this instance refers:
     *         {@linkplain ObjectStatus#LIVE LIVE}, {@linkplain ObjectStatus#DEAD DEAD}, or one of the {@linkplain ObjectStatus#isQuasi() quasi states}.
     */
    public abstract ObjectStatus status();

    /**
     * Gets the previous status of the object; this is interesting mainly when the the current {@link #status()} is {@linkplain ObjectStatus#DEAD DEAD}.
     * @return the status of the object representation prior to the one reported by {@link #status()}, {@code null} if there was none.
     */
    public abstract ObjectStatus priorStatus();

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
     * Reads the {@link Hub} word from the object's header field in VM memory.
     *
     * @return contents of the object's hub field
     */
    public Word readHubAsWord() {
        return vm.referenceScheme().readHubAsWord(this);
    }

    /**
     * Reads from this object in VM memory the reference to the object's {@link Hub}, traversing a forwarder if
     * necessary to locate the live Hub; returns {@link RemoteReference#zero()} if the hub field in the object header
     * does not point at a live object.
     *
     * @return the value of the object's hub field in VM memory interpreted as a reference, unless it refers to a forwarder
     *         in which case the reference returned refers to the destination of the forwarder.
     */
    public final RemoteReference readHubAsRemoteReference() {
        return vm.referenceScheme().readHubAsRemoteReference(this);
    }

    /**
     * Reads from this tuple or hybrid object in VM memory the reference presumed to be held in a field at the specified
     * offset, traversing a forwarder if necessary; returns {@link RemoteReference#zero()} if the field's value does not
     * point at a live object. No checking is performed to ensure that the offset identifies a reference field, or even
     * whether the offset is contained within the object.
     *
     * @param fieldActor field description, including the offset in bytes from the presumed object's origin at which to read the field
     * @return the value of the instance field in VM memory interpreted as a reference, unless it refers to a forwarder
     *         in which case the reference returned refers to the destination of the forwarder.
     */
    public final RemoteReference readFieldAsRemoteReference(FieldActor fieldActor) {
        return vm.referenceScheme().readFieldAsRemoteReference(this, fieldActor);
    }

    /**
     * Reads from this array in VM memory the reference presumed to be held in an at the specified index, traversing a
     * forwarder if necessary; returns {@link RemoteReference#zero()} if the element's value does not point at a live
     * object. No checking is performed to ensure that this is an array or that it hold references or that the index is
     * within the array bounds.
     *
     * @param index the index into the presumed array of references at which to read
     * @return the value of the array element in VM memory interpreted as a reference, unless it refers to a forwarder
     *         in which case the reference returned refers to the destination of the forwarder.
     */
    public final RemoteReference readArrayAsRemoteReference(int index) {
        return vm.referenceScheme().readArrayAsRemoteReference(this, index);
    }

    /**
     * Read a VM array element as a generic boxed value. Does not check that there is a valid array of the specified
     * kind at the specified location, or whether the index is in bounds.
     *
     * @param kind identifies one of the basic VM value types
     * @param index offset into the array
     * @return a generic boxed value based on the contents of the array element in VM memory.
     */
    public final Value readArrayAsValue(Kind kind, int index) {
        return vm.referenceScheme().readArrayAsValue(kind, this, index);
    }

    /**
     * Returns the location of the <em>old</em> copy of the object in VM memory during any period of time when the
     * object is being <em>forwarded</em>. This is available <em>only</em> when the heap phase is
     * {@linkplain HeapPhase#ANALYZING ANALYZING} <em>and</em> the object has been copied to a new location. In all
     * other situations returns {@link Address#zero()}.
     *
     * @return the former address of an object while it is being <em>forwarded</em>.
     */
    public abstract Address forwardedFrom();

    /**
     * Returns the location of the <em>new</em> copy of the object in VM memory during any period of time when the
     * object is being <em>forwarded</em> and a new live copy created elsewhere. This can happen <em>only</em> when the
     * heap phase is {@linkplain HeapPhase#ANALYZING ANALYZING} <em>and</em> the status is
     * {@link ObjectStatus#FORWARDER}. In all other situations returns {@link Address#zero()}.
     *
     * @return the former address of an object while it is being <em>forwarded</em>.
     */
    public abstract Address forwardedTo();

    /**
     * Returns {@code this} unless the reference status is {@link ObjectStatus#FORWARDER}, in which case a reference is
     * returned to the new, live copy created elsewhere. This can happen <em>only</em> when the heap phase is
     * {@linkplain HeapPhase#ANALYZING ANALYZING}.
     *
     * @return a reference to the actual location of the object, possibly following a forwarder to a new copy.
     */
    final public RemoteReference followIfForwarded() {
        if (status().isForwarder()) {
            return vm.referenceManager().makeReference(forwardedTo());
        }
        return this;
    }

    /**
     * Generates a string describing the status of the object in VM memory with respect to memory management, designed
     * to provide useful information to a person: information that the Inspector can't already deduce from the standard
     * interfaces. For example, the Inspector can identify the region into which the reference points and the basic
     * status of the object's {@linkplain ObjectStatus status}.
     *
     * @return an optional string with information useful to a person, null if unavailable.
     */
    public abstract String gcDescription();

    /**
     * @return is this reference a special temporary {@linkplain ObjectStatus#DEAD DEAD} reference that should not be
     *         allowed to persist past any VM execution?
     */
    public boolean isTemporary() {
        return false;
    }

    /**
     * @return is this reference a special temporary {@linkplain ObjectStatus#LIVE LIVE} reference that appears to refer
     *         to an object that is not in any known VM memory region?
     */
    public boolean isProvisional() {
        return false;
    }

    /**
     * @return is this reference a local value dressed up to look like a remote reference
     */
    public boolean isLocal() {
        return false;
    }

}

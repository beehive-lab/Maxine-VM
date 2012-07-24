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

import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.direct.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * The singleton manager for instances of {@link Reference} that point (or pretend to point) at
 * objects in the VM.
 * <p>
 * This is a work in progress; part of an evolution toward modeling correctly the
 * generality of object representations in the VM.
 * <p>
 * A <strong>raw reference</strong> is an {@link Address} in VM memory where the object is currently
 * located.  However, the location may be subject to change by GC, so the raw reference may change over time.
 * <p>
 * References are intended to be canonical, i.e. refer to only one object.  However, in the course of inspection
 * duplicates may appear.  These are resolved at the conclusion of each GC.
 */
public final class VmReferenceManager extends AbstractVmHolder {

    private static final int TRACE_VALUE = 1;

    private static VmReferenceManager vmReferenceManager;

    public static VmReferenceManager make(TeleVM vm, RemoteReferenceScheme referenceScheme) {
        if (vmReferenceManager == null) {
            vmReferenceManager = new VmReferenceManager(vm, referenceScheme);
        }
        return vmReferenceManager;
    }

    private final RemoteReferenceScheme referenceScheme;

    private VmReferenceManager(TeleVM vm, RemoteReferenceScheme referenceScheme) {
        super(vm);
        this.referenceScheme = referenceScheme;
        referenceScheme.setContext(vm);
    }

    /**
     * Checks that a {@link RemoteReference} points to a live heap object in the VM;
     * throws an unchecked exception if not.  This is a low-level method
     * that uses a debugging tag or (if no tags in image) a heuristic; it does
     * not require access to the {@link VmClassAccess}.
     *
     * @param reference memory location in the VM
     * @throws InvalidReferenceException when the location does <strong>not</strong> point
     * at a valid heap object.
     */
    public void checkReference(RemoteReference reference) throws InvalidReferenceException {
        if (!objects().objectStatusAt(reference.toOrigin()).isLive()) {
            throw new InvalidReferenceException(reference);
        }
    }

    /**
     * Gets the location of an object's origin in VM memory.
     *
     * @param reference a remote reference to a VM object
     * @return a VM memory location that is the object's origin;
     * {@linkplain Address#zero()} if the reference is {@linkplain ObjectStatus#DEAD DEAD}.
     */
    public Address toOrigin(RemoteReference reference) {
        return referenceScheme.toOrigin(reference);
    }

   /**
     * Gets a non-canonical instance of {@link RemoteReference} that represents the {@code null} remote object reference.
     *
     * @return the default instance of a zero reference
     */
    public RemoteReference zeroReference() {
        return (RemoteReference) referenceScheme.zero();
    }

//    /**
//     * Returns some kind of reference associated with the given raw reference in the VM, depending
//     * on what is known about the address.
//     * <ol>
//     * <li>If a canonical reference pointing at that location already exists, then returns it.</li>
//     * <li>If the address is the valid origin of an object in a <strong>non-collected</strong> heap region, for
//     * example the boot heap or an immortal heap, then return a new reference that is canonical,
//     * but which is not tracked for possible GC relocation.</li>
//     * <li>If the address is the valid origin of a live object in a dynamic heap region, then return
//     * a new reference that is canonical and which is tracked for possible GC relocation.</li>
//     * <li>If the address is the valid origin of an object in a dynamic heap region, but the object
//     * is known <strong>not</strong> to be live, then return an unsafe, temporary reference that
//     * wraps the address.</li>
//     * <li>If the address does not point an an object origin,  then return an unsafe, temporary reference that
//     * wraps the address.</li>
//     * </ol>
//     *
//     * @param address a memory location in VM memory
//     * @return a special kind of {@link Reference} implementation that encapsulates a remote
//     * location in VM memory, allowing the reuse of much VM code that deals with references.
//     */


    /**
     * Creates a specialized instance of the VM's {@link Reference} class that can refer to live objects
     * remotely in the VM.  Each instance is specialized for the kind of object management that takes
     * place in the memory region that contains the specified location, and in the case of managed
     * regions, the {@link RemoteReference} tracks the object, just as in the VM itself.
     *
     * @param origin a location in VM Memory
     * @return a reference to a live VM object, the zero {@link RemoteReference} if the specified location is {@link Address#zero()}
     * or there is no live object at the location
     */
    public RemoteReference makeReference(Address origin) {
        if (origin.isZero()) {
            return zeroReference();
        }
        vm().lock();
        try {
            final MaxEntityMemoryRegion<?> maxMemoryRegion = vm().addressSpace().find(origin);
            if (maxMemoryRegion != null && maxMemoryRegion.owner() instanceof VmObjectHoldingRegion<?>) {
                // In an object-holding region
                final VmObjectHoldingRegion<?> objectHoldingRegion = (VmObjectHoldingRegion<?>) maxMemoryRegion.owner();
                final RemoteReference liveObjectReference = objectHoldingRegion.objectReferenceManager().makeReference(origin);
                if (liveObjectReference != null) {
                    // A valid origin
                    return liveObjectReference;
                } else {
                    // In an object holding region, but not an origin
                    return referenceScheme.makeZeroReference("Null ref: not a valid origin in " + objectHoldingRegion.entityName(), origin);
                }
            } else if (maxMemoryRegion != null) {
                // In a region that isn't supposed to hold objects
                return referenceScheme.makeZeroReference("Null ref: address in non-object holding region " + maxMemoryRegion.owner().entityName(), origin);
            }
            // Not in any memory region we know about
            if (vm().isAttaching() && objects().isPlausibleOriginUnsafe(origin)) {
                return new ProvisionalRemoteReference(vm(), origin);
            }
            return referenceScheme.makeZeroReference("Null ref: address in no known memory region ", origin);
        } finally {
            vm().unlock();
        }
    }

    /**
     * Creates a specialized instance of the VM's {@link Reference} class that can refer to <em>quasi</em> objects
     * remotely in the VM.  Each instance is specialized for the kind of object management that takes
     * place in the memory region that contains the specified location.
     *
     * @param origin a location in VM Memory
     * @return a reference to a <em>quasi</em> VM object, the zero {@link RemoteReference} if the specified location is {@link Address#zero()}
     * or there is no quasi object at the location
     */
    public RemoteReference makeQuasiReference(Address origin) {
        if (origin.isZero()) {
            return zeroReference();
        }
        vm().lock();
        try {
            final MaxEntityMemoryRegion<?> maxMemoryRegion = vm().addressSpace().find(origin);
            if (maxMemoryRegion != null && maxMemoryRegion.owner() instanceof VmObjectHoldingRegion<?>) {
                // In an object-holding region
                final VmObjectHoldingRegion<?> objectHoldingRegion = (VmObjectHoldingRegion<?>) maxMemoryRegion.owner();
                final RemoteReference quasiObjectReference = objectHoldingRegion.objectReferenceManager().makeQuasiReference(origin);
                if (quasiObjectReference != null) {
                    // Origin of a quasi object
                    return quasiObjectReference;
                } else {
                    // In an object holding region, but not an origin of a quasi object
                    return referenceScheme.makeZeroReference("Null ref: not a quasi object origin in " + objectHoldingRegion.entityName(), origin);
                }
            } else if (maxMemoryRegion != null) {
                // In a region that isn't supposed to hold objects
                return referenceScheme.makeZeroReference("Null ref: address in non-object holding region " + maxMemoryRegion.owner().entityName(), origin);
            }
            // Not in any memory region we know about
            if (vm().isAttaching() && objects().isPlausibleOriginUnsafe(origin)) {
                return new ProvisionalRemoteReference(vm(), origin);
            }
            return referenceScheme.makeZeroReference("Null ref: address in no known memory region ", origin);
        } finally {
            vm().unlock();
        }
    }

    /**
     * Create a remote instance of {@link Reference} whose origin is at a given address, but without any checking that a
     * valid object is at that address and without any support for possible relocation.
     * <p>
     * <strong>Unsafe:</strong> These are not canonical and should only be used for temporary, low level access to
     * object state. They should not be retained across VM execution.
     * <p>
     * The object status is permanently {@link ObjectStatus#DEAD}.
     *
     * @param origin a constant location in VM memory about which almost nothing is guaranteed
     * @return the address wrapped as a remote object reference for temporary use
     */
    public RemoteReference makeTemporaryRemoteReference(Address origin) {
        return new TemporaryRemoteReference(vm(), origin);
    }

    /**
     * An unsafe {@link Reference} intended to wrap a fixed address that
     * appears be the origin of a VM object, but which is not in any known
     * memory region.  This is intended to be used only in transient situations,
     * where (for example during an attach) objects may be discovered before
     * meta-information about the region that contains them has been discovered.
     * <p>
     * Memory status is permanently {@link ObjectStatus#LIVE}.
     *
     * @param origin a location in an unknown region of VM memory where an object appears to be stored
     * @return the address wrapped as a reference for temporary use.
     */
    public RemoteReference makeUnknownRemoteReference(Address origin) {
        return new ProvisionalRemoteReference(vm(), origin);
    }

    /**
     * Boxes the remote flavor of reference as a {@link ReferenceValue}.
     *
     * @param reference
     * @return a boxed remote reference
     */
    public ReferenceValue createReferenceValue(RemoteReference reference) {
        return TeleReferenceValue.from(vm(), reference);
    }

    /**
     * A constant reference intended for use when performing some computation on
     * an {@link Address} that is implemented by re-using reference-based VM code.
     *
     * @see VmReferenceManager#makeTemporaryRemoteReference(Address)
     */
    private static final class TemporaryRemoteReference extends ConstantRemoteReference {

        TemporaryRemoteReference(TeleVM vm, Address origin) {
            super(vm, origin);
        }

        @Override
        public boolean isTemporary() {
            return true;
        }

        @Override
        public ObjectStatus status() {
            return ObjectStatus.DEAD;
        }
    }

    /**
     * A constant reference intended to represent what appears to be a legitimate object but whose enclosing memory
     * region and manger are so far unknown.
     *
     * @see VmReferenceManager#makeUnknownRemoteReference(Address)
     */
    private static final class ProvisionalRemoteReference extends ConstantRemoteReference {

        ProvisionalRemoteReference(TeleVM vm, Address origin) {
            super(vm, origin);
        }

        @Override
        public boolean isProvisional() {
            return true;
        }

        @Override
        public ObjectStatus status() {
            return ObjectStatus.LIVE;
        }
    }




}

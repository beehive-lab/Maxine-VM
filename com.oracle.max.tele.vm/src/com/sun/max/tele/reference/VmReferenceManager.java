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

import java.lang.ref.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.direct.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.tele.value.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.Reference;
import com.sun.max.vm.reference.hosted.*;
import com.sun.max.vm.value.*;

// TODO (mlvdv) this should be eliminated, cleaned up and possibly folded into VMObjectAccess.
// TODO (mlvdv) as of October 2011, only references to the dynamic heap are handled by the old mechanism,
// which is specialized for the semispace GC.  That mechanism should first be folded into VmHeapAccess
// and then encapsulated so that it is only used when the VM is configured with that collector.
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
 * Each ordinary reference is represented as a unique index into a root table, a mirror of such a table, in the VM.  The
 * table holds the current raw reference (address), and it is updated by the GC at the end of each collection.
 * <p>
 * References are intended to be canonical, i.e. refer to only one object.  However, in the course of inspection
 * duplicates may appear.  These are resolved at the conclusion of each GC.
 */
public final class VmReferenceManager extends AbstractVmHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private static VmReferenceManager vmReferenceManager;

    public static VmReferenceManager make(TeleVM vm, RemoteReferenceScheme referenceScheme) {
        if (vmReferenceManager == null) {
            vmReferenceManager = new VmReferenceManager(vm, referenceScheme);
        }
        return vmReferenceManager;
    }

    private long lastUpdateEpoch = -1L;

    private final RemoteReferenceScheme referenceScheme;

    private final TeleReference zeroReference;

    private TeleRoots teleRoots;

    protected LocalTeleReferenceManager localTeleReferenceManager;

    private VmReferenceManager(TeleVM vm, RemoteReferenceScheme referenceScheme) {
        super(vm);
        this.referenceScheme = referenceScheme;
        referenceScheme.setContext(vm);
        this.teleRoots = new TeleRoots(vm, this);
        this.localTeleReferenceManager = new LocalTeleReferenceManager(vm);
        this.zeroReference = new TeleReference(vm) {

            @Override
            public ObjectMemoryStatus memoryStatus() {
                return ObjectMemoryStatus.DEAD;
            }

            @Override
            public String toString() {
                return "null";
            }

            @Override
            public boolean equals(Reference other) {
                return this == other;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };
    }

    /**
     * Checks that a {@link Reference} points to a heap object in the VM;
     * throws an unchecked exception if not.  This is a low-level method
     * that uses a debugging tag or (if no tags in image) a heuristic; it does
     * not require access to the {@link VmClassAccess}.
     *
     * @param reference memory location in the VM
     * @throws InvalidReferenceException when the location does <strong>not</strong> point
     * at a valid heap object.
     */
    public void checkReference(Reference reference) throws InvalidReferenceException {
        if (!objects().isValidOrigin(reference.toOrigin())) {
            throw new InvalidReferenceException(reference);
        }
    }

    /**
     * Gets the location of an object's origin in VM memory.
     *
     * @param reference a remote reference to a VM object
     * @return a VM memory location that is the object's origin.
     */
    public Address toOrigin(Reference reference) {
        return referenceScheme.toOrigin(reference);
    }

    /**
     * Create a remote instance of {@link Reference} whose origin is at a given address,
     * but without any checking that a valid object is at that address and without any
     * support for possible relocation.
     * <p>
     * <strong>Unsafe:</strong> These are not canonical and should only be used
     * for temporary, low level access to object state.  They should not be retained across
     * VM execution.
     *
     * @param address a location in VM memory
     * @return the address wrapped as a remote object reference
     */
    public RemoteTeleReference makeTemporaryRemoteReference(Address address) {
        return new TemporaryTeleReference(vm(), address);
    }

    public ReferenceValue createReferenceValue(Reference reference) {
        if (reference instanceof TeleReference) {
            return TeleReferenceValue.from(vm(), reference);
        } else if (reference instanceof HostedReference) {
            return TeleReferenceValue.from(vm(), Reference.fromJava(reference.toJava()));
        }
        throw TeleError.unexpected("Got a non-Prototype, non-Tele reference in createReferenceValue");
    }

    /**
     * @return the canonical null/zero reference, can be compared with {@code ==}
     */
    public TeleReference zeroReference() {
        return zeroReference;
    }

    public int registeredRootCount() {
        return teleRoots == null ? 0 : teleRoots.registeredRootCount();
    }

    /**
     * Memory location in VM -> an inspector {@link Reference} that refers to the object that is (or once was) at that location.
     * Note that the location to which the {@link Reference} refers may actually change in the VM, something that only becomes
     * apparent at the conclusion of a GC when the root table gets refreshed.  At that point, this map, which is intended
     * keep References canonical, is unreliable and must be rebuilt.  Duplicates may be discovered, which must then be resolved.
     */
    private Map<Long, WeakReference<RemoteTeleReference>> rawReferenceToRemoteTeleReference = new HashMap<Long, WeakReference<RemoteTeleReference>>(100);

    /**
     * Called by MutableTeleReference.finalize() and CanonicalConstantTeleReference.finalize().
     */
    synchronized void finalizeCanonicalConstantTeleReference(CanonicalConstantTeleReference canonicalConstantTeleReference) {
        // This is not necessary; the loop below in refreshTeleReferenceCanonicalization() will remove
        // a finalized reference. More importantly, this method will be most likely be called on a
        // special VM thread used for running finalizers. In that case, modifying the map can
        // cause a ConcurrentModificationException in refreshTeleReferenceCanonicalization().
//        rawReferenceToRemoteTeleReference.remove(canonicalConstantTeleReference.raw().toLong());
    }

    /**
     * Rebuild the canonicalization table when we know that the raw (remote) bits of the remote location have changed by GC.
     */
    private void refreshTeleReferenceCanonicalization() {
        final Map<Long, WeakReference<RemoteTeleReference>> newMap = new HashMap<Long, WeakReference<RemoteTeleReference>>();

        // Make a copy of the values in the map as the loop may alter the map by causing 'finalizeCanonicalConstantTeleReference()'
        // to be called as weak references are cleaned up.
        ArrayList<WeakReference<RemoteTeleReference>> remoteTeleReferences = new ArrayList<WeakReference<RemoteTeleReference>>(rawReferenceToRemoteTeleReference.values());
        for (WeakReference<RemoteTeleReference> r : remoteTeleReferences) {
            final RemoteTeleReference remoteTeleReference = r.get();
            if (remoteTeleReference != null && !remoteTeleReference.raw().equals(Word.zero())) {
                WeakReference<RemoteTeleReference> remoteTeleReferenceRef = newMap.get(remoteTeleReference.raw().toLong());
                if (remoteTeleReferenceRef != null) {
                    RemoteTeleReference alreadyInstalledRemoteTeleReference = remoteTeleReferenceRef.get();
                    Log.println("Drop Duplicate: " + remoteTeleReference.toString() + " " + alreadyInstalledRemoteTeleReference.makeOID() + " " + remoteTeleReference.makeOID());

                    if (alreadyInstalledRemoteTeleReference instanceof MutableTeleReference) {
                        if (alreadyInstalledRemoteTeleReference.makeOID() > remoteTeleReference.makeOID()) {
                            MutableTeleReference mutableRemoteTeleReference = (MutableTeleReference) remoteTeleReference;
                            int index = mutableRemoteTeleReference.index();
                            if (index >= 0) {
                                teleRoots.unregister(index);
                            }
                            mutableRemoteTeleReference.setForwardedTeleReference(alreadyInstalledRemoteTeleReference);
                        } else {
                            teleRoots.unregister(((MutableTeleReference) alreadyInstalledRemoteTeleReference).index());
                            ((MutableTeleReference) alreadyInstalledRemoteTeleReference).setForwardedTeleReference(remoteTeleReference);
                            newMap.put(remoteTeleReference.raw().toLong(), r);
                        }
                    }

                } else {
                    newMap.put(remoteTeleReference.raw().toLong(), r);
                }
            }
        }
        teleRoots.flushUnregisteredRoots();
        rawReferenceToRemoteTeleReference = newMap;
    }

    // TODO (mlvdv) Debug this and replace the above
//    private void refreshTeleReferenceCanonicalization() {
//        // Save a copy of the old collection of references
//        final Iterable<WeakReference<RemoteTeleReference>> oldReferenceRefs = rawReferenceToRemoteTeleReference.values();
//        // Clear out the canonicalization table
//        rawReferenceToRemoteTeleReference = HashMapping.createVariableEqualityMapping();
//        // Populate the new canonicalization table, resolving duplicates (references whose current memory locations have become identical)
//        for (WeakReference<RemoteTeleReference> referenceRef : oldReferenceRefs) {
//            final RemoteTeleReference reference = referenceRef.get();
//            if (reference != null && !reference.raw().isZero()) {
//                final long referenceRaw = reference.raw().toLong();
//                final WeakReference<RemoteTeleReference> duplicateReferenceRef = rawReferenceToRemoteTeleReference.get(referenceRaw);
//                if (duplicateReferenceRef == null) {
//                    // No entry in the table at this location; add it.
//                    rawReferenceToRemoteTeleReference.put(referenceRaw, referenceRef);
//                } else {
//                    // We've located a duplicate reference, already in the new table, whose VM memory location is the same
//                    // as the one we're considering now.  This should only ever happen with mutable references
//                    final MutableTeleReference duplicateMutableReference = (MutableTeleReference) duplicateReferenceRef.get();
//                    final MutableTeleReference mutableReference = (MutableTeleReference) reference;
//
//                    final long duplicateReferenceOID = duplicateMutableReference.makeOID();
//                    final long referenceOID = mutableReference.makeOID();
//
//                    if (duplicateReferenceOID > referenceOID) {
//                        // The one already in the table is newer, based on the generated OIDs.  Leave it in the table and forward this one to the duplicate.
//                        teleRoots.unregister(mutableReference.index());
//                        mutableReference.setForwardedTeleReference(duplicateMutableReference);
//                        Trace.line(TRACE_VALUE, traceForwardMessage(mutableReference, duplicateMutableReference));
//                    } else {
//                        // the one in the table is older, based on the generated OID.  Replace it in the table with this one and forward the duplicate to this one.
//                        teleRoots.unregister(duplicateMutableReference.index());
//                        duplicateMutableReference.setForwardedTeleReference(reference);
//                        rawReferenceToRemoteTeleReference.put(referenceRaw, referenceRef);
//                        Trace.line(TRACE_VALUE, traceForwardMessage(duplicateMutableReference, mutableReference));
//                    }
//                }
//            }
//        }
//    }

    private String traceForwardMessage(MutableTeleReference fromReference, MutableTeleReference toReference) {
        final StringBuilder sb = new StringBuilder(tracePrefix());
        sb.append("Duplicate references: ");
        sb.append("(").append(fromReference.index()).append(",<").append(fromReference.makeOID()).append("> 0x").append(fromReference.raw().toHexString()).append(")");
        sb.append(" forwarded to ");
        sb.append("(").append(toReference.index()).append(",<").append(toReference.makeOID()).append("> 0x").append(toReference.raw().toHexString()).append(")");
        return sb.toString();
    }

    /**
     * Update Inspector state after a change to the remote contents of the Inspector root table.
     */
    public void updateCache(long epoch) {
        if (epoch > lastUpdateEpoch) {
            // Update Inspector's local cache of the remote Inspector root table.
            teleRoots.updateCache(epoch);
            // Rebuild the canonicalization map.
            refreshTeleReferenceCanonicalization();
            lastUpdateEpoch = epoch;
        } else {
            Trace.line(TRACE_VALUE, tracePrefix() + "redundant update epoch=" + epoch + ": " + this);
        }
    }

    public LocalTeleReferenceManager localTeleReferenceManager() {
        return localTeleReferenceManager;
    }

    /**
     * Returns some kind of reference associated with the given raw reference in the VM, depending
     * on what is known about the address.
     * <ol>
     * <li>If a canonical reference pointing at that location already exists, then returns it.</li>
     * <li>If the address is the valid origin of an object in a <strong>non-collected</strong> heap region, for
     * example the boot heap or an immortal heap, then return a new reference that is canonical,
     * but which is not tracked for possible GC relocation.</li>
     * <li>If the address is the valid origin of a live object in a dynamic heap region, then return
     * a new reference that is canonical and which is tracked for possible GC relocation.</li>
     * <li>If the address is the valid origin of an object in a dynamic heap region, but the object
     * is known <strong>not</strong> to be live, then return an unsafe, temporary reference that
     * wraps the address.</li>
     * <li>If the address does not point an an object origin,  then return an unsafe, temporary reference that
     * wraps the address.</li>
     * </ol>
     *
     * @param address a memory location in VM memory
     * @return a special kind of {@link Reference} implementation that encapsulates a remote
     * location in VM memory, allowing the reuse of much VM code that deals with references.
     */
    public synchronized TeleReference makeReference(Address address) {
        if (address.isZero()) {
            return zeroReference();
        }
        // TODO (mlvdv) Transition to the new reference management framework; use it for the regions supported so far
        final VmHeapRegion bootHeapRegion = vm().heap().bootHeapRegion();
        if (bootHeapRegion.contains(address)) {
            TeleReference teleReference = bootHeapRegion.objectReferenceManager().makeReference(address);
            return teleReference == null ? zeroReference() : teleReference;
        }

        final VmHeapRegion immortalHeapRegion = vm().heap().immortalHeapRegion();
        if (immortalHeapRegion != null && immortalHeapRegion.contains(address)) {
            TeleReference teleReference = immortalHeapRegion.objectReferenceManager().makeReference(address);
            return teleReference == null ? zeroReference() : teleReference;
        }

        final VmCodeCacheRegion compiledCodeRegion = vm().codeCache().findCodeCacheRegion(address);
        if (compiledCodeRegion != null) {
            TeleReference teleReference = compiledCodeRegion.objectReferenceManager().makeReference(address);
            return teleReference == null ? zeroReference() : teleReference;
        }

        // For everything else, use the old machinery; by now this should only be the dynamic heap.

        final WeakReference<RemoteTeleReference> existingCanonicalTeleReference = rawReferenceToRemoteTeleReference.get(address.toLong());
        if (existingCanonicalTeleReference != null) {
            final RemoteTeleReference remoteTeleReference = existingCanonicalTeleReference.get();
            if (remoteTeleReference != null) {
                // Found an existing canonical reference that points here; return it.
                return remoteTeleReference;
            }
        }
        if (!objects().isValidOrigin(address.asPointer())) {
            // Doesn't point at an object; create what amounts to a wrapped
            // address that is unsafe with respect to GC.
            return makeTemporaryRemoteReference(address);
        }
        if (!heap().containsInDynamicHeap(address) || TeleVM.targetLocation().kind == TeleVM.TargetLocation.Kind.FILE) {
            // Points to an object that's not collectible; create a canonical reference but don't
            // register it as a tracked root.
            final CanonicalConstantTeleReference nonCollectableTeleReference = new CanonicalConstantTeleReference(vm(), address);
            makeCanonical(nonCollectableTeleReference);
            return nonCollectableTeleReference;
        }
        if (!heap().getMemoryManagementInfo(address).status().isLive()) {
            // Points to an object that is in a collectible heap, but
            // which is known not to be live; create what amounts to a wrapped
            // address that is unsafe with respect to GC.
            return makeTemporaryRemoteReference(address);
        }
        // The common case:  points to a live object in a collectible heap;
        // return a new canonical reference that is traced for possible GC relocation.
        final int index = teleRoots.register(address);
        final MutableTeleReference liveCollectibleTeleReference = new MutableTeleReference(vm(), index);
        makeCanonical(liveCollectibleTeleReference);
        return liveCollectibleTeleReference;
    }

    /**
     * @param remoteTeleReference
     */
    private void makeCanonical(RemoteTeleReference remoteTeleReference) {
        rawReferenceToRemoteTeleReference.put(remoteTeleReference.raw().toLong(), new WeakReference<RemoteTeleReference>(remoteTeleReference));
    }

    synchronized Address getRawReference(MutableTeleReference mutableTeleReference) {
        return teleRoots.getRawReference(mutableTeleReference.index());
    }

    void finalizeMutableTeleReference(int index) {
        synchronized (this) {
            rawReferenceToRemoteTeleReference.remove(teleRoots.getRawReference(index).toLong());
        }
        // Synchronizing the following statement on 'this' often causes deadlock on
        // Linux when the SingleThread used by ptrace is trying to update the cache
        // via makeTeleReference() in the process of gathering threads.
        teleRoots.unregister(index);
    }


    static {
        if (Trace.hasLevel(1)) {
            Runtime.getRuntime().addShutdownHook(new Thread("Reference counts") {

                @Override
                public void run() {
                    System.out.println("References(by type):");
                    System.out.println("    " + "local = " + vmReferenceManager.localTeleReferenceManager.referenceCount());
                }
            });
        }

    }


}

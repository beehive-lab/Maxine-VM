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
package com.sun.max.tele.reference.legacy;

import java.lang.ref.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.Reference;

// TODO (mlvdv) Old Heap
/**
 * Manages references in the old style, hard wired for the semispace collector,
 * using a roots table in the VM.
 */
public class LegacyReferenceManager extends AbstractVmHolder implements TeleVMCache {

    private final VmReferenceManager vmReferenceManager;
    private TeleRoots teleRoots;

    public LegacyReferenceManager(TeleVM vm, VmReferenceManager referenceManager) {
        super(vm);
        this.vmReferenceManager = referenceManager;
        this.teleRoots = new TeleRoots(vm, referenceManager);
    }

    @Override
    public void updateCache(long epoch) {
        teleRoots.updateCache(epoch);
        // Rebuild the canonicalization map.
        refreshTeleReferenceCanonicalization();
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
            return vmReferenceManager.makeTemporaryRemoteReference(address);
        }
        if (!heap().containsInDynamicHeap(address) || TeleVM.targetLocation().kind == TeleVM.TargetLocation.Kind.FILE) {
            // Points to an object that's not collectible; create a canonical reference but don't
            // register it as a tracked root.
            final CanonicalConstantTeleReference nonCollectableTeleReference = new CanonicalConstantTeleReference(vm(), address, this);
            makeCanonical(nonCollectableTeleReference);
            return nonCollectableTeleReference;
        }
        if (!heap().getMemoryManagementInfo(address).status().isLive()) {
            // Points to an object that is in a collectible heap, but
            // which is known not to be live; create what amounts to a wrapped
            // address that is unsafe with respect to GC.
            return vmReferenceManager.makeTemporaryRemoteReference(address);
        }
        // The common case:  points to a live object in a collectible heap;
        // return a new canonical reference that is traced for possible GC relocation.
        final int index = teleRoots.register(address);
        final MutableTeleReference liveCollectibleTeleReference = new MutableTeleReference(vm(), index, this);
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

}

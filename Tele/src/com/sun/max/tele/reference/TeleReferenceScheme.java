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

import java.lang.ref.*;
import java.util.*;

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.reference.Reference;

/**
 * A variation of VM references for use by the Inspector to refer to an object location in the VM.
 * <br>
 * A  <strong>raw reference</strong> is an {@link Address} in VM memory where the object is currently
 * located.  However, the location may be subject to change by GC, so the raw reference may change over time.
 * <br>
 * Each reference is represented as a unique index into a root table, a mirror of such a table, in the VM.  The
 * table holds the current raw reference (address), and it is updated by the GC at the end of each collection.
 * <br>
 * References are intended to be canonical, i.e. refer to only one object.  However, in the course of inspection
 * duplicates may appear.  These are resolved at the conclusion of each GC.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleReferenceScheme extends AbstractVMScheme implements TeleVMCache, ReferenceScheme {

    private static final int TRACE_VALUE = 1;

    private final String tracePrefix;

    private long lastUpdateEpoch = -1L;

    private TeleVM vm;
    private TeleRoots teleRoots;

    protected TeleReferenceScheme() {
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
    }

    public void setTeleVM(TeleVM vm) {
        this.vm = vm;
        this.teleRoots = new TeleRoots(this);
    }

    public TeleVM vm() {
        return vm;
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }

    /**
     * Memory location in VM -> an inspector {@link Reference} that refers to the object that is (or once was) at that location.
     * Note that the location to which the {@link Reference} refers may actually change in the VM, something that only becomes
     * apparent at the conclusion of a GC when the root table gets refreshed.  At that point, this map, which is intended
     * keep References canonical, is unreliable and must be rebuilt.  Duplicates may be discovered, which must then be resolved.
     */
    private Map<Long, WeakReference<RemoteTeleReference>> rawReferenceToRemoteTeleReference = new HashMap<Long, WeakReference<RemoteTeleReference>>();

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

    /**
     * Returns a canonicalized tele reference associated with the given raw reference in the tele VM.
     */
    public synchronized TeleReference makeTeleReference(Address rawReference) {
        if (rawReference.isZero()) {
            return TeleReference.ZERO;
        }
        final WeakReference<RemoteTeleReference> r = rawReferenceToRemoteTeleReference.get(rawReference.toLong());
        RemoteTeleReference remoteTeleReference;
        if (r != null) {
            remoteTeleReference = r.get();
            if (remoteTeleReference != null) {
                return remoteTeleReference;
            }
        }
        remoteTeleReference = createTemporaryRemoteTeleReference(rawReference);
        if (vm.isValidOrigin(remoteTeleReference.toOrigin())) {
            if (vm().heap().containsInDynamicHeap(remoteTeleReference.toOrigin()) && TeleVM.targetLocation().kind != TeleVM.TargetLocation.Kind.FILE) {
                if (vm().heap().isInLiveMemory(remoteTeleReference.toOrigin())) {
                    final int index = teleRoots.register(rawReference);
                    remoteTeleReference = new MutableTeleReference(this, index);
                } else {
                    return remoteTeleReference;
                }
            } else {
                remoteTeleReference = new CanonicalConstantTeleReference(this, rawReference);
            }
            rawReferenceToRemoteTeleReference.put(rawReference.toLong(), new WeakReference<RemoteTeleReference>(remoteTeleReference));
        }

        return remoteTeleReference;
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

    private final Map<Object, WeakReference<LocalTeleReference>> objectToLocalTeleReference = new HashMap<Object, WeakReference<LocalTeleReference>>();

    /**
     * Called by LocalTeleReference.finalize().
     */
    synchronized void disposeCanonicalLocalReference(Object object) {
        objectToLocalTeleReference.remove(object);
    }

    /**
     * Returns a canonicalized local reference associated with the given local object.
     */
    public synchronized TeleReference makeLocalReference(Object object) {
        if (object == null) {
            return TeleReference.ZERO;
        }
        final WeakReference<LocalTeleReference> r = objectToLocalTeleReference.get(object);
        if (r != null) {
            return r.get();
        }
        final LocalTeleReference localTeleReference = new LocalTeleReference(this, object);
        objectToLocalTeleReference.put(object, new WeakReference<LocalTeleReference>(localTeleReference));
        return localTeleReference;
    }

    public RemoteTeleReference createTemporaryRemoteTeleReference(Address rawReference) {
        return new TemporaryTeleReference(this, rawReference);
    }

    public abstract RemoteTeleReference temporaryRemoteTeleReferenceFromOrigin(Word origin);

}

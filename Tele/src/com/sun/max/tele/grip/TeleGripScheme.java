/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.tele.grip;

import java.lang.ref.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.grip.*;

/**
 * A variation of VM grips for use by the Inspector to refer to an object location in the VM.
 * <br>
 * A  <strong>raw grip</strong> is an {@link Address} in VM memory where the object is currently
 * located.  However, the location may be subject to change by GC, so the raw grip may change over time.
 * <br>
 * Each grip is represented as a unique index into a root table, a mirror of such a table, in the VM.  The
 * table holds the current raw grip (address), and it is updated by the GC at the end of each collection.
 * <br>
 * Grips are intended to be cannonical, i.e. refer to only one object.  However, in the course of inspection
 * duplicates may appear.  These are resolved at the conclusion of each GC.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public abstract class TeleGripScheme extends AbstractVMScheme implements GripScheme {

    private static final int TRACE_VALUE = 1;

    private final String tracePrefix;

    private TeleVM teleVM;
    private TeleRoots teleRoots;

    protected TeleGripScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        this.tracePrefix = "[" + getClass().getSimpleName() + "] ";
    }

    public void setTeleVM(TeleVM teleVM) {
        this.teleVM = teleVM;
        this.teleRoots = new TeleRoots(this);
    }

    public TeleVM teleVM() {
        return teleVM;
    }

    /**
     * @return default prefix text for trace messages; identifies the class being traced.
     */
    protected String tracePrefix() {
        return tracePrefix;
    }

    /**
     * Memory location in VM -> an inspector {@link Grip} that refers to the object that is (or once was) at that location.
     * Note that the location to which the {@link Grip} refers may actually change in the VM, something that only becomes
     * apparent at the conclusion of a GC when the root table gets refreshed.  At that point, this map, which is intended
     * keep Grips canonical, is unreliable and must be rebuilt.  Duplicates may be discovered, which must then be resolved.
     */
    private Map<Long, WeakReference<RemoteTeleGrip>> rawGripToRemoteTeleGrip = new HashMap<Long, WeakReference<RemoteTeleGrip>>();

    /**
     * Called by MutableTeleGrip.finalize() and CanonicalConstantTeleGrip.finalize().
     */
    synchronized void finalizeCanonicalConstantTeleGrip(CanonicalConstantTeleGrip canonicalConstantTeleGrip) {
        rawGripToRemoteTeleGrip.remove(canonicalConstantTeleGrip.raw().toLong());
    }

    /**
     * Rebuild the canonicalization table when we know that the raw (remote) bits of the remote location have changed by GC.
     */
    private void refreshTeleGripCanonicalization() {
        final Map<Long, WeakReference<RemoteTeleGrip>> newMap = new HashMap<Long, WeakReference<RemoteTeleGrip>>();
        for (WeakReference<RemoteTeleGrip> r : rawGripToRemoteTeleGrip.values()) {
            final RemoteTeleGrip remoteTeleGrip = r.get();
            if (remoteTeleGrip != null && !remoteTeleGrip.raw().equals(Word.zero())) {
                WeakReference<RemoteTeleGrip> remoteTeleGripRef = newMap.get(remoteTeleGrip.raw().toLong());
                if (remoteTeleGripRef != null) {
                    RemoteTeleGrip alreadyInstalledRemoteTeleGrip = remoteTeleGripRef.get();
                    Log.println("Drop Duplicate: " + remoteTeleGrip.toString() + " " + alreadyInstalledRemoteTeleGrip.makeOID() + " " + remoteTeleGrip.makeOID());

                    if (alreadyInstalledRemoteTeleGrip.makeOID() > remoteTeleGrip.makeOID()) {
                        MutableTeleGrip mutableRemoteTeleGrip = (MutableTeleGrip) remoteTeleGrip;
                        int index = mutableRemoteTeleGrip.index();
                        if (index >= 0) {
                            teleRoots.unregister(index);
                        }
                        mutableRemoteTeleGrip.setForwardedTeleGrip(alreadyInstalledRemoteTeleGrip);
                    } else {
                        teleRoots.unregister(((MutableTeleGrip) alreadyInstalledRemoteTeleGrip).index());
                        ((MutableTeleGrip) alreadyInstalledRemoteTeleGrip).setForwardedTeleGrip(remoteTeleGrip);
                        newMap.put(remoteTeleGrip.raw().toLong(), r);
                    }

                } else {
                    newMap.put(remoteTeleGrip.raw().toLong(), r);
                }
            }
        }
        rawGripToRemoteTeleGrip = newMap;
    }

    // TODO (mlvdv) Debug this and replace the above
//    private void refreshTeleGripCanonicalization() {
//        // Save a copy of the old collection of grips
//        final Iterable<WeakReference<RemoteTeleGrip>> oldGripRefs = rawGripToRemoteTeleGrip.values();
//        // Clear out the canonicalization table
//        rawGripToRemoteTeleGrip = HashMapping.createVariableEqualityMapping();
//        // Populate the new canonicalization table, resolving duplicates (grips whose current memory locations have become identical)
//        for (WeakReference<RemoteTeleGrip> gripRef : oldGripRefs) {
//            final RemoteTeleGrip grip = gripRef.get();
//            if (grip != null && !grip.raw().isZero()) {
//                final long gripRaw = grip.raw().toLong();
//                final WeakReference<RemoteTeleGrip> duplicateGripRef = rawGripToRemoteTeleGrip.get(gripRaw);
//                if (duplicateGripRef == null) {
//                    // No entry in the table at this location; add it.
//                    rawGripToRemoteTeleGrip.put(gripRaw, gripRef);
//                } else {
//                    // We've located a duplicate grip, already in the new table, whose VM memory location is the same
//                    // as the one we're considering now.  This should only ever happen with mutable grips
//                    final MutableTeleGrip duplicateMutableGrip = (MutableTeleGrip) duplicateGripRef.get();
//                    final MutableTeleGrip mutableGrip = (MutableTeleGrip) grip;
//
//                    final long duplicateGripOID = duplicateMutableGrip.makeOID();
//                    final long gripOID = mutableGrip.makeOID();
//
//                    if (duplicateGripOID > gripOID) {
//                        // The one already in the table is newer, based on the generated OIDs.  Leave it in the table and forward this one to the duplicate.
//                        teleRoots.unregister(mutableGrip.index());
//                        mutableGrip.setForwardedTeleGrip(duplicateMutableGrip);
//                        Trace.line(TRACE_VALUE, traceForwardMessage(mutableGrip, duplicateMutableGrip));
//                    } else {
//                        // the one in the table is older, based on the generated OID.  Replace it in the table with this one and forward the duplicate to this one.
//                        teleRoots.unregister(duplicateMutableGrip.index());
//                        duplicateMutableGrip.setForwardedTeleGrip(grip);
//                        rawGripToRemoteTeleGrip.put(gripRaw, gripRef);
//                        Trace.line(TRACE_VALUE, traceForwardMessage(duplicateMutableGrip, mutableGrip));
//                    }
//                }
//            }
//        }
//    }

    private String traceForwardMessage(MutableTeleGrip fromGrip, MutableTeleGrip toGrip) {
        final StringBuilder sb = new StringBuilder(tracePrefix());
        sb.append("Duplicate grips: ");
        sb.append("(").append(fromGrip.index()).append(",<").append(fromGrip.makeOID()).append("> 0x").append(fromGrip.raw().toHexString()).append(")");
        sb.append(" forwarded to ");
        sb.append("(").append(toGrip.index()).append(",<").append(toGrip.makeOID()).append("> 0x").append(toGrip.raw().toHexString()).append(")");
        return sb.toString();
    }

    /**
     * Update Inspector state after a change to the remote contents of the Inspector root table.
     */
    public void refresh() {
        // Update Inspector's local cache of the remote Inspector root table.
        teleRoots.refresh();
        // Rebuild the canonicalization map.
        refreshTeleGripCanonicalization();
    }

    /**
     * Returns a canonicalized tele grip associated with the given raw grip in the tele VM.
     */
    public synchronized TeleGrip makeTeleGrip(Address rawGrip) {
        if (rawGrip.isZero()) {
            return TeleGrip.ZERO;
        }
        final WeakReference<RemoteTeleGrip> r = rawGripToRemoteTeleGrip.get(rawGrip.toLong());
        RemoteTeleGrip remoteTeleGrip;
        if (r != null) {
            remoteTeleGrip = r.get();
            if (remoteTeleGrip != null) {
                return remoteTeleGrip;
            }
        }
        remoteTeleGrip = createTemporaryRemoteTeleGrip(rawGrip);
        if (teleVM.isValidOrigin(remoteTeleGrip.toOrigin())) {
            if (teleVM().heap().containsInDynamicHeap(remoteTeleGrip.toOrigin())) {
                if (teleVM().heap().isInLiveMemory(remoteTeleGrip.toOrigin())) {
                    final int index = teleRoots.register(rawGrip);
                    remoteTeleGrip = new MutableTeleGrip(this, index);
                } else {
                    return remoteTeleGrip;
                }
            } else {
                remoteTeleGrip = new CanonicalConstantTeleGrip(this, rawGrip);
            }
            rawGripToRemoteTeleGrip.put(rawGrip.toLong(), new WeakReference<RemoteTeleGrip>(remoteTeleGrip));
        }

        return remoteTeleGrip;
    }

    synchronized Address getRawGrip(MutableTeleGrip mutableTeleGrip) {
        return teleRoots.getRawGrip(mutableTeleGrip.index());
    }

    synchronized void finalizeMutableTeleGrip(int index) {
        rawGripToRemoteTeleGrip.remove(teleRoots.getRawGrip(index).toLong());
        teleRoots.unregister(index);
    }

    private final Map<Object, WeakReference<LocalTeleGrip>> objectToLocalTeleGrip = new HashMap<Object, WeakReference<LocalTeleGrip>>();

    /**
     * Called by LocalTeleGrip.finalize().
     */
    synchronized void disposeCanonicalLocalGrip(Object object) {
        objectToLocalTeleGrip.remove(object);
    }

    /**
     * Returns a canonicalized local grip associated with the given local object.
     */
    public synchronized TeleGrip makeLocalGrip(Object object) {
        if (object == null) {
            return TeleGrip.ZERO;
        }
        final WeakReference<LocalTeleGrip> r = objectToLocalTeleGrip.get(object);
        if (r != null) {
            return r.get();
        }
        final LocalTeleGrip localTeleGrip = new LocalTeleGrip(this, object);
        objectToLocalTeleGrip.put(object, new WeakReference<LocalTeleGrip>(localTeleGrip));
        return localTeleGrip;
    }

    public RemoteTeleGrip createTemporaryRemoteTeleGrip(Address rawGrip) {
        return new TemporaryTeleGrip(this, rawGrip);
    }

    public abstract RemoteTeleGrip temporaryRemoteTeleGripFromOrigin(Word origin);

}

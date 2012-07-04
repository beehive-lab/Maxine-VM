/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.method;

import java.io.*;
import java.lang.ref.*;
import java.text.*;
import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.object.TeleTargetMethod.CodeCacheReferenceKind;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;

/**
 * A manager for remote references to objects allocated in a {@link SemiSpaceCodeRegion}.  This manager:
 * <ul>
 * <li>assumes that objects can be relocated and eventually collected/evicted.</li>
 * <li>assumes that there can only
 * be objects in the region of the kinds enumerated by {@link CodeCacheReferenceKiknd}, which
 * are pointed to by corresponding fields in a {@link TeleMethodActor}.</li>
 * <li>creates <em>canonical references</em>.</li>
 * </ul>
 * <p>
 * This implementation depends on knowledge of the internal workings of {@link TargetMethod}.
 *
 * @see TargetMethod
 * @see VmCodeCacheRegion
 * @see TeleTargetMethod
 */
final class SemispaceCodeCacheRemoteReferenceManager extends AbstractVmHolder implements RemoteObjectReferenceManager {

    /**
     * The code cache region whose objects are being managed.
     */
    private final VmSemiSpaceCodeCacheRegion semispaceCodeCacheRegion;

    /**
     * The status of the region with respect to object management.
     */
    private HeapPhase heapPhase;

    /**
     * A two level map.  For each of the possible kinds of references that can be created,
     * record the ones we've created, indexed by TeleTargetMethod
     * <pre>
     *  CodeCacheReferenceKind  -->    [  TeleTargetMethod --> WeakReference&lt;TeleReference&gt;]
     * </pre>
     */
    private final Map<CodeCacheReferenceKind, Map<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> > > refMaps =
        new HashMap<CodeCacheReferenceKind, Map<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> > >();

    /**
     * Creates a manager for objects allocated in a {@link SemiSpaceCodeRegion}.
     */
    public SemispaceCodeCacheRemoteReferenceManager(TeleVM vm, VmSemiSpaceCodeCacheRegion semispaceCodeCacheRegion) {
        super(vm);
        this.semispaceCodeCacheRegion = semispaceCodeCacheRegion;
        this.heapPhase = HeapPhase.MUTATING;
        // Create a separate map for references of each kind
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            refMaps.put(kind, new HashMap<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> >());
        }
    }

    // TODO (mlvdv) Interpret this status for the special case of objects in the code cache.
    public HeapPhase phase() {
        return heapPhase;
    }



    /**
     * {@inheritDoc}
     * <p>
     * We don't need a heuristic for objects here; if they are present, then they are
     * pointed at by one of the fields in the {@link TargetMethod}.
     */
    public ObjectStatus objectStatusAt(Address origin) throws TeleError {
        TeleError.check(semispaceCodeCacheRegion.memoryRegion().contains(origin), "Location is outside region");
        final TeleCompilation compilation = semispaceCodeCacheRegion.findCompilation(origin);
        if (compilation != null) {
            final TeleTargetMethod teleTargetMethod = compilation.teleTargetMethod();
            if (teleTargetMethod != null) {
                // The address is contained in the code cache allocation for this target method.
                for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
                    // Does one of the target method's references point at this location??
                    final Address objectOrigin = teleTargetMethod.codeCacheObjectOrigin(kind);
                    if (objectOrigin != null && objectOrigin.equals(origin)) {
                        // The specified location matches one of the target method's pointers.
                        // There should be an object there, but check just in case.
                        assert objects().isPlausibleOriginUnsafe(objectOrigin);
                        return ObjectStatus.LIVE;
                    }
                }
            }
        }
        return ObjectStatus.DEAD;
    }

    public boolean isForwardingAddress(Address forwardingAddress) {
        return false;
    }

    @Override
    public RemoteReference makeReference(Address origin) throws TeleError {
        assert vm().lockHeldByCurrentThread();
        TeleError.check(semispaceCodeCacheRegion.contains(origin));
        // Locate the compilation, if any, whose code cache allocation in VM memory includes the address
        final TeleCompilation compilation = semispaceCodeCacheRegion.findCompilation(origin);
        if (compilation != null) {
            final TeleTargetMethod teleTargetMethod = compilation.teleTargetMethod();
            if (teleTargetMethod != null) {
                // The address is contained in the code cache allocation for this target method.
                for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
                    // Does one of the target method's references point at this location??
                    final Address objectOrigin = teleTargetMethod.codeCacheObjectOrigin(kind);
                    if (objectOrigin != null && origin.equals(objectOrigin)) {
                        // Return a canonical reference to this location
                        return makeCanonicalReference(teleTargetMethod, kind);
                    }
                }
            }
        }
        return vm().referenceManager().zeroReference();
    }

    /**
     * {@inheritDoc}
     * <p>
     * There are no <em>quasi</em> objects in this kind of region.
     */
    public RemoteReference makeQuasiReference(Address origin) throws TeleError {
        return null;
    }

    private int activeReferenceCount() {
        int count = 0;
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            final Map<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> > kindRefMap = refMaps.get(kind);
            for (WeakReference<SemispaceCodeCacheRemoteReference> weakRef : kindRefMap.values()) {
                if (weakRef != null) {
                    final SemispaceCodeCacheRemoteReference teleRef = weakRef.get();
                    if (teleRef != null) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private int totalReferenceCount() {
        int count = 0;
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            count += refMaps.get(kind).size();
        }
        return count;
    }

    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        final String indentation = Strings.times(' ', indent);
        printStream.println(indentation + "Object holding region: " + semispaceCodeCacheRegion.entityName());
        final NumberFormat formatter = NumberFormat.getInstance();
        final StringBuilder sb2 = new StringBuilder();
        final int activeReferenceCount = activeReferenceCount();
        final int totalReferenceCount = totalReferenceCount();
        sb2.append("object refs:  active=" + formatter.format(activeReferenceCount));
        sb2.append(", inactive=" + formatter.format(totalReferenceCount - activeReferenceCount));
        sb2.append(", mgr=" + getClass().getSimpleName());
        printStream.println(indentation + sb2.toString());
    }


    /**
     * @return a canonical reference of the specified kind for the specified target method
     */
    private RemoteReference makeCanonicalReference(TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
        SemispaceCodeCacheRemoteReference remoteRef = null;
        final Map<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> > kindMap = refMaps.get(kind);
        WeakReference<SemispaceCodeCacheRemoteReference> weakRef = kindMap.get(teleTargetMethod);
        if (weakRef != null) {
            remoteRef = weakRef.get();
        }
        if (remoteRef == null) {
            // By construction, there should be an object at the location; let's just check.
            assert objects().isPlausibleOriginUnsafe(teleTargetMethod.codeCacheObjectOrigin(kind));
            remoteRef = new SemispaceCodeCacheRemoteReference(vm(), teleTargetMethod, kind);
            kindMap.put(teleTargetMethod, new WeakReference<SemispaceCodeCacheRemoteReference>(remoteRef));
        }
        return remoteRef;
    }


    /**
     * A remote object reference constrained to point only at data stored in object format in a region of code cache. In
     * particular, it may point only at one of the three possible data arrays pointed at by an instance of
     * {@link TargetMethod} in the VM.
     * <p>
     * The current code eviction algorithm marks the three fields that may contain references to data arrays when the
     * method is evicted; it does so by assigning to them a sentinel reference to an empty array that lives in the boot
     * heap.
     *
     * @see TargetMethod
     * @see CodeEviction
     */
    private final class SemispaceCodeCacheRemoteReference extends AbstractCodeCacheRemoteReference {

        private final CodeCacheReferenceKind kind;
        private Address origin = Address.zero();
        private ObjectStatus status = ObjectStatus.LIVE;

        public SemispaceCodeCacheRemoteReference(TeleVM vm, TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
            super(vm, teleTargetMethod);
            this.origin = teleTargetMethod.codeCacheObjectOrigin(kind);
            this.kind = kind;
        }

        @Override
        public ObjectStatus status() {
            // References to objects in the code cache are treated for now as either
            // LIVE or DEAD.
            if (status.isLive() && teleTargetMethod().isCodeEvicted()) {
                status = ObjectStatus.DEAD;
            }
            return status;
        }

        // TODO (mlvdv) we actually need only check the origin if we can determine that there has
        // been an eviction since the last time we checked (once we have a non-zero origin in the first
        // place. Access to that information hasn't been arranged yet.
        /**
         * {@inheritDoc}
         * <p>
         * Return the actual address of the array in the code cache, even after it has been reassigned
         * when the method's code is <em>wiped</em> during eviction.
         */
        @Override
        public Address origin() {
            if (status().isDead()) {
                return Address.zero();
            }
            origin = teleTargetMethod().codeCacheObjectOrigin(kind);
            return origin;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Objects in code cache allocations may be relocated, but they are never <em>forwarded</em>
         * in the usual GC sense.
         */
        @Override
        public Address forwardedFrom() {
            return Address.zero();
        }

        /**
         * {@inheritDoc}
         * <p>
         * Objects in code cache allocations may be relocated, but they are never <em>forwarded</em>
         * in the usual GC sense.
         */
        @Override
        public Address forwardedTo() {
            return Address.zero();
        }

        @Override
        public String gcDescription() {
            return "object in a semispace managed code cache region:  " + kind.label();
        }

    }

}

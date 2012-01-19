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

import java.lang.ref.*;
import java.util.*;

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
final class SemispaceCodeCacheRemoteReferenceManager extends AbstractRemoteReferenceManager {

    /**
     * The code cache region whose objects are being managed.
     */
    private final VmCodeCacheRegion codeCacheRegion;

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
    public SemispaceCodeCacheRemoteReferenceManager(TeleVM vm, VmCodeCacheRegion codeCacheRegion) {
        super(vm);
        this.codeCacheRegion = codeCacheRegion;
        this.heapPhase = HeapPhase.ALLOCATING;
        // Create a separate map for references of each kind
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            refMaps.put(kind, new HashMap<TeleTargetMethod, WeakReference<SemispaceCodeCacheRemoteReference> >());
        }
    }

    public VmObjectHoldingRegion objectRegion() {
        return codeCacheRegion;
    }

    // TODO (mlvdv) Interpret this status for the special case of objects in the code cache.
    public HeapPhase heapPhase() {
        return heapPhase;
    }

    /**
     * {@inheritDoc}
     * <p>
     * We don't need a heuristic for objects here; if they are present, then they are
     * pointed at by one of the fields in the {@link TargetMethod}.
     */
    @Override
    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(codeCacheRegion.memoryRegion().contains(origin), "Location is outside region");
        final TeleCompilation compilation = codeCacheRegion.findCompilation(origin);
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
                        return objects().isObjectOriginHeuristic(objectOrigin);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public RemoteReference makeReference(Address origin) throws TeleError {
        TeleError.check(codeCacheRegion.contains(origin));
        // Locate the compilation, if any, whose code cache allocation in VM memory includes the address
        final TeleCompilation compilation = codeCacheRegion.findCompilation(origin);
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
        return null;
    }

    public int activeReferenceCount() {
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

    public int totalReferenceCount() {
        int count = 0;
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            count += refMaps.get(kind).size();
        }
        return count;
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
            assert objects().isObjectOriginHeuristic(teleTargetMethod.codeCacheObjectOrigin(kind));
            remoteRef = new SemispaceCodeCacheRemoteReference(vm(), teleTargetMethod, kind);
            kindMap.put(teleTargetMethod, new WeakReference<SemispaceCodeCacheRemoteReference>(remoteRef));
        }
        return remoteRef;
    }


    /**
     * A remote object reference constrained to point only at data stored in object format in a region
     * of code cache.  In particular, it may point only at one of the three possible data arrays pointed
     * at by an instance of {@link TargetMethod} in the VM.
     * <p>
     * The current code eviction algorithm marks the three fields that may contain references to data
     * arrays when the method is evicted; it does so by assigning to them a sentinel reference to
     * an empty array that lives in the boot heap.
     *
     * @see TargetMethod
     * @see CodeEviction
     */
    private final class SemispaceCodeCacheRemoteReference extends AbstractCodeCacheRemoteReference {

        private Address lastValidOrigin = Address.zero();

        public SemispaceCodeCacheRemoteReference(TeleVM vm, TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
            super(vm, teleTargetMethod, kind);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Return the actual address of the array in the code cache, even after it has been reassigned
         * when the method's code is <em>wiped</em> during eviction.
         */
        @Override
        public Address raw() {
            if (memoryStatus() == ObjectMemoryStatus.LIVE) {
                lastValidOrigin = super.raw();
            }
            return lastValidOrigin;
        }

        @Override
        public ObjectMemoryStatus memoryStatus() {
            // Don't look at the memory status of the teleTargetMethod; that refers to the
            // TargetMethod object, not to the objects stored in the code cache, which is
            // what we're dealing with here.
            return teleTargetMethod().isCodeEvicted() ? ObjectMemoryStatus.DEAD : ObjectMemoryStatus.LIVE;
        }

    }

}

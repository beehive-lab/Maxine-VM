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
import com.sun.max.vm.compiler.target.*;

/**
 * A manager for remote references to objects allocated in an <em>unmanaged</em> {@linkplain VmCodeCacheRegion code cache region}.
 * <ul>
 * <li>This manager assumes that objects in this region, once created, neither move nor are collected.</li>
 * <li>This manager assumes that there can only
 * be objects in the region of the kinds enumerated by {@link CodeCacheReferenceKiknd}, which
 * are pointed to by corresponding fields in a (heap) instance of {@link TeleMethodActor}.</li>
 * <li>This manager creates <em>canonical references</em>.</li>
 * </ul>
 * <p>
 * This implementation depends on knowledge of the internal workings of {@link TargetMethod}.
 *
 * @see TargetMethod
 * @see VmCodeCacheRegion
 * @see TeleTargetMethod
 */
final class UnmanagedCodeCacheRemoteReferenceManager extends AbstractRemoteReferenceManager {

    /**
     * The code cache region whose objects are being managed.
     */
    private final VmCodeCacheRegion codeCacheRegion;

    /**
     * A two level map.  For each of the possible kinds of references that can be created,
     * it records the ones we've created so far, indexed by TeleTargetMethod
     * <pre>
     *  CodeCacheReferenceKind  -->  [  TeleTargetMethod --> WeakReference&lt;TeleReference&gt;]
     * </pre>
     */
    private final Map<CodeCacheReferenceKind, Map<TeleTargetMethod, WeakReference<UnmanagedCodeCacheRemoteReference> > > refMaps =
        new HashMap<CodeCacheReferenceKind, Map<TeleTargetMethod, WeakReference<UnmanagedCodeCacheRemoteReference> > >();

    /**
     * Create a manager for objects allocated in an <em>unmanaged</em> {@linkplain VmCodeCacheRegion code cache region}.
     *
     * @param vm the TM
     * @param codeCacheRegion the code cache region whose objects are to be managed.
     */
    public UnmanagedCodeCacheRemoteReferenceManager(TeleVM vm, VmCodeCacheRegion codeCacheRegion) {
        super(vm);
        this.codeCacheRegion = codeCacheRegion;
        // Create a separate map for references of each kind
        for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
            refMaps.put(kind, new HashMap<TeleTargetMethod, WeakReference<UnmanagedCodeCacheRemoteReference> >());
        }
    }

    public MaxObjectHoldingRegion objectRegion() {
        return codeCacheRegion;
    }

    /**
     * {@inheritDoc}
     * <p>
     * We don't need a heuristic for objects in a code cache region; if they are present, then they are
     * pointed at by one of the fields in a {@link TargetMethod}.
     */
    public boolean isObjectOrigin(Address origin) throws TeleError {
        TeleError.check(codeCacheRegion.memoryRegion().contains(origin), "Location is outside region");
        final TeleCompilation compilation = codeCacheRegion.findCompilation(origin);
        if (compilation != null) {
            final TeleTargetMethod teleTargetMethod = compilation.teleTargetMethod();
            if (teleTargetMethod != null) {
                // The address is contained in the code cache allocation for this target method.
                // Does one of the target method's references point at this location??
                for (CodeCacheReferenceKind kind : CodeCacheReferenceKind.values()) {
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

    /**
     * {@inheritDoc}
     * <p>
     * The only objects in the code cache region must be pointed to by a field in
     * some (heap) instance of {@link TargetMethod}.  This is a precise check.
     * <p>
     * If the location is an origin of such an object, return an object reference
     * that indirects through the {@link TargetMethod} that points at the object.
     */
    public TeleReference makeReference(Address origin) throws TeleError {
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
            final Map<TeleTargetMethod, WeakReference<UnmanagedCodeCacheRemoteReference> > kindRefMap = refMaps.get(kind);
            for (WeakReference<UnmanagedCodeCacheRemoteReference> weakRef : kindRefMap.values()) {
                if (weakRef != null) {
                    final UnmanagedCodeCacheRemoteReference teleRef = weakRef.get();
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
    private TeleReference makeCanonicalReference(TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
        UnmanagedCodeCacheRemoteReference remoteRef = null;
        final Map<TeleTargetMethod, WeakReference<UnmanagedCodeCacheRemoteReference> > kindMap = refMaps.get(kind);
        WeakReference<UnmanagedCodeCacheRemoteReference> weakRef = kindMap.get(teleTargetMethod);
        if (weakRef != null) {
            remoteRef = weakRef.get();
        }
        if (remoteRef == null) {
            // By construction, there should be an object at the location; let's just check.
            assert objects().isObjectOriginHeuristic(teleTargetMethod.codeCacheObjectOrigin(kind));
            remoteRef = new UnmanagedCodeCacheRemoteReference(vm(), teleTargetMethod, kind);
            kindMap.put(teleTargetMethod, new WeakReference<UnmanagedCodeCacheRemoteReference>(remoteRef));
        }
        return remoteRef;
    }


    /**
     * A remote object reference constrained to point only at data stored in object format in a region
     * of code cache.  In particular, it may point only at one of the three possible data arrays pointed
     * at by an instance of {@link TargetMethod} in the VM.
     * <p>
     * For unmanaged heaps, such data is always live by definition.
     *
     * @see TargetMethod
     */
    private final class UnmanagedCodeCacheRemoteReference extends AbstractCodeCacheRemoteReference {

        public UnmanagedCodeCacheRemoteReference(TeleVM vm, TeleTargetMethod teleTargetMethod, CodeCacheReferenceKind kind) {
            super(vm, teleTargetMethod, kind);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Objects in an unmanaged code cache region are immortal.
         */
        @Override
        public ObjectMemoryStatus memoryStatus() {
            return ObjectMemoryStatus.LIVE;
        }

    }

}

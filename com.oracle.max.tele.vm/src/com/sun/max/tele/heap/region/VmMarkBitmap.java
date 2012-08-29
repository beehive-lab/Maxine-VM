/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.heap.region;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;


/**
 * Access to the Mark Bitmap used by some GC implementations.
 * <p>
 * The Mark Bitmap is stored in a region separate from the heap, allocated from the OS.
 * The region is filled with a single array (in standard Maxine format) of longs.
 */
public class VmMarkBitmap extends AbstractVmHolder
    implements TeleVMCache, MaxMarkBitmap, VmObjectHoldingRegion<MaxMarkBitmap> {

    /**
     * Representation of a VM memory region used to hold a MarkBitmap.  The MarkBitmap is implemented as a single long array that
     * occupied the entire region.
     * <p>
     * This region has no parent; it is allocated dynamically from the OS
     * <p>
     * This region has no children.
     */
    private static final class MarkBitmapMemoryRegion extends TeleDelegatedMemoryRegion
        implements MaxEntityMemoryRegion<MaxMarkBitmap> {

        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

        private final MaxMarkBitmap owner;

        protected MarkBitmapMemoryRegion(MaxVM vm, MaxMarkBitmap owner, TeleMemoryRegion teleMemoryRegion) {
            super(vm, teleMemoryRegion);
            this.owner = owner;
        }

        @Override
        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            // The MarkBitmap fully occupies a region allocated from the OS, not part of any other region.
            return null;
        }

        @Override
        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        @Override
        public MaxMarkBitmap owner() {
            return owner;
        }
    }

    private final TeleTricolorHeapMarker heapMarker;
    private final MarkBitmapMemoryRegion markBitmapMemoryRegion;
    private TeleArrayObject markBitmapArray;

    private final MarkBitmapObjectReferenceManager objectReferenceManager;

    public VmMarkBitmap(TeleVM vm, TeleTricolorHeapMarker heapMarker) {
        super(vm);
        this.heapMarker = heapMarker;
        this.markBitmapMemoryRegion = new MarkBitmapMemoryRegion(vm, this, heapMarker.colorMap());
        this.objectReferenceManager = new MarkBitmapObjectReferenceManager(vm);
    }

    public void updateCache(long epoch) {
        objectReferenceManager.updateCache(epoch);
        if (markBitmapArray == null && objectReferenceManager.longArrayRef != null) {
            markBitmapArray = (TeleArrayObject) objects().makeTeleObject(objectReferenceManager.longArrayRef);
            markBitmapArray.setMaxineRole("Heap-Mark Bitmap data");
        }
    }

    public String entityName() {
        return markBitmapMemoryRegion.regionName();
    }

    public String entityDescription() {
        return "The region of OS-allocated memory in which a Mark Bitmap is stored, formatted as a single long array";
    }

    public MaxEntityMemoryRegion<MaxMarkBitmap> memoryRegion() {
        return markBitmapMemoryRegion;
    }

    public boolean contains(Address address) {
        return markBitmapMemoryRegion.contains(address);
    }

    public MaxObject representation() {
        return markBitmapArray;
    }

    public boolean isCovered(Address heapAddress) {
        // TODO Auto-generated method stub
        return false;
    }

    public int bitIndex(Address heapAddress) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int bitmapWordIndex(Address heapAddress) {
        // TODO Auto-generated method stub
        return 0;
    }

    public Address bitmapWord(Address heapAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address heapAddress(int bitIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public Address bitmapWord(int bitIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public Color color(int bitIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    public Color color(Address heapAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    public Color[] colors() {
        // TODO Auto-generated method stub
        return null;
    }

    public RemoteObjectReferenceManager objectReferenceManager() {
        return objectReferenceManager;
    }

    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        TeleWarning.unimplemented();
    }

    /**
     * Manager for object references for the unmanaged mark bitmap region, which contains,
     * once initialized, a singleton long array.
     */
    private class MarkBitmapObjectReferenceManager extends AbstractVmHolder implements RemoteObjectReferenceManager, TeleVMCache {

        private Address longArrayOrigin = Address.zero();
        private ConstantRemoteReference longArrayRef = null;

        protected MarkBitmapObjectReferenceManager(TeleVM vm) {
            super(vm);
        }

        /**
         * {@inheritDoc}
         * <p>
         * There is no GC cycle; the singleton long array is neither relocated nor collected.
         */
        public HeapPhase phase() {
            return HeapPhase.MUTATING;
        }

        public ObjectStatus objectStatusAt(Address origin) {

            if (longArrayOrigin.isZero()) {
                return ObjectStatus.DEAD;
            }
            TeleError.check(memoryRegion().contains(origin), "Location is outside region");
            return origin.equals(longArrayOrigin) ? ObjectStatus.LIVE : ObjectStatus.DEAD;
        }

        public boolean isForwardingAddress(Address forwardingAddress) {
            return false;
        }

        public RemoteReference makeReference(Address origin) throws TeleError {
            return objectStatusAt(origin).isLive() ? longArrayRef : null;
        }

        public RemoteReference makeQuasiReference(Address origin) {
            return null;
        }

        public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
            TeleWarning.unimplemented();
        }

        public void updateCache(long epoch) {
            // Force cached information from remote object describing the memory region to be updated now,
            // rather than waiting until later in the update cycle, so we'll have this information about the
            // array right away.
            markBitmapMemoryRegion.updateCache(epoch);
            if (longArrayOrigin.isZero() && memoryRegion().isAllocated()) {
                final Pointer start = memoryRegion().start().asPointer();
                longArrayOrigin = objects().layoutScheme().generalLayout.cellToOrigin(start);
                longArrayRef = new ConstantRemoteReference(vm(), longArrayOrigin) {

                    @Override
                    public ObjectStatus status() {
                        return ObjectStatus.LIVE;
                    }

                    @Override
                    public ObjectStatus priorStatus() {
                        return null;
                    }
                };
            }

        }
    }



}

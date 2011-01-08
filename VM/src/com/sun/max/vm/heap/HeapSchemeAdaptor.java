/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap;

import static com.sun.max.vm.thread.VmThread.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.management.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Class to capture common methods for heap scheme implementations.
 *
 * @author Mick Jordan
 */
public abstract class HeapSchemeAdaptor extends AbstractVMScheme implements HeapScheme {


    public static class GarbageCollectorMXBeanAdaptor extends MemoryManagerMXBeanAdaptor implements GarbageCollectorMXBean  {
        public GarbageCollectorMXBeanAdaptor(String name) {
            super(name);
        }

        public GcInfo getLastGcInfo() {
            return null;
        }

        public long getCollectionCount() {
            return 0;
        }

        public long getCollectionTime() {
            return 0;
        }

    }

    /**
     * Local copy of Dynamic Hub for java.lang.Object to speed up filling cell with dead object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected static DynamicHub OBJECT_HUB;
    /**
     * Local copy of Dynamic Hub for byte [] to speed up filling cell with dead object.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected static DynamicHub BYTE_ARRAY_HUB;
    /**
     * Size of an java.lang.Object instance, presumably the minimum object size.
     */
    @CONSTANT_WHEN_NOT_ZERO
    public static Size MIN_OBJECT_SIZE;
    /**
     * Size of a byte array header.
     */
    @CONSTANT_WHEN_NOT_ZERO
    protected static Size BYTE_ARRAY_HEADER_SIZE;

    /**
     * Plants a dead instance of java.lang.Object at the specified pointer.
     */
    private static void plantDeadObject(Pointer cell) {
        DebugHeap.writeCellTag(cell);
        final Pointer origin = Layout.tupleCellToOrigin(cell);
        Memory.clearWords(cell, MIN_OBJECT_SIZE.dividedBy(Word.size()).toInt());
        Layout.writeHubReference(origin, Reference.fromJava(OBJECT_HUB));
    }

    /**
     * Plants a dead byte array at the specified cell.
     */
    private static void plantDeadByteArray(Pointer cell, Size size) {
        DebugHeap.writeCellTag(cell);
        final int length = size.minus(BYTE_ARRAY_HEADER_SIZE).toInt();
        final Pointer origin = Layout.arrayCellToOrigin(cell);
        Memory.clearWords(cell, BYTE_ARRAY_HEADER_SIZE.dividedBy(Word.size()).toInt());
        Layout.writeArrayLength(origin, length);
        Layout.writeHubReference(origin, Reference.fromJava(BYTE_ARRAY_HUB));
    }


    /**
     * Helper function to fill an area with a (tagged) dead object.
     * Used to make a dead area in the heap parseable by GCs.
     *
     * @param start start of the dead heap area
     * @param end end of the dead heap area
     */
    public static void fillWithDeadObject(Pointer start, Pointer end) {
        Pointer cell = DebugHeap.adjustForDebugTag(start);
        Size deadObjectSize = end.minus(cell).asSize();
        if (deadObjectSize.greaterThan(MIN_OBJECT_SIZE)) {
            plantDeadByteArray(cell, deadObjectSize);
        } else if (deadObjectSize.equals(MIN_OBJECT_SIZE)) {
            plantDeadObject(cell);
        } else {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("[");
            Log.print(start);
            Log.print(",");
            Log.print(end);
            Log.print(" ("); Log.print(end.minus(start));
            Log.print(")");
            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("Not enough space to fit a dead object");
        }
    }

    /**
     * Switch to turn off allocation globally.
     */
    protected boolean allocationEnabled = true;

    @HOSTED_ONLY
    public HeapSchemeAdaptor() {
    }

    @Override
    public void initialize(MaxineVM.Phase phase) {
        super.initialize(phase);
        if (MaxineVM.isHosted()) {
            OBJECT_HUB = ClassRegistry.OBJECT.dynamicHub();
            BYTE_ARRAY_HUB = ClassRegistry.BYTE_ARRAY.dynamicHub();
            MIN_OBJECT_SIZE = OBJECT_HUB.tupleSize;
            BYTE_ARRAY_HEADER_SIZE = Layout.byteArrayLayout().getArraySize(Kind.BYTE, 0);
        }
    }

    @HOSTED_ONLY
    public CodeManager createCodeManager() {
        switch (Platform.platform().os) {
            case LINUX: {
                return new LowAddressCodeManager();
            }
            case DARWIN:
            case SOLARIS: {
                return new VariableAddressCodeManager();
            }
            case MAXVE: {
                return new FixedAddressCodeManager();
            }
            default: {
                FatalError.unimplemented();
                return null;
            }
        }
    }

    public boolean decreaseMemory(Size amount) {
        HeapScheme.Inspect.notifyDecreaseMemoryRequested(amount);
        return false;
    }

    public boolean increaseMemory(Size amount) {
        HeapScheme.Inspect.notifyIncreaseMemoryRequested(amount);
        return false;
    }

    public void disableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    public void enableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    public boolean isAllocationDisabledForCurrentThread() {
        throw FatalError.unimplemented();
    }

    public void notifyCurrentThreadDetach() {
        // nothing by default
    }

    @INLINE(override = true)
    public boolean usesTLAB() {
        return false;
    }

    @INLINE(override = true)
    public boolean supportsTagging() {
        return true;
    }

    public void disableCustomAllocation() {
        final Pointer etla = ETLA.load(currentTLA());
        CUSTOM_ALLOCATION_ENABLED.store(etla, Word.zero());
    }

    public void enableCustomAllocation(Address customAllocator) {
        final Pointer etla = ETLA.load(currentTLA());
        CUSTOM_ALLOCATION_ENABLED.store(etla, customAllocator);
    }

    public long maxObjectInspectionAge() {
        FatalError.unimplemented();
        return 0;
    }

    public GarbageCollectorMXBean getGarbageCollectorMXBean() {
        return new GarbageCollectorMXBeanAdaptor("Invalid") {
            @Override
            public boolean isValid() {
                return false;
            }
        };
    }

    public int reservedVirtualSpaceSize() {
        return 0;
    }

    public BootRegionMappingConstraint bootRegionMappingConstraint() {
        return BootRegionMappingConstraint.ANYWHERE;
    }
}

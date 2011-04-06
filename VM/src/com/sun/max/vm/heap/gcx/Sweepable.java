/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;

/**
 * Interface that space managers must implement to be notified of sweeping events emitted by a sweeping collector.
 * The interface allows both precise and imprecise sweeping. Precise sweeping inspects
 * every marked location to determine the end of the live data and identify every
 * dead space, and invoke the space manager for every. Imprecise sweeping only inspects marked location separated by a minimum
 * distance, thus avoiding inspecting object when the size of a potential free chunk is
 * too small to be of interest to the free space manager.
 *
 * @author Laurent Daynes.
 */
public abstract class Sweepable {

    static boolean TraceSweep;
    static {
        if (MaxineVM.isDebug()) {
            VMOptions.addFieldOption("-XX:", "TraceSweep", Sweepable.class, "Trace heap sweep operations", Phase.PRISTINE);
        }
    }

    static final VMIntOption freeChunkMinSizeOption =
        register(new VMIntOption("-XX:FreeChunkMinSize=", 256,
                        "Minimum size of contiguous space considered for space reclamation." +
                        "Below this size, the space is ignored (dark matter)"),
                        MaxineVM.Phase.PRISTINE);

    /**
     * Invoked when doing precise sweeping on the first black object following the pointer last returned by this method.
     * @param liveObject a pointer to a live cell in the heap
     * @return a pointer to the position in the heap where to resume sweeping.
     */
    public abstract Pointer processLiveObject(Pointer liveObject);

    /**
     * Invoked when doing imprecise sweeping to process an large interval between to marked locations.
     * Imprecise heap sweeping ignores any space before two live objects smaller than a specified amount of space.
     * When the distance between two live marks is large enough to indicate a potentially large chunk of free space,
     * the sweeper invoke this method.
     *
     * @param leftLiveObject
     * @param rightLiveObject
     * @return
     */
    public abstract Pointer processLargeGap(Pointer leftLiveObject, Pointer rightLiveObject);

    /**
     * Invoked to record a known chunk of free space.
     * Used both by precise and imprecise sweeper, typically to record the unmarked space
     * at both end of the traced space.
     * @param freeChunk
     * @param size
     */
    public abstract void processDeadSpace(Address freeChunk, Size size);

    /**
     * Get the space manager ready for a sweep of the space it manages.
     * @param precise indicate whether the sweeping will be precise.
     * @return return the minimum amount of byte the space manager is interested in reclaiming. A heap sweeper may not
     * notify the space manager for free space smaller than this amount.
     */
    public abstract Size beginSweep(boolean precise);

    /**
     * Notify the space manager that sweeping is terminated.
     *
     * @return total free spaces
     */
    public abstract Size endSweep();


    /**
     * @param verifier
     */
    public abstract void verify(AfterMarkSweepVerifier verifier);

}

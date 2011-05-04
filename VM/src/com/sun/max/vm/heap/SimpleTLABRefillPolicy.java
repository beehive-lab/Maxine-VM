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

import com.sun.max.unsafe.*;

/**
 * A policy object that helps with taking decisions with respect to when to refill a tlab on allocation failure, what size the tlab should have on next refill etc....
 * TLABRefillPolicy are stored in a thread local variable TLAB_REFILL_POLICY if tlabs are being used. Threads may refer to the same TLAB policy or
 * to one individual policy if the policy allows each thread to have their TLAB evolves differently.
 *
 * @author Laurent Daynes
 */
public class SimpleTLABRefillPolicy extends TLABRefillPolicy {
    /**
     * Number of allocation failure tolerated per allocation mark.
     * Arbitrarily chosen at the moment. Statistics needed to see if it helps.
     * with limiting fragmentation of TLABs.
     */
    static final int TLAB_NUM_ALLOCATION_FAILURES_PER_MARK = 3;

    /**
     * refillThreshold is computed as TLAB size / refill ratio.
     * A refill ratio of 10 correspond to a refill threshold at 10% of the TLAB size.
     */
    static final int TLAB_REFILL_RATIO = 10;

    /**
     * Size the TLAB should have on next refill.
     */
    private Size nextSize;

    /**
     * Threshold for refilling the TLAB on allocation failure.
     *  When space left in the TLAB is below this threshold, the TLAB is refilled.
     */
    private Size refillThreshold;
    /**
     * Number of allocation failures on the same allocation mark.
     * A number of failures are tolerated on the same allocation mark
     * when the TLAB's used space is below the refill threshold.
     */
    private int allocationFailures;
    /**
     * Last allocation mark where an allocation failure occurred.
     */
    private Pointer lastMark;

    public SimpleTLABRefillPolicy(Size initialTLABSize) {
        lastMark = Pointer.zero();
        allocationFailures = 0;
        nextSize = initialTLABSize;
        refillThreshold = initialTLABSize.dividedBy(TLAB_REFILL_RATIO);
    }

    @Override
    public boolean shouldRefill(Size size, Pointer allocationMark) {
        if (allocationMark.isZero()) {
            // No TLAB. Refill whatsoever
            return true;
        }
        if (size.lessThan(refillThreshold)) {
            // space the TLAB failed to allocate is smaller than the refill threshold.
            // We should definitively refill
            return true;
        }
        // Space left in TLAB is larger than refill ratio. Don't want to refill, unless
        // we had a number of allocation failures for the same allocation mark.
        // In that case, let bite the bullet and get a new TLAB.

        // TODO (ld) a tlab global allocation failure counter is probably better...
        if (!lastMark.equals(allocationMark)) {
            lastMark = allocationMark;
            allocationFailures = 1;
            return false;
        }
        allocationFailures++;
        return allocationFailures > TLAB_NUM_ALLOCATION_FAILURES_PER_MARK;
    }

    @Override
    public Size nextTlabSize() {
        // Currently, nothing fancy. Just always returns the initial TLAB size value.
        return nextSize;
    }

}

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
        if (size.greaterThan(refillThreshold)) {
            return true;
        }
        // FIXME: a tlab global allocation failure counter is probably better...
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

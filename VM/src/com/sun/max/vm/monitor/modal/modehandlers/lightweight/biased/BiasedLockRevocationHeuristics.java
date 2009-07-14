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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.atomic.*;

/**
 *
 * @author Simon Wilkinson
 */
public class BiasedLockRevocationHeuristics {

    enum RevocationType {SINGLE_OBJECT_REVOCATION, BULK_REBIAS, BULK_REVOCATION}

    private static final int BULK_REBIAS_THRESHOLD = 20;
    private static final int BULK_REVOCATION_THRESHOLD = 40;
    private static final int BULK_REBIAS_DECAY_TIME = 25000;

    private final AtomicInteger revocationCount = new AtomicInteger();
    private long lastBulkRebiasTime = 0;

    public RevocationType notifyContentionRevocationRequest() {

        // This heuristic re-implements that used in HotSpot (as of 1.7)

        int currentRevocationCount = revocationCount.get();
        final long bulkRebiasTime = lastBulkRebiasTime;
        final long currentTime = System.currentTimeMillis();
        if (currentRevocationCount >= BULK_REBIAS_THRESHOLD &&
            currentRevocationCount < BULK_REVOCATION_THRESHOLD &&
            lastBulkRebiasTime != 0 &&
            currentTime - bulkRebiasTime > BULK_REBIAS_DECAY_TIME) {
            currentRevocationCount = 0;
            revocationCount.set(0);
        }

        if (currentRevocationCount <= BULK_REVOCATION_THRESHOLD) {
            currentRevocationCount = revocationCountAtomicInc();
        }

        if (currentRevocationCount == BULK_REBIAS_THRESHOLD) {
            return RevocationType.BULK_REBIAS;
        } else if (currentRevocationCount == BULK_REVOCATION_THRESHOLD) {
            return RevocationType.BULK_REVOCATION;
        }
        return RevocationType.SINGLE_OBJECT_REVOCATION;
    }

    public void notifyBulkRebiasComplete() {
        lastBulkRebiasTime = System.currentTimeMillis();
    }

    private int revocationCountAtomicInc() {
        return revocationCount.getAndAdd(1) + 1;
    }
}

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

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.reference.*;

/**
 *
 * @author Simon Wilkinson
 */
public class BiasedLockRevocationHeuristics {

    enum RevocationType {SINGLE_OBJECT_REVOCATION, BULK_REBIAS, BULK_REVOCATION}

    private static final int BULK_REBIAS_THRESHOLD = 20;
    private static final int BULK_REVOCATION_THRESHOLD = 40;
    private static final int BULK_REBIAS_DECAY_TIME = 25000;

    private int _revocationCount = 0;
    private long _lastBulkRebiasTime = 0;

    private static final FieldActor _revocationCountFieldActor = FieldActor.findInstance(BiasedLockRevocationHeuristics.class, "_revocationCount");

    public RevocationType notifyContentionRevocationRequest() {

        // This heuristic re-implements that used in HotSpot (as of 1.7)

        int revocationCount = _revocationCount;
        final long lastBulkRebiasTime = _lastBulkRebiasTime;
        final long currentTime = System.currentTimeMillis();
        if (revocationCount >= BULK_REBIAS_THRESHOLD &&
            revocationCount < BULK_REVOCATION_THRESHOLD &&
            _lastBulkRebiasTime != 0 &&
            currentTime - lastBulkRebiasTime > BULK_REBIAS_DECAY_TIME) {
            revocationCount = 0;
            _revocationCount = 0;
        }

        if (revocationCount <= BULK_REVOCATION_THRESHOLD) {
            revocationCount = revocationCountAtomicInc();
        }

        if (revocationCount == BULK_REBIAS_THRESHOLD) {
            return RevocationType.BULK_REBIAS;
        } else if (revocationCount == BULK_REVOCATION_THRESHOLD) {
            return RevocationType.BULK_REVOCATION;
        }
        return RevocationType.SINGLE_OBJECT_REVOCATION;
    }

    public void notifyBulkRebiasComplete() {
        _lastBulkRebiasTime = System.currentTimeMillis();
    }

    private int revocationCountAtomicInc() {
        while (true) {
            final Address revocationCount = Address.fromInt(_revocationCount);
            final Address newRevocationCount = revocationCount.plus(1);
            if (Reference.fromJava(this).compareAndSwapWord(_revocationCountFieldActor.offset(), revocationCount, newRevocationCount).equals(revocationCount)) {
                return newRevocationCount.toInt();
            }

        }
    }
}

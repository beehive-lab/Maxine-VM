/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.monitor.modal.modehandlers.lightweight.biased;

import com.sun.max.atomic.*;

/**
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

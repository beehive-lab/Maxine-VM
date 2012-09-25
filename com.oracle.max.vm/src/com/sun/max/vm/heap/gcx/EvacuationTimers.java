/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.annotate.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.heap.*;


public final class EvacuationTimers {
    public enum TIMED_OPERATION {
        TOTAL,
        PROLOGUE,
        ROOT_SCAN,
        BOOT_HEAP_SCAN,
        CODE_SCAN,
        IMMORTAL_SCAN,
        RSET_SCAN,
        COPY,
        WEAK_REF,
        EPILOGUE;
        public final TimerMetric timer = new TimerMetric(new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK));
    }

    private boolean trackTime = false;

    public EvacuationTimers() {
    }

    public void resetTrackTime() {
        trackTime = Heap.logGCTime();
    }

    @INLINE
    public TimerMetric get(TIMED_OPERATION timedOp) {
        return timedOp.timer;
    }

    public void start(TIMED_OPERATION timedOp) {
        if (trackTime) {
            timedOp.timer.start();
        }
    }

    public void stop(TIMED_OPERATION timedOp) {
        if (trackTime) {
            timedOp.timer.stop();
        }
    }
}

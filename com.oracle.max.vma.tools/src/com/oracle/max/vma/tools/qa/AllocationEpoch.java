/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa;

/**
 * Information on an allocation epoch derived from GC events in the log.
 * An epoch is defined by its start and end time, which allows
 * another event that references an object id to find the right epoch,
 * and hence the qualifier to create a unique id for an object created within that epoch.
 *
 */
public class AllocationEpoch {
    /**
     * Start of the epoch.
     */
    public final long startTime;
    /**
     * End of the epoch.
     */
    public long endTime;
    /**
     * The unique id assigned to the epoch.
     */
    public final int epoch;

    /**
     * Indices into the {@link TraceRun#adviceRecordList} enumerating the objects removed at the end of this allocation
     * epoch by GC.
     */
    public static class RemovalRange {

        public final int startRemovalRange;
        public final int endRemovalRange;

        private RemovalRange(int first, int last) {
            this.startRemovalRange = first;
            this.endRemovalRange = last;
        }
    }

    private RemovalRange removalRange;

    private static int nextEpoch;

    public AllocationEpoch(long startTime) {
        this.startTime = startTime;
        this.epoch = nextEpoch++;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setRemovalRange(int first, int last) {
        removalRange = new RemovalRange(first, last);
    }

    /**
     * Gets the {@link RemovalRange} for this epoch or null if no GC occurred (i.e. the last epoch).
     */
    public RemovalRange getRemovalRange() {
        return removalRange;
    }

    @Override
    public String toString() {
        return toString(startTime, endTime);
    }

    public String toString(long start, long end) {
        return "Epoch " + epoch + ", " + TimeFunctions.formatTime(start) + ", " + TimeFunctions.formatTime(end);
    }

}

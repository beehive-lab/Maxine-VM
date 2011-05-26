/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Information on a GC epoch derived from GC events in the log.
 * An epoch is defined by its start and end time, which allows
 * another event that references an object id to find the right epoch,
 * and hence qualifier to create a unique id.
 *
 * @author Mick Jordan
 *
 */
public class GCEpoch {
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

    private static int nextEpoch;

    public GCEpoch(long startTime) {
        this.startTime = startTime;
        this.epoch = nextEpoch++;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return epoch + ", " + TimeFunctions.formatTime(startTime) + ", " + TimeFunctions.formatTime(endTime);
    }

}

/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */

package com.sun.max.vm.profilers.dynamic;

import com.sun.max.unsafe.Size;
import com.sun.max.vm.Log;
import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.thread.VmThread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Profiler {

    /**
     * Histogram: the data structure that stores the profiling outcome.
     */
    public Map<Size, Integer> histogram;


    public Profiler() {
        histogram = new ConcurrentHashMap<Size, Integer>();
    }


    /**
     * Methods to manage the histogram.
     */

    /**
     * Records the object with that size.
     * If that size has never been recorded again, records a new entry.
     * Else it increments the number of the equal-size allocated objects.
     * @param size
     */
    public void record(Size size) {
        //Log.println("Size=" + size.toLong() + " Bytes, ThreadId=" + VmThread.current().id());
        if (!histogram.containsKey(size)) {
            histogram.put(size, 1);
        } else {
            histogram.put(size, histogram.get(size) + 1);
        }

    }

    /**
     * This method is called when a profiled object is allocated.
     *
     * */
    public void profile(Size size) {
        // if the thread local profiling flag is enabled
        if (!VmThread.current().PROFILE) {
            if (MaxineVM.isRunning()) {
                record(size);
            }
        }
    }

    /**
     * Dump the histogram.
     */
    public void dumpHistogram() {
        Log.println(histogram);
    }
}

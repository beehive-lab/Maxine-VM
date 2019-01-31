/*
 * Copyright (c) 2018, 2019, APT Group, School of Computer Science,
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
package com.sun.max.vm.profilers.allocation;

import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.heap.Heap;

/**
 * A class that implements Heap.GCCallback for Allocation Profiler.
 * This mechanism enables the profiler callbacks BEFORE and AFTER a gc.
 * As every GCCallback implementation, it needs to be registered during BOOTSTRAPPING phase.
 */
public class ProfilerGCCallback implements Heap.GCCallback {

    public static void init() {
        Heap.registerGCCallback(new ProfilerGCCallback());
    }

    @Override
    public void gcCallback(Heap.GCCallbackPhase gcCallbackPhase) {

        if (gcCallbackPhase == Heap.GCCallbackPhase.BEFORE) {
            if (MaxineVM.isRunning() && MaxineVM.isAllocationProfilerInitialized) {
                //any PRE gc action must be placed here
                MaxineVM.allocationProfiler.preGCActions();
            }
        } else if (gcCallbackPhase == Heap.GCCallbackPhase.AFTER) {
            if (MaxineVM.isRunning() && MaxineVM.isAllocationProfilerInitialized) {
                //any POST gc action must be placed here
                MaxineVM.allocationProfiler.postGCActions();
            }
        }

    }


}

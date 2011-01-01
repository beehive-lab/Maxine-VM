/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.management;

import static com.sun.max.vm.VMConfiguration.*;

import java.lang.management.*;
import java.util.*;

import com.sun.max.vm.code.*;
import com.sun.max.vm.heap.*;

/**
 * This class provides the entry point to all the memory management functions in Maxine.
 *
 * @author Mick Jordan
 */

public class MemoryManagement {

    public static MemoryPoolMXBean[] getMemoryPools() {
        /*
         * It is somewhat annoying that a MemoryManagerMXBean only provides access to the names
         * of its managed pools, not the pool instances themselves. So Maxine uses subclasses
         * of MemoryManagerMXBean and GarbageCollectorMXBean that implement the
         * MemoryManagerMXBeanPools interface, which provides this information.
         */
        final MemoryManagerMXBean[] theMemoryManagers = getMemoryManagers();
        final ArrayList<MemoryPoolMXBean> theMemoryPoolMXBeans = new ArrayList<MemoryPoolMXBean>();
        for (MemoryManagerMXBean memoryManagerMXBean : theMemoryManagers) {
            MemoryManagerMXBeanPools memoryManagerMXBeanPools = (MemoryManagerMXBeanPools) memoryManagerMXBean;
            theMemoryPoolMXBeans.addAll(memoryManagerMXBeanPools.getAll());
        }
        return theMemoryPoolMXBeans.toArray(new MemoryPoolMXBean[theMemoryPoolMXBeans.size()]);
    }

    public static MemoryManagerMXBean[] getMemoryManagers() {
        /*
         * In a complete implementation there would be a manager for code, non-heap data and heap data.
         * Currently, we only support code and heap. This information probably could be cached safely.
         */
        final MemoryManagerMXBean[] result = new MemoryManagerMXBean[3];
        result[0] = Code.getMemoryManagerMXBean();
        result[1] = ImmortalHeap.getMemoryManagerMXBean();
        result[2] = vmConfig().heapScheme().getGarbageCollectorMXBean();
        return result;
    }

    public static MemoryUsage getMemoryUsage(boolean heap) {
        List<MemoryPoolMXBean> pools = null;
        if (heap) {
            pools = new ArrayList<MemoryPoolMXBean>();
            pools.addAll(getMemoryManagerMXBeanPools(ImmortalHeap.getMemoryManagerMXBean()).getAll());
            pools.addAll(getMemoryManagerMXBeanPools(vmConfig().heapScheme().getGarbageCollectorMXBean()).getAll());
        } else {
            pools = getMemoryManagerMXBeanPools(Code.getMemoryManagerMXBean()).getAll();
        }
        return sum(pools);
    }

    private static MemoryManagerMXBeanPools getMemoryManagerMXBeanPools(MemoryManagerMXBean memoryManagerMXBean) {
        return (MemoryManagerMXBeanPools) memoryManagerMXBean;
    }

    private static MemoryUsage sum(List<MemoryPoolMXBean> pools) {
        long init = 0;
        long committed = 0;
        long max = 0;
        long used = 0;

        for (MemoryPoolMXBean pool : pools) {
            final MemoryUsage poolUsage = pool.getUsage();
            init += poolUsage.getInit();
            committed += poolUsage.getCommitted();
            max += poolUsage.getMax();
            used += poolUsage.getUsed();
        }
        return new MemoryUsage(init, used, committed, max);
    }

    public static boolean setVerboseGC(boolean value) {
        final boolean result = Heap.verbose();
        Heap.setVerbose(value);
        return result;
    }
}

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
package com.sun.max.vm.management;

import java.lang.management.*;
import java.util.*;

import com.sun.max.vm.*;
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
        result[2] = VMConfiguration.target().heapScheme().getGarbageCollectorMXBean();
        return result;
    }

    public static MemoryUsage getMemoryUsage(boolean heap) {
        List<MemoryPoolMXBean> pools = null;
        if (heap) {
            pools = new ArrayList<MemoryPoolMXBean>();
            pools.addAll(getMemoryManagerMXBeanPools(ImmortalHeap.getMemoryManagerMXBean()).getAll());
            pools.addAll(getMemoryManagerMXBeanPools(VMConfiguration.target().heapScheme().getGarbageCollectorMXBean()).getAll());
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

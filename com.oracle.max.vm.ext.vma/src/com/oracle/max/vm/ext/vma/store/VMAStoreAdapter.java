/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store;

import java.util.*;

import com.oracle.max.vm.ext.vma.store.txt.*;
import com.sun.max.vm.*;
import com.sun.max.vm.thread.*;

/**
 * Support for per-thread {@link VMAStore store adapters} of various types.
 *
 * Per-thread adapters are supported in {@link #perThread per-thread} mode. In this case each adapter has its
 * own {@link VMATextStore} instance, avoiding any synchronization in the storing process. The caller
 * must announce new threads via the {@link #newThread} method  and subsequently use the returned
 * {@link VMAStoreAdapter} when adapting records for that thread.
 *
*/
public abstract class VMAStoreAdapter implements VMAStore.PerThreadStoreOwner {

    private static class StoreIterator implements Iterator<VMAStore> {
        private int index;
        private int count;

        @Override
        public boolean hasNext() {
            return count < storeAdaptersCount;
        }

        @Override
        public VMAStore next() {
            if (count < storeAdaptersCount) {
                while (index < storeAdapters.length) {
                    VMAStoreAdapter sa = storeAdapters[index++];
                    if (sa != null) {
                        count++;
                        return sa.getStore();
                    }
                }
                assert false;
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    protected static VMAStoreAdapter[] storeAdapters;

    protected static int storeAdaptersCount;

    /**
     * Handle to the store instance.
     */
    protected VMAStore store;

    /**
     * Denotes whether the log records are batched per thread.
     */
    protected final boolean threadBatched;

    /**
     * {@code true} when there are per thread adapters.
     */
    protected final boolean perThread;

    /**
     * Non-null when per-thread adapters, the associated thread.
     */
    protected VmThread vmThread;


    protected abstract VMAStoreAdapter[] createArray(int length);

    /**
     * Create a new store adapter for given thread, and the associated {@link VMAStore}.
     * @param vmThread
     * @return
     */
    protected abstract VMAStoreAdapter createThreadStoreAdapter(VmThread vmThread);

    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.RUNNING) {
            storeAdapters = createArray(16);
        }
    }

    protected VMAStoreAdapter(boolean threadBatched, boolean perThread) {
        this.threadBatched = threadBatched;
        this.perThread = perThread;
    }

    protected VMAStoreAdapter(VmThread vmThread, VMAStore store) {
        this(true, true);
        this.vmThread = vmThread;
        this.store = store;
    }

    public VMAStore getStore() {
        return store;
    }

    /**
     * Returns the adapter for the given thread (id).
     * N.B. {@link #newThread} must have been called previously for the thread.
     * @param vmThreadId
     * @return
     */
    public VMAStoreAdapter getStoreAdaptorForThread(int vmThreadId) {
        if (perThread) {
            return storeAdapters[vmThreadId];
        } else {
            return this;
        }
    }

    /**
     * In per-thread mode must be called to notify the start of a new thread, typically from
     * {@code adviseBeforeThreadStarting}, but certainly before any {@code adviseXXX} methods of this class are called.
     *
     * @param vmThread
     * @return in per-thread mode a per-thread adaptor, else {@code this}.
     */
    public VMAStoreAdapter newThread(VmThread vmThread) {
        if (perThread) {
            int id = vmThread.uuid;
            synchronized (this) {
                if (id >= storeAdapters.length) {
                    VMAStoreAdapter[] newStoreAdaptors = createArray(2 * storeAdapters.length);
                    System.arraycopy(storeAdapters, 0, newStoreAdaptors, 0, storeAdapters.length);
                    storeAdapters = newStoreAdaptors;
                }
                VMAStoreAdapter sa = createThreadStoreAdapter(vmThread);
                assert storeAdapters[id] == null;
                storeAdapters[id] = sa;
                storeAdaptersCount++;
                return sa;
            }
        } else {
            return this;
        }
    }

    @Override
    public Iterator<VMAStore> getThreadStores() {
        return new StoreIterator();
    }

    /**
     * Must be called in {@link #threadBatched} mode to indicate records now for given thread.
     * @param time
     * @param vmThread
     */
    public void threadSwitch(long time, VmThread vmThread) {
        if (!perThread) {
            store.threadSwitch(time, vmThread.getName());
        }
    }


}

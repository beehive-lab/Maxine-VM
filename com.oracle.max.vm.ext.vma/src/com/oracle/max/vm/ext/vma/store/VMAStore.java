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

/**
 * Denotes a persistent store for VMA records. No implementation is prescribed, but support for per-thread stores is
 * defined here.
 *
 * The interface provides explicit support for per-thread stores through the {@code threadBatched} and {@code perThread}
 * arguments to {@link #initializeStore}. If both of these values are {@code false} then the assumption is that calls
 * from multiple threads are interleaved. If {@code threadBatched} is {@code true} then calls are batched from the same
 * thread and each batch must be preceded by a call to {@link #threadSwitch}. If {@code perThread} is {@code true} then
 * the store implementation is expected to support per-thread stores. The caller must announce new threads via the
 * {@link #newThread} method and subsequently use the returned {@VMATextStore} instance when storing
 * records for that thread. In {@code perThread} mode, the caller of {@link #initializeStore} must pass
 * a {@link PerThreadStoreOwner} and be prepared to provide the store returned from {@link #newThread} on request.
 * I.e., a store implementation is <i>not</i> required to manage the set of per-thread stores; this must be
 * supported by the store owner.
 *
 * Since the thread is implicit in per-thread or batched mode, it is permissible to pass
 * {@code null} for the {@code threadName} argument in the advice methods.
 *
 *
 *
 */
public abstract class VMAStore implements VMAIdStore {

    public interface PerThreadStoreOwner {
        /**
         * Returns an iterator for the per-thread stores.
         * Caller should synchronize on the {@code PerThreadStoreOwner} for the iteration.
         */
        Iterator<VMAStore> getThreadStores();
    }

    /**
     * Initialize the store subsystem.
     * @param threadBatched {@code true} thread records are batched together and not interleaved
     * @param perThread a request for per thread output store if possible
     * @param storeOwner TODO
     * @return {@code true} iff the initialization was successful.
     */
    public abstract boolean initializeStore(boolean threadBatched, boolean perThread, PerThreadStoreOwner storeOwner);

    /**
     * Finalize the store, e.g. flush trace.
     */
    public abstract void finalizeStore();

    /**
     * In per-thread mode must be called notify the start of a new thread, typically
     * from {@code adviseBeforeThreadStarting}, but certainly before any {@code adviseXXX} methods
     * of this called are called.
     * For per-thread stores may return a per-thread specific store that should be
     * used instead of {@code this}.
     */
    public abstract VMAStore newThread(String threadName);

    /**
     * Must be called if {@code threadBatched} is {@code true} and {@link perThread} is {@code false},
     *  to indicate records now for given thread.
     */
    public abstract void threadSwitch(long time, String threadName);

    /**
     * Log the removal on an object from the VM (i.e. object death).
     * @param id
     */
    public abstract void removal(long id);


}

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
package com.sun.max.memory;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.compiler.builtin.*;

/**
 * A memory instruction ordering barrier.
 *
 * The memory access relationships offered here
 * map directly to SPARC 'membar' instruction operands.
 *
 * On other systems, an instruction that enforces at least
 * as strong ordering as the selected one will be emitted.
 * For example, on IA32/AMD64:
 *
 *     LOAD_LOAD     -> LFENCE
 *     STORE_STORE   -> SFENCE
 *     anything else -> MFENCE
 *
 * Note that the code generator may omit emitting memory barriers
 * that are implied by the target memory model.
 *
 * @see MemoryModel
 *
 * @author Bernd Mathiske
 */
public enum MemoryBarrier implements PoolObject {

    /**
     * All loads before subsequent loads will be serialized.
     */
    LOAD_LOAD,

    /**
     * All loads before subsequent stores will be serialized.
     */
    LOAD_STORE,

    /**
     * All stores before subsequent loads will be serialized.
     */
    STORE_LOAD,

    /**
     * All stores and loads before subsequent loads will be serialized.
     */
    MEMOP_STORE,

    /**
     * All stores before subsequent stores will be serialized.
     */
    STORE_STORE;

    public static final Pool<MemoryBarrier> VALUE_POOL = new ArrayPool<MemoryBarrier>(values());

    public int serial() {
        return ordinal();
    }

    @BUILTIN(builtinClass = SpecialBuiltin.BarMemory.class)
    private static void barMemory(PoolSet<MemoryBarrier> relations) {
    }

    private static final PoolSet<MemoryBarrier> loadLoad = PoolSet.of(VALUE_POOL, MemoryBarrier.LOAD_LOAD);

    /**
     * Ensures all preceding loads complete before any subsequent loads.
     */
    @INLINE
    public static void loadLoad() {
        barMemory(loadLoad);
    }

    private static final PoolSet<MemoryBarrier> loadStore = PoolSet.of(VALUE_POOL, MemoryBarrier.LOAD_STORE);

    /**
     * Ensures all preceding loads complete before any subsequent stores.
     */
    @INLINE
    public static void loadStore() {
        barMemory(loadStore);
    }

    private static final PoolSet<MemoryBarrier> storeLoad = PoolSet.of(VALUE_POOL, MemoryBarrier.STORE_LOAD);

    /**
     * Ensures all preceding stores complete before any subsequent loads.
     */
    @INLINE
    public static void storeLoad() {
        barMemory(storeLoad);
    }

    private static final PoolSet<MemoryBarrier> storeStore = PoolSet.of(VALUE_POOL, MemoryBarrier.STORE_STORE);

    /**
     * Ensures all preceding stores complete before any subsequent stores.
     */
    @INLINE
    public static void storeStore() {
        barMemory(storeStore);
    }

    private static final PoolSet<MemoryBarrier> memopStore = PoolSet.of(VALUE_POOL, MemoryBarrier.STORE_STORE, MemoryBarrier.LOAD_STORE);

    /**
     * Ensures all preceding stores and loads complete before any subsequent stores.
     */
    @INLINE
    public static void memopStore() {
        barMemory(memopStore);
    }

    private static final PoolSet<MemoryBarrier> all = PoolSet.allOf(VALUE_POOL);

    /**
     * Ensures all preceding stores and loads complete before any subsequent stores and loads.
     */
    @INLINE
    public static void all() {
        barMemory(all);
    }
}

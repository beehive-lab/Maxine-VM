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

import static com.sun.max.memory.MemoryBarrier.*;

import com.sun.max.collect.*;

/**
 * SMP memory models.
 *
 * @author Bernd Mathiske
 */
public enum MemoryModel {
    SequentialConsistency(LOAD_LOAD, LOAD_STORE, STORE_LOAD, STORE_STORE),
    TotalStoreOrder(LOAD_LOAD, LOAD_STORE, STORE_STORE),
    AMD64(LOAD_STORE, STORE_STORE),
    PartialStoreOrder(LOAD_LOAD),
    RelaxedMemoryOrder();

    private final PoolSet<MemoryBarrier> _barriers;

    public PoolSet<MemoryBarrier> barriers() {
        return _barriers;
    }

    /**
     * @param barriers the barriers that are implied everywhere in the code by this memory model
     */
    private MemoryModel(MemoryBarrier... barriers) {
        _barriers = PoolSet.of(MemoryBarrier.VALUE_POOL, barriers);
    }

}

/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.memory;

import static com.sun.cri.bytecode.Bytecodes.MemoryBarriers.*;

import com.sun.cri.bytecode.Bytecodes.*;

/**
 * SMP memory models.
 */
public enum MemoryModel {
    SequentialConsistency(LOAD_LOAD | LOAD_STORE | STORE_LOAD | STORE_STORE),
    TotalStoreOrder(LOAD_LOAD | LOAD_STORE | STORE_STORE),
    AMD64(LOAD_STORE | STORE_STORE),
    PartialStoreOrder(LOAD_LOAD),
    RelaxedMemoryOrder(0);

    /**
     * Mask of {@linkplain MemoryBarriers memory barrier} flags denoting the barriers that
     * are not required to be explicitly inserted under this memory model.
     */
    public final int impliedBarriers;

    /**
     * @param barriers the barriers that are implied everywhere in the code by this memory model
     */
    private MemoryModel(int barriers) {
        this.impliedBarriers = barriers;
    }
}

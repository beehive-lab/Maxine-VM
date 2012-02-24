/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;

/**
 * A more elaborated refill manager for linear space allocator.
 * It implements a refill policy that decide whether refill is warranted. If denied, the allocator
 * must issue an overflow allocation request, otherwise it issue a refill request.
 * Additionally, a method for handling large allocation requests that should never be handled
 * by the linear space allocator is provided.
 */
public abstract class RefillManager extends Refiller {
    /**
     * Called directly by the allocator if there isn't enough
     * space left to satisfy request and the refill manager rejected the refill.
     *
     * @param size number of bytes requested
     * @return the address to a contiguous region of the requested size
     */
    public abstract Address allocateOverflow(Size size);

    /**
     * Called directly by the allocator if the requested size is larger than its maximum size limit.
     * @param size number of bytes requested
     * @return the address to a contiguous region of the requested size
     */
    public abstract Address allocateLarge(Size size);

    /**
     * Tell whether the amount of space left warrants a refill.
     * @param requestedSpace initial space requested
     * @param spaceLeft space left in the allocator requesting refill
     *
     * @return a boolean indicating whether refill should be done
     */
    public abstract boolean shouldRefill(Size requestedSpace, Size spaceLeft);
}

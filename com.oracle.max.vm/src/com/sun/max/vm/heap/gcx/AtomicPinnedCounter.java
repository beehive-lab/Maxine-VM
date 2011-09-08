/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * Simple debug  utility class to atomically count pin/unpin requests and track unbalanced pinned requests.
 */
public final class AtomicPinnedCounter {
    private volatile int pinnedCounter = 0;
    private static final int pinnedCounterOffset = ClassActor.fromJava(AtomicPinnedCounter.class).findLocalInstanceFieldActor("pinnedCounter").offset();

    public void increment() {
        int newValue;
        int oldValue;
        do {
            oldValue = pinnedCounter;
            FatalError.check(oldValue >= 0, "Unbalance pinned request");
            newValue  = oldValue + 1;
        } while (Reference.fromJava(this).compareAndSwapInt(pinnedCounterOffset, oldValue, newValue) == newValue);
    }

    public void decrement() {
        int newValue;
        int oldValue;
        do {
            oldValue = pinnedCounter;
            FatalError.check(oldValue > 0, "Unbalance pinned request");
            newValue  = oldValue - 1;
        } while (Reference.fromJava(this).compareAndSwapInt(pinnedCounterOffset, oldValue, newValue) == newValue);
    }
}

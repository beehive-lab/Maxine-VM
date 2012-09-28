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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.tele.reference.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;


/**
 * @see BaseAtomicBumpPointerAllocator
 */
public class TeleBaseAtomicBumpPointerAllocator extends TeleTupleObject {

    Address start = Address.zero();
    Address end = Address.zero();
    Address top = Address.zero();

    public TeleBaseAtomicBumpPointerAllocator(TeleVM vm, RemoteReference reference) {
        super(vm, reference);
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        start = vm().fields().BaseAtomicBumpPointerAllocator_start.readWord(reference()).asAddress();
        end = vm().fields().BaseAtomicBumpPointerAllocator_end.readWord(reference()).asAddress();
        top = vm().fields().BaseAtomicBumpPointerAllocator_top.readWord(reference()).asAddress();
        return true;
    }

    /**
     * @see BaseAtomicBumpPointerAllocator#start()
     */
    public Address start() {
        return start;
    }

    /**
     * @see BaseAtomicBumpPointerAllocator#end()
     */
    public Address end() {
        return end;
    }

    /**
     * @see BaseAtomicBumpPointerAllocator
     */
    public Address top() {
        return top;
    }

    /**
     * Determines whether an address is in the allocated portion of the memory region served by the sub-class of {@link BaseAtomicBumpPointerAllocator}
     * described by this {@link TeleBaseAtomicBumpPointerAllocator}.
     */
    public boolean containsInAllocated(Address address) {
        return start.lessEqual(address) && top.greaterThan(address);
    }

    /**
     * Determines whether an address is in the memory region the sub-class of {@link BaseAtomicBumpPointerAllocator}
     * described by this {@link TeleBaseAtomicBumpPointerAllocator} allocates memory from.
     */
    public boolean contains(Address address) {
        return start.lessEqual(address) && end.greaterThan(address);
    }
}

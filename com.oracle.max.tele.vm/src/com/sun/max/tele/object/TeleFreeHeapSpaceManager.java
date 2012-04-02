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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.heap.gcx.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a free heap space manager object in the VM.
 *
 * @see FreeHeapSpaceManager
 */
public class TeleFreeHeapSpaceManager extends TeleTupleObject {

    private static final int TRACE_VALUE = 1;

    /**
     * The heap's memory; final field.
     */
    private TeleContiguousHeapSpace committedHeapSpace = null;

    public TeleFreeHeapSpaceManager(TeleVM vm, Reference freeHeapSpaceManagerReference) {
        super(vm, freeHeapSpaceManagerReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (committedHeapSpace == null) {
            final Reference contiguousHeapSpaceRef = fields().FreeHeapSpaceManager_committedHeapSpace.readReference(reference());
            if (!contiguousHeapSpaceRef.isZero()) {
                committedHeapSpace = (TeleContiguousHeapSpace) objects().makeTeleObject(contiguousHeapSpaceRef);
                Trace.line(TRACE_VALUE, tracePrefix() + "contiguousHeapSpace now allocated");
            }
        }
        return true;
    }

    public TeleContiguousHeapSpace contiguousHeapSpace() {
        return committedHeapSpace;
    }

}

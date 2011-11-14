/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.vm.code.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for a {@link SemiSpaceCodeRegion} object in the VM, which describes a
 * managed VM memory region that is used to allocate compiled code.
 *
 * @see SemiSpaceCodeRegion
 */

public final class TeleSemiSpaceCodeRegion extends TeleCodeRegion {

    private final Object localStatsPrinter = new Object() {

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("evictions=(").append(evictionStartedCount);
            sb.append(",").append(evictionCompletedCount).append(")");
            if (isInEviction()) {
                sb.append(" in EVICTION");
            }
            return sb.toString();
        }
    };

    private long lastEvictionCompletedCount = 0;

    public TeleSemiSpaceCodeRegion(TeleVM vm, Reference codeRegionReference) {
        super(vm, codeRegionReference);
    }

    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (isInEviction()) {
            System.out.println(tracePrefix() + ": " + getRegionName() + " in EVICTION");
        }
        if (lastEvictionCompletedCount < evictionCompletedCount) {
            System.out.println(tracePrefix() + ": " + getRegionName() + " EVICTION COMPLETED");
            lastEvictionCompletedCount = evictionCompletedCount;
        }
        statsPrinter.addStat(localStatsPrinter);
        return true;
    }

    @Override
    public boolean isManaged() {
        return true;
    }

}

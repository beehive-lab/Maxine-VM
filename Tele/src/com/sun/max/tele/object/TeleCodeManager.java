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

import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;

/**
 * Access to the singleton {@link CodeManager} in the VM.
 *
 * @author Michael Van De Vanter
 * @author Hannes Payer
 */
public final class TeleCodeManager extends TeleTupleObject {

    private static final int TRACE_VALUE = 1;

    @Override
    protected String  tracePrefix() {
        return "[TeleCodeManager] ";
    }

    private TeleCodeRegion teleBootCodeRegion = null;

    /**
     * Access to the runtime (dynamic) code region created by the {@link CodeManager} in the VM.
     * <br>
     * Assume that the region is created at startup, and that its identity doesn't change, just the
     * address when memory is allocated for it.
     */
    private TeleCodeRegion teleRuntimeCodeRegion = null;

    TeleCodeManager(TeleVM vm, Reference codeManagerReference) {
        super(vm, codeManagerReference);
    }

    /**
     * Lazy initialization; try to keep data reading out of constructor.
     */
    private void initialize() {
        if (teleBootCodeRegion == null) {
            assert vm().lockHeldByCurrentThread();
            Trace.begin(TRACE_VALUE, tracePrefix() + "initializing");
            final long startTimeMillis = System.currentTimeMillis();
            final Reference bootCodeRegionReference = vm().teleFields().Code_bootCodeRegion.readReference(vm());
            teleBootCodeRegion = (TeleCodeRegion) heap().makeTeleObject(bootCodeRegionReference);

            final Reference runtimeCodeRegionReference = vm().teleFields().CodeManager_runtimeCodeRegion.readReference(vm());
            teleRuntimeCodeRegion = (TeleCodeRegion) heap().makeTeleObject(runtimeCodeRegionReference);

            teleBootCodeRegion.updateCache(vm().teleProcess().epoch());
            teleRuntimeCodeRegion.updateCache(vm().teleProcess().epoch());

            Trace.end(TRACE_VALUE, tracePrefix() + "initializing, contains BootCodeRegion and RuntimeCodeRegion", startTimeMillis);
        }
    }

    /**
     * @return access to the special {@link CodeRegion} in the {@link BootImage} of the VM.
     */
    public TeleCodeRegion teleBootCodeRegion() {
        initialize();
        return teleBootCodeRegion;
    }

    /**
     * @return access to the special Runtime {@link CodeRegion} of the VM.
     */
    public TeleCodeRegion teleRuntimeCodeRegion() {
        initialize();
        return teleRuntimeCodeRegion;
    }

}

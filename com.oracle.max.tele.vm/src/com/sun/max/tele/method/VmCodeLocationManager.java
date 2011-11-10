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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;

// TODO (mlvdv)  just a stub for now; will replace some older stuff
/**
 * The singleton manager for representations of code locations in the VM.
 * <p>
 * This implementation is incomplete.
 */
public class VmCodeLocationManager extends AbstractVmHolder implements TeleVMCache {

    private static final int TRACE_VALUE = 1;

    private static VmCodeLocationManager vmCodeLocationManager;

    public static VmCodeLocationManager make(TeleVM vm) {
        if (vmCodeLocationManager == null) {
            vmCodeLocationManager = new VmCodeLocationManager(vm);
        }
        return vmCodeLocationManager;
    }

    private long lastUpdateEpoch = -1L;


    public VmCodeLocationManager(TeleVM vm) {
        super(vm);
    }

    public void updateCache(long epoch) {
        lastUpdateEpoch = epoch;
    }

    // TODO (mlvdv)  Can we now enforce the precondition that we know about the region containing the address?
    // this might be an issue of startup timing, or more likely about native code.
    /**
     * Creates a code location in VM specified as the memory address of a compiled machine code instruction.
     * <p>
     * Thread-safe
     *
     * @param address a non-zero address in VM memory that represents the beginning of a compiled machine code instruction
     * @param description a human-readable description, suitable for a menu or for debugging
     * @return a newly created location
     * @throws TeleError if the address is null or zero
     */
    public MachineCodeLocation createMachineCodeLocation(Address address, String description) throws TeleError {
//        if (vm().codeCache() != null) {
//            final VmCodeCacheRegion codeCacheRegion = vm().codeCache().findCompiledCodeRegion(address);
//            if (codeCacheRegion != null) {
//
//            }
//        }

        return null;
    }



}

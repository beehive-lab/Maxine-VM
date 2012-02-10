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
package com.sun.max.tele.debug;

import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.log.nat.thread.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;


public class TeleThreadVMLog extends AbstractVmHolder implements MaxThreadVMLog {

    private static class VMLogMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxThreadVMLog> {
        private static final List<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY = Collections.emptyList();

        private MaxThreadVMLog owner;

        protected VMLogMemoryRegion(MaxVM vm, MaxThreadVMLog owner, String regionName, Address start, int nBytes) {
            super(vm, regionName, start, nBytes);
            this.owner = owner;
        }

        public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
            return null;
        }

        public List<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
            return EMPTY;
        }

        public MaxThreadVMLog owner() {
            return owner;
        }

    }

    private VMLogMemoryRegion vmLogMemoryRegion;
    private TeleNativeThread teleNativeThread;
    private final String entityName;
    private final String entityDescription;
    MaxThreadLocalVariable bufOffsets;
    private int lastId;

    public TeleThreadVMLog(TeleVM vm, TeleNativeThread teleNativeThread) {
        super(vm);
        this.teleNativeThread = teleNativeThread;
        this.entityName = "VM log for Thread-" + teleNativeThread.localHandle();
        this.entityDescription = "VM log for the thread named " + teleNativeThread.entityName();
    }

    public void updateCache(long epoch) {
        if (vmLogMemoryRegion == null) {
            MaxThreadLocalsArea tla = teleNativeThread.localsBlock().tlaFor(SafepointPoll.State.ENABLED);
            if (tla != null) {
                for (VmThreadLocal vmtl : tla.values()) {
                    if (vmtl.name.equals(VMLogNativeThread.VMLOG_BUFFER_NAME)) {
                        MaxThreadLocalVariable tlaBuf = tla.getThreadLocalVariable(vmtl.index);
                        Address addr = tlaBuf.value().toWord().asAddress();
                        if (addr.isNotZero()) {
                            int size = vm().fields().VMLogNative_logSize.readInt(vm().fields().VMLog_vmLog.readReference(vm()));
                            vmLogMemoryRegion = new VMLogMemoryRegion(vm(), this, entityName + " VMLog", addr, size);
                        }
                    } else if (vmtl.name.equals(VMLogNativeThread.VMLOG_BUFFER_OFFSETS_NAME)) {
                        bufOffsets = tla.getThreadLocalVariable(vmtl.index);
                    }
                }
            }
        }
    }

    public int firstOffset() {
        return (int) ((bufOffsets.value().toLong() >> VMLogNativeThread.FIRST_OFFSET_SHIFT) & VMLogNativeThread.SHIFTED_FIRST_OFFSET_MASK);
    }

    public int nextOffset() {
        return (int) (bufOffsets.value().toLong() & VMLogNativeThread.NEXT_OFFSET_MASK);
    }

    @Override
    public String entityName() {
        return entityName;
    }

    @Override
    public String entityDescription() {
        return entityDescription;
    }

    @Override
    public boolean contains(Address address) {
        return vmLogMemoryRegion.contains(address);
    }

    @Override
    public TeleObject representation() {
        return null;
    }

    @Override
    public MaxEntityMemoryRegion<MaxThreadVMLog> memoryRegion() {
        return vmLogMemoryRegion;
    }

    @Override
    public MaxThread thread() {
        return teleNativeThread;
    }

    @Override
    public int size() {
        return (int) vmLogMemoryRegion.nBytes();
    }

    @Override
    public Address start() {
        return vmLogMemoryRegion.start();
    }

}

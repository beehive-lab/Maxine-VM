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
package com.sun.max.tele.heap;

import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.tele.TeleVM.InitializationListener;
import com.sun.max.tele.object.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.sequential.gen.semiSpace.*;
import com.sun.max.vm.reference.*;

/**
 * Inspector support for working with VM sessions using the VM's simple
* {@linkplain GenSSHeapScheme generational collector},
* an implementation of the VM's {@link HeapScheme} interface.
* WORK IN PROGRESS.
*/
public final class RemoteGenSSHeapScheme extends AbstractRemoteHeapScheme implements RemoteObjectReferenceManager {
    private static final int TRACE_VALUE = 1;
    private TeleGenSSHeapScheme scheme;
    private final List<VmHeapRegion> heapRegions = new ArrayList<VmHeapRegion>(5);
    private final TimedTrace heapUpdateTracer;
    private long lastUpdateEpoch = -1L;
    private long lastAnalyzingPhaseCount = 0L;
    private long lastReclaimingPhaseCount = 0L;

    public RemoteGenSSHeapScheme(TeleVM vm) {
        super(vm);
        this.heapUpdateTracer = new TimedTrace(TRACE_VALUE, tracePrefix() + "updating");
    }

    @Override
    public Class heapSchemeClass() {
        return GenSSHeapScheme.class;
    }

    @Override
    public void initialize(long epoch) {
        vm().addInitializationListener(new InitializationListener() {
            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(GenSSHeapScheme.class, TeleGenSSHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleGenSSHeapScheme) teleHeapScheme();
                assert scheme != null;
            }
        });

    }

    @Override
    public List<VmHeapRegion> heapRegions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MaxMemoryManagementInfo getMemoryManagementInfo(Address address) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isObjectOrigin(Address origin) throws TeleError {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isFreeSpaceOrigin(Address origin) throws TeleError {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public RemoteReference makeReference(Address origin) throws TeleError {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        // TODO Auto-generated method stub

    }

    public static class TeleGenSSHeapScheme extends TeleHeapScheme {
        public TeleGenSSHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }

    }

}

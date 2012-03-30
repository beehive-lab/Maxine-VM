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
import com.sun.max.vm.heap.gcx.gen.mse.*;
import com.sun.max.vm.reference.*;

/**
 * Implementation details about the heap in the VM,
 * specialized for the region-based mark-sweep implementation.
 * @see GenMSEHeapScheme
 */
public final class RemoteGenMSEHeapScheme extends RemoteRegionBasedHeapScheme implements RemoteObjectReferenceManager {

    /**
     * The VM object that implements the {@link HeapScheme} in the current configuration.
     */
    private TeleGenMSEHeapScheme scheme;

    public RemoteGenMSEHeapScheme(TeleVM vm) {
        super(vm);
    }

    public Class heapSchemeClass() {
        return GenMSEHeapScheme.class;
    }

    public void initialize(long epoch) {

        vm().addInitializationListener(new InitializationListener() {

            public void initialiationComplete(final long initializationEpoch) {
                objects().registerTeleObjectType(GenMSEHeapScheme.class, TeleGenMSEHeapScheme.class);
                // Get the VM object that represents the heap implementation; can't do this any sooner during startup.
                scheme = (TeleGenMSEHeapScheme) teleHeapScheme();
                assert scheme != null;
            }
        });
    }

    public List<VmHeapRegion> heapRegions() {
        return Collections.emptyList();
    }

    public boolean isObjectOrigin(Address origin) throws TeleError {
        // TODO Auto-generated method stub
        return false;
    }

    public RemoteReference makeReference(Address origin) throws TeleError {
        // TODO Auto-generated method stub
        return null;
    }

    public void printObjectSessionStats(PrintStream printStream, int indent, boolean verbose) {
        // TODO Auto-generated method stub
    }


    /**
     * Surrogate object for the scheme instance in the VM.
     */
    public static class TeleGenMSEHeapScheme extends TeleHeapScheme {

        public TeleGenMSEHeapScheme(TeleVM vm, Reference reference) {
            super(vm, reference);
        }


    }
}

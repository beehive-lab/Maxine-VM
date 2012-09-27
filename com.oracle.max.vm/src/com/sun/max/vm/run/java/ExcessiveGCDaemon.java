/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.run.java;

import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.HeapScheme.GCRequest;
import com.sun.max.vm.thread.*;

/**
 * A daemon thread that calls {@link Heap#collectGarbage(Size)} periodically.
 */
class ExcessiveGCDaemon extends Thread {

    private final int frequency;

    /**
     * Creates a daemon thread that calls {@link Heap#collectGarbage(Size)} every {@code frequency} milliseconds.
     */
    public ExcessiveGCDaemon(int frequency) {
        super("ExcessiveGCDaemon");
        setDaemon(true);
        this.frequency = frequency;
    }

    @Override
    public void run() {
        final GCRequest gcRequest = VmThread.current().gcRequest;
        while (true) {
            try {
                Thread.sleep(frequency);
                // FIXME: should use a different flag to distinguish these "forced" GC from actual Runtime.gc call.
                gcRequest.explicit = true;
                Heap.collectGarbage();
            } catch (InterruptedException e) {
            }
        }
    }
}

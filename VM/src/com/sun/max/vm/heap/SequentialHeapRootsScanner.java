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
package com.sun.max.vm.heap;

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.monitor.*;
import com.sun.max.vm.thread.*;

/**
 * Scans all GC roots in the VM sequentially. The GC roots scanned by the {@link #run()}
 * method of this object are the references on the stacks of all active mutator threads as well as
 * any references {@linkplain MonitorScheme#scanReferences(PointerIndexVisitor) held}
 * by the monitor scheme in use.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class SequentialHeapRootsScanner {

    private PointerIndexVisitor pointerIndexVisitor;

    public SequentialHeapRootsScanner(PointerIndexVisitor pointerIndexVisitor) {
        this.pointerIndexVisitor = pointerIndexVisitor;
    }

    final class VmThreadLocalsScanner implements Pointer.Procedure {

        public void run(Pointer tla) {
            if (Heap.traceGCPhases()) {
                Log.print("Scanning thread local and stack roots for thread ");
                Log.printThread(VmThread.fromTLA(tla), true);
            }
            VmThreadLocal.scanReferences(tla, pointerIndexVisitor);
        }
    }

    private final VmThreadLocalsScanner tlaScanner = new VmThreadLocalsScanner();

    public void run() {
        VmThreadMap.ACTIVE.forAllThreadLocals(null, tlaScanner);
        vmConfig().monitorScheme().scanReferences(pointerIndexVisitor);
    }

}

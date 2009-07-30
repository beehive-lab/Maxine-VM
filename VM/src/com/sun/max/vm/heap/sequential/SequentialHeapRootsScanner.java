/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap.sequential;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
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

    public void setPointerIndexVisitor(PointerIndexVisitor pointerIndexVisitor) {
        this.pointerIndexVisitor = pointerIndexVisitor;
    }

    final class VmThreadLocalsScanner implements Pointer.Procedure {

        public void run(Pointer vmThreadLocals) {
            if (Heap.traceGCPhases()) {
                Log.print("Scanning roots in stack for thread ");
                Log.printVmThread(VmThread.fromVmThreadLocals(vmThreadLocals), true);
            }
            VmThreadLocal.scanReferences(vmThreadLocals, pointerIndexVisitor);
        }
    }

    private final VmThreadLocalsScanner vmThreadLocalsScanner = new VmThreadLocalsScanner();

    public void run() {
        VmThreadMap.ACTIVE.forAllVmThreadLocals(null, vmThreadLocalsScanner);
        VMConfiguration.hostOrTarget().monitorScheme().scanReferences(pointerIndexVisitor);
    }

}

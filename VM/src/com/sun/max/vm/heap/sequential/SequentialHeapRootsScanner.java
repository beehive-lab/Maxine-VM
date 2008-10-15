<<<<<<< local
<<<<<<< local
/*VCSID=34e0a288-6514-4950-984a-23b8dee9ae1f
=======
=======
>>>>>>> other
/*
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
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

<<<<<<< local
<<<<<<< local
=======
=======
>>>>>>> other
import com.sun.max.memory.*;
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.thread.*;

/**
 * Scans all GC roots in the VM sequentially.
 *
 * @author Bernd Mathiske
 */
public class SequentialHeapRootsScanner {

    private PointerIndexVisitor _pointerIndexVisitor;
<<<<<<< local
<<<<<<< local
=======
=======
>>>>>>> other
    private RuntimeMemoryRegion _fromSpace;
    private RuntimeMemoryRegion _toSpace;
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
    private HeapScheme _heapScheme;

<<<<<<< local
<<<<<<< local
    public SequentialHeapRootsScanner(HeapScheme heapScheme, PointerIndexVisitor pointerIndexVisitor) {
=======
=======
>>>>>>> other
    public SequentialHeapRootsScanner() {
    }

    public void setHeapScheme(HeapScheme heapScheme) {
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
        _heapScheme = heapScheme;
<<<<<<< local
<<<<<<< local
=======
=======
>>>>>>> other
    }

    public void setPointerIndexVisitor(PointerIndexVisitor pointerIndexVisitor) {
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
        _pointerIndexVisitor = pointerIndexVisitor;
    }
<<<<<<< local
<<<<<<< local
=======
=======
>>>>>>> other

    public void setFromSpace(RuntimeMemoryRegion fromSpace) {
        _fromSpace = fromSpace;
    }

    public void setToSpace(RuntimeMemoryRegion toSpace) {
        _toSpace = toSpace;
    }

<<<<<<< local
>>>>>>> other
=======
>>>>>>> other

    private final Pointer.Procedure _vmThreadLocalsScanner = new Pointer.Procedure() {

        public void run(Pointer localSpace) {
<<<<<<< local
<<<<<<< local
            VmThreadLocal.scanReferences(localSpace, _pointerIndexVisitor);
=======
=======
>>>>>>> other
            VmThreadLocal.scanReferences(localSpace, _pointerIndexVisitor, _fromSpace, _toSpace);
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
        }
    };

    /**
     * The run() method of the Sequential Heap Root Scanner performs two tasks. 1) For all ACTIVE threads in VMThreadMap
     * (holds the active threads of the VM) execute the _localSpaceScanner.run() which scans all the roots objects. 2)
     * Scans the references of the monitor scheme used. The run() method of the Sequential Heap Root Scanner performs
     * two tasks. 1) For all ACTIVE threads in VMThreadMap (holds the active threads of the VM) execute the
     * _vmThreadLocalsScanner.run() which scans all the roots objects. 2) Scans the references of the monitor scheme
     * used.
     */
    public void run() {
        VmThreadMap.ACTIVE.forAllVmThreadLocals(null, _vmThreadLocalsScanner);
<<<<<<< local
<<<<<<< local
        VMConfiguration.hostOrTarget().monitorScheme().scanReferences(_pointerIndexVisitor);
=======
=======
>>>>>>> other
        VMConfiguration.hostOrTarget().monitorScheme().scanReferences(_pointerIndexVisitor, _fromSpace, _toSpace);
<<<<<<< local
>>>>>>> other
=======
>>>>>>> other
    }

}

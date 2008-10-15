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
/*VCSID=9b9d9be4-ecee-46ab-b85f-b273ff4df8e1*/
package com.sun.max.vm.heap;

import com.sun.max.vm.*;
import com.sun.max.vm.heap.sequential.Beltway.*;
import com.sun.max.vm.heap.util.*;

/**
 * @author Bernd Mathiske
 */
public abstract class GripUpdatingHeapScheme extends AbstractVMScheme implements HeapScheme {

    protected static Action _copyAction;
    protected static Action _verifyAction = new VerifyActionImpl();
    protected static BeltWayCellVisitor _visitor = new CellVisitorImpl();

    static {
        if (BeltwayConfiguration._parallelScavenging) {
            _copyAction = new ParallelCopyActionImpl();
        } else {
            _copyAction = new CopyActionImpl();
        }
    }

    private static BeltWayPointerOffsetVisitor _pointerOffsetGripVerifier = new PointerOffsetVisitorImpl(_verifyAction);
    private static BeltWayPointerIndexVisitor _pointerIndexGripVerifier = new PointerIndexVisitorImpl(_verifyAction);

    private static BeltWayPointerOffsetVisitor _pointerOffsetGripUpdater = new PointerOffsetVisitorImpl(_copyAction);
    private static BeltWayPointerIndexVisitor _pointerIndexGripUpdater = new PointerIndexVisitorImpl(_copyAction);

    protected GripUpdatingHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public BeltWayCellVisitor getVisitor() {
        return _visitor;
    }

    public Action getAction() {
        return _copyAction;
    }

    public BeltWayPointerOffsetVisitor getPointerOffsetGripVerifier() {
        return _pointerOffsetGripVerifier;
    }

    public BeltWayPointerIndexVisitor getPointerIndexGripVerifier() {
        return _pointerIndexGripVerifier;
    }

    public BeltWayPointerIndexVisitor getPointerIndexGripUpdater() {
        return _pointerIndexGripUpdater;
    }

    public BeltWayPointerOffsetVisitor getPointerOffsetGripUpdater() {
        return _pointerOffsetGripUpdater;
    }

}

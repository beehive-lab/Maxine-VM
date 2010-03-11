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
package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;

/**
 * A simple generational scavenger with mark-sweep collection for the old generation.
 * This is a first step towards the full mark-sweep with partial evacuation planned for
 * Maxine's new heap management.
 *
 * @author Laurent Daynes
 */
public class MSGenHeapScheme extends HeapSchemeWithTLAB {

    public MSGenHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    @Override
    protected void doBeforeTLABRefill(Pointer tlabAllocationMark, Pointer tlabEnd) {
        // TODO Auto-generated method stub

    }

    @Override
    protected Pointer handleTLABOverflow(Size size, Pointer enabledVmThreadLocals, Pointer tlabMark, Pointer tlabEnd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int auxiliarySpaceSize(int bootImageSize) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean collectGarbage(Size requestedFreeSpace) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean contains(Address address) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isGcThread(Thread thread) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPinned(Object object) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean pin(Object object) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Size reportFreeSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Size reportUsedSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void runFinalization() {
        // TODO Auto-generated method stub

    }

    @Override
    public void unpin(Object object) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeBarrier(Reference from, Reference to) {
        // TODO Auto-generated method stub

    }

}

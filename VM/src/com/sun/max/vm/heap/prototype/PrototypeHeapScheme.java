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
package com.sun.max.vm.heap.prototype;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.thread.*;

/**
 * A pseudo heap for limited unit testing on the prototype host,
 * without bootstrapping the VM.
 *
 * @author Bernd Mathiske
 */
public class PrototypeHeapScheme extends AbstractVMScheme implements HeapScheme {

    public PrototypeHeapScheme(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public boolean isGcThread(VmThread vmThread) {
        return false;
    }

    public int auxiliarySpaceSize(int bootImageSize) {
        return 0;
    }

    public Action getAction() {
        return null;
    }

    public void initializePrimordialCardTable(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {

    }

    public void initializeVmThread(VmThread vmThread) {
    }

    public Object createArrayIntoCell(DynamicHub hub, int length, Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Object createArray(DynamicHub hub, int length) {
        final Class javaArrayClass = hub.classActor().toJava();
        return Array.newInstance(javaArrayClass.getComponentType(), length);
    }

    public Object createTupleIntoCell(Hub hub, Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Object createTuple(Hub hub) {
        if (hub instanceof StaticHub) {
            return StaticTuple.create(hub.classActor());
        }
        final Class javaTupleClass = hub.classActor().toJava();
        try {
            return javaTupleClass.newInstance();
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not create tuple of class: " + javaTupleClass, throwable);
        }
    }

    public <Hybrid_Type extends Hybrid> Hybrid_Type createHybrid(DynamicHub hub) {
        throw ProgramError.unexpected();
    }

    public <Hybrid_Type extends Hybrid> Hybrid_Type expandHybrid(Hybrid_Type hybrid, int length) {
        throw ProgramError.unexpected();
    }

    public Object cloneIntoCell(Object object, Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Object clone(Object object) {
        throw ProgramError.unexpected();
    }

    public boolean equals(Object object1, Object object2) {
        throw ProgramError.unexpected();
    }

    public boolean contains(Address address) {
        return false;
    }

    public boolean collectGarbage(Size requestedFreeSpace) {
        throw ProgramError.unexpected();
    }

    public boolean collect(Size requestedFreeSpace) {
        throw ProgramError.unexpected();
    }

    public Size reportFreeSpace() {
        throw ProgramError.unexpected();
    }

    public void runFinalization() {
        ProgramError.unexpected();
    }

    public long numberOfGarbageCollectionInvocations() {
        return 0L;
    }

    public long numberOfGarbageTurnovers() {
        return 0L;
    }

    @INLINE
    public <Object_Type> boolean flash(Object_Type object, Procedure<Object_Type> procedure) {
        return false;
    }

    public boolean pin(Object object) {
        return false;
    }

    public void unpin(Object object) {
    }

    public boolean isPinned(Object object) {
        return false;
    }

    @Override
    public void scanRegion(Address start, Address end) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeBarrier(Reference reference) {
    }

    public Pointer gcAllocate(RuntimeMemoryRegion region, Size size) {
        return null;
    }

    public Pointer gcBumpAllocate(RuntimeMemoryRegion region, Size size) {
        return null;
    }

    public BeltWayCellVisitor getVisitor() {
        return null;
    }

    public BeltWayPointerIndexVisitor getPointerIndexGripUpdater() {
        return null;
    }

    public BeltWayPointerOffsetVisitor getPointerOffsetGripUpdater() {
        return null;
    }

    public BeltWayPointerIndexVisitor getPointerIndexGripVerifier() {
        return null;
    }

    public BeltWayPointerOffsetVisitor getPointerOffsetGripVerifier() {
        return null;
    }

    @Override
    public Address adjustedCardTableAddress() {
        return null;
    }

}

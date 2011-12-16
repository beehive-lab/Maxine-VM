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
package com.sun.max.vm.heap.hosted;

import java.lang.reflect.*;

import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

/**
 * A pseudo heap for limited unit testing on the prototype host,
 * without bootstrapping the VM.
 */
public class HostedHeapScheme extends HeapSchemeAdaptor implements HeapScheme {

    public HostedHeapScheme() {
        super();
    }

    public boolean isGcThread(Thread thread) {
        return false;
    }

    public Object createArrayIntoCell(DynamicHub hub, int length, Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Object createArray(DynamicHub hub, int length) {
        final Class javaArrayClass = hub.classActor.toJava();
        return Array.newInstance(javaArrayClass.getComponentType(), length);
    }

    public Object createTupleIntoCell(Hub hub, Pointer cell) {
        throw ProgramError.unexpected();
    }

    public Object createTuple(Hub hub) {
        if (hub instanceof StaticHub) {
            return ClassActor.create(hub.classActor);
        }
        final Class javaTupleClass = hub.classActor.toJava();
        try {
            return javaTupleClass.newInstance();
        } catch (Throwable throwable) {
            throw ProgramError.unexpected("could not create tuple of class: " + javaTupleClass, throwable);
        }
    }

    public Object createHybrid(DynamicHub hub) {
        throw ProgramError.unexpected();
    }

    public Hybrid expandHybrid(Hybrid hybrid, int length) {
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

    public Size reportUsedSpace() {
        throw ProgramError.unexpected();
    }

    public Pointer gcAllocate(MemoryRegion region, Size size) {
        return Pointer.zero();
    }

    public Pointer gcBumpAllocate(MemoryRegion region, Size size) {
        return Pointer.zero();
    }

    public boolean pin(Object object) {
        return false;
    }

    public void unpin(Object object) {
    }

    @Override
    public void disableCustomAllocation() {
        FatalError.unexpected("Non implemented");
    }

    @Override
    public void enableCustomAllocation(Address customAllocator) {
        FatalError.unexpected("Non implemented");
    }
}

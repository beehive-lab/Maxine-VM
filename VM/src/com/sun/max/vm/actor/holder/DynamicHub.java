/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;

/**
 * @author Bernd Mathiske
 */
public final class DynamicHub extends Hub {

    DynamicHub(Size tupleSize,
               SpecificLayout specificLayout,
               ClassActor classActor,
               int[] superClassActorIds,
               Iterable<InterfaceActor> allInterfaceActors,
               int vTableLength,
               TupleReferenceMap referenceMap) {
        super(tupleSize,
              specificLayout,
              classActor,
              superClassActorIds,
              allInterfaceActors,
              vTableLength,
              referenceMap);
    }

    private void initializeMTable(int[] superClassActorIds, Iterable<InterfaceActor> allInterfaceActors, Mapping<MethodActor, VirtualMethodActor> methodLookup, int[] iToV) {
        // The first word of the iTable is where all unused mTable entries point:
        int iTableIndex = iTableStartIndex;
        // We set it to zero so it does not match any class actor's serial (they start at 1):
        setWord(iTableIndex, Address.zero());

        // Initialize all mTable entries by making them point to the first iTable slot:
        for (int mTableIndex = mTableStartIndex; mTableIndex < mTableStartIndex + mTableLength; mTableIndex++) {
            setInt(mTableIndex, iTableIndex);
        }
        iTableIndex++;

        // Reserve iTable space for interface method entries:
        for (InterfaceActor interfaceActor : allInterfaceActors) {
            final int mTableIndex = getMTableIndex(interfaceActor.id);
            setInt(mTableIndex, iTableIndex);
            assert getWord(iTableIndex).isZero();
            setWord(iTableIndex, Address.fromInt(interfaceActor.id));
            iTableIndex++;
            if (classActor.isReferenceClassActor()) {
                for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                    final VirtualMethodActor virtualMethodActor = methodLookup.get(interfaceMethodActor);
                    iToV[iTableIndex - iTableStartIndex] = virtualMethodActor.vTableIndex();
                    iTableIndex++;
                }
            }
        }
        for (int id : superClassActorIds) {
            if (id >= 0) {
                final int mTableIndex = getMTableIndex(id);
                setInt(mTableIndex, iTableIndex);
                assert getWord(iTableIndex).isZero();
                setWord(iTableIndex, Address.fromInt(id));
                iTableIndex++;
            } else {
                // ignore interface ids
            }
        }
    }

    DynamicHub expand(int[] superClassActorIds, Iterable<InterfaceActor> allInterfaceActors, Mapping<MethodActor, VirtualMethodActor> methodLookup, int[] iToV, TupleReferenceMap referenceMap) {
        final DynamicHub hub = (DynamicHub) expand();
        assert hub.mTableLength > 0;
        referenceMap.copyIntoHub(hub);
        hub.initializeMTable(superClassActorIds, allInterfaceActors, methodLookup, iToV);
        return hub;
    }

    /**
     * Ensures all the vtable entries in this hub are resolved to compiled methods, not trampolines.
     */
    public void compileVTable() {
        VirtualMethodActor[] allVirtualMethodActors = classActor.allVirtualMethodActors();
        for (int i = 0; i < allVirtualMethodActors.length; i++) {
            final VirtualMethodActor virtualMethodActor = allVirtualMethodActors[i];
            final int vTableIndex = firstWordIndex() + i;
            assert virtualMethodActor.vTableIndex() == vTableIndex;
            Address vTableEntry = CallEntryPoint.VTABLE_ENTRY_POINT.in(vmConfig().compilationScheme().synchronousCompile(virtualMethodActor));
            setWord(vTableIndex, vTableEntry);
        }
    }

    void initializeITable(Iterable<InterfaceActor> allInterfaceActors, Mapping<MethodActor, VirtualMethodActor> methodLookup) {
        if (classActor.isReferenceClassActor()) {
            for (InterfaceActor interfaceActor : allInterfaceActors) {
                final int interfaceIndex = getITableIndex(interfaceActor.id);
                for (InterfaceMethodActor interfaceMethodActor : interfaceActor.localInterfaceMethodActors()) {
                    final VirtualMethodActor virtualMethodActor = methodLookup.get(interfaceMethodActor);
                    final int iTableIndex = interfaceIndex + interfaceMethodActor.iIndexInInterface();
                    final int iIndex = iTableIndex - iTableStartIndex;
                    assert getWord(iTableIndex).isZero();
                    Address iTableEntry;
                    iTableEntry = checkCompiled(virtualMethodActor);
                    if (iTableEntry.isZero()) {
                        iTableEntry = vm().stubs.interfaceTrampoline(iIndex);
                    }
                    setWord(iTableIndex, iTableEntry);
                }
            }
        }
    }

    @Override
    public FieldActor findFieldActor(int offset) {
        return classActor.findInstanceFieldActor(offset);
    }

}

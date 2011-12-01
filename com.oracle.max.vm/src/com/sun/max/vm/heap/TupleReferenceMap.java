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

import java.util.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A facility for building and traversing the reference map for a {@linkplain TupleClassActor tuple} object.
 * A reference map is an ordered list of word-scaled offsets relative to an object's origin describing the
 * object slots containing references.
 * The reference maps copied into hubs are ordered by increasing offsets.
 */
public class TupleReferenceMap {

    /**
     * The set of word-scaled offsets to references.
     * The list is built so that offsets are ordered from highest to lowest.
     * The list will be traversed from last to first when copying offsets into a hub's reference map.
     */
    private List<Integer> indexes = new LinkedList<Integer>();

    /**
     * Builds a reference map for a given set of static fields. This is the reference map that
     * will be {@linkplain #copyIntoHub(Hub) copied} into the {@linkplain ClassActor#staticHub() static hub}
     * of a class to cover the fields in its {@linkplain ClassActor#staticTuple() static tuple}.
     */
    public TupleReferenceMap(FieldActor[] staticFieldActors) {
        // Iterate from last to first to insert reference fields in decreasing order to enable sharing the code of copyIntoHub with instance field reference maps.
        int i = staticFieldActors.length - 1;
        while (i >= 0) {
            final FieldActor staticFieldActor = staticFieldActors[i--];
            assert !staticFieldActor.descriptor().equals(JavaTypeDescriptor.CODE_POINTER) : "there must be no fields of type CodePointer";
            if (staticFieldActor.kind.isReference) {
                final int fieldIndex = UnsignedMath.divide(staticFieldActor.offset(), Word.size());
                indexes.add(fieldIndex);
            }
        }
    }

    /**
     * Builds a reference map for the instance fields of a given class. This is the reference map that
     * will be {@linkplain #copyIntoHub(Hub) copied} into the {@linkplain ClassActor#dynamicHub() dynamic hub}
     * of the class. To ease visiting references of a tuple within a region of heap space, field indexes are ordered by increasing
     * value in the reference map.
     */
    public TupleReferenceMap(ClassActor classActor) {
        ClassActor c = classActor;
        do {
            final FieldActor [] fieldActors = c.localInstanceFieldActors();
            int i = fieldActors.length - 1;
            while (i >= 0) {
                final FieldActor instanceFieldActor = fieldActors[i--];
                if (instanceFieldActor.kind.isReference && instanceFieldActor != ClassRegistry.JLRReference_referent) {
                    final int fieldIndex = UnsignedMath.divide(instanceFieldActor.offset(), Word.size());
                    indexes.add(fieldIndex);
                }
            }            c = c.superClassActor;
        } while (c != null);
        // The indexes list is ordered from higher to lower offsets.
        // This means that when copying it into a hub, the list will be scanned from last to first.
    }

    public static final TupleReferenceMap EMPTY = new TupleReferenceMap(new FieldActor[0]);

    /**
     * Gets the number of entries in this reference map.
     */
    public int numberOfEntries() {
        return indexes.size();
    }

    /**
     * Copy the reference map into the specified hub. The copied map is ordered by increasing offsets.
     * @param hub
     */
    public void copyIntoHub(Hub hub) {
        int index = hub.referenceMapStartIndex;
        ListIterator<Integer>  iterator = indexes.listIterator(indexes.size());
        int lastRefIndex = 0;
        while (iterator.hasPrevious()) {
            int refIndex = iterator.previous();
            FatalError.check(refIndex > lastRefIndex, "Reference index in tuple reference maps must be ordered");
            lastRefIndex = refIndex;
            hub.setInt(index, refIndex);
            index++;
        }
    }

    /**
     * Visits all the references in a given object described by a given hub.
     *
     * @param hub a hub describing where the references are in the object at {@code origin}
     * @param origin the origin of an object
     * @param visitor the visitor to notify of each reference in the object denoted by {@code origin}
     */
    public static void visitReferences(Hub hub, Pointer origin, PointerIndexVisitor visitor) {
        final int n = hub.referenceMapStartIndex + hub.referenceMapLength;
        for (int i = hub.referenceMapStartIndex; i < n; i++) {
            final int index = hub.getInt(i);
            visitor.visit(origin, index);
        }
    }
    /**
     * Visits all the references within a range of heap space in a given object described by a given hub.
     *
     * @param hub a hub describing where the references are in the object at {@code origin}
     * @param origin the origin of an object
     * @param visitor the visitor to notify of each reference in the object denoted by {@code origin}
     * @param startOfRange start of the range of heap space (inclusive)
     * @param endOfRange  end of the range of heap space (exclusive)
     */
    public static void visitReferences(Hub hub, Pointer origin, PointerIndexVisitor visitor, Address startOfRange, Address endOfRange) {
        final int l = hub.referenceMapLength;
        if (l == 0) {
            return;
        }
        // compute the bounds in the map that corresponds to references in the range.
        // This code assumes that the reference maps is ordered by increasing offsets.
        final int n = hub.referenceMapStartIndex + l;
        int firstInRange = hub.referenceMapStartIndex;
        do  {
            final int index = hub.getInt(firstInRange);
            if (origin.plusWords(index).greaterEqual(startOfRange)) {
                break;
            }
            firstInRange++;
        } while (firstInRange < n);
        int lastInRange = n - 1;
        while (lastInRange > firstInRange) {
            final int index = hub.getInt(lastInRange);
            if (origin.plusWords(index).lessThan(endOfRange)) {
                break;
            }
            lastInRange--;
        }
        // Visit the references in the range.
        for (int i = firstInRange; i <= lastInRange; i++) {
            visitor.visit(origin, hub.getInt(i));
        }
    }
}

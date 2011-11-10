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
import com.sun.max.vm.type.*;

/**
 * A facility for building and traversing the reference map for a {@linkplain TupleClassActor tuple} object.
 * A reference map is a list of word-scaled offsets relative to an object's origin describing the
 * object slots containing references.
 */
public class TupleReferenceMap {

    /**
     * The set of word-scaled offsets to references.
     */
    private List<Integer> indexes = new LinkedList<Integer>();

    /**
     * Builds a reference map for a given set of static fields. This is the reference map that
     * will be {@linkplain #copyIntoHub(Hub) copied} into the {@linkplain ClassActor#staticHub() static hub}
     * of a class to cover the fields in its {@linkplain ClassActor#staticTuple() static tuple}.
     */
    public TupleReferenceMap(FieldActor[] staticFieldActors) {
        for (FieldActor staticFieldActor : staticFieldActors) {
            assert !staticFieldActor.isTaggedField() : "there must be no fields of type CodePointer";
            if (staticFieldActor.kind.isReference) {
                final int fieldIndex = UnsignedMath.divide(staticFieldActor.offset(), Word.size());
                indexes.add(fieldIndex);
            }
        }
    }

    /**
     * Builds a reference map for the instance fields of a given class. This is the reference map that
     * will be {@linkplain #copyIntoHub(Hub) copied} into the {@linkplain ClassActor#dynamicHub() dynamic hub}
     * of the class.
     */
    public TupleReferenceMap(ClassActor classActor) {
        ClassActor c = classActor;
        do {
            for (FieldActor instanceFieldActor : c.localInstanceFieldActors()) {
                if (instanceFieldActor.kind.isReference && instanceFieldActor != ClassRegistry.JLRReference_referent) {
                    final int fieldIndex = UnsignedMath.divide(instanceFieldActor.offset(), Word.size());
                    indexes.add(fieldIndex);
                }
            }
            c = c.superClassActor;
        } while (c != null);
    }

    public static final TupleReferenceMap EMPTY = new TupleReferenceMap(new FieldActor[0]);

    /**
     * Gets the number of entries in this reference map.
     */
    public int numberOfEntries() {
        return indexes.size();
    }

    public void copyIntoHub(Hub hub) {
        int index = hub.referenceMapStartIndex;
        for (Integer referenceIndex : indexes) {
            hub.setInt(index, referenceIndex);
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
}

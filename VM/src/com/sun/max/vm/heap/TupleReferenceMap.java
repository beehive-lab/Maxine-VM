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
package com.sun.max.vm.heap;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * A facility for building and traversing the reference map for a {@linkplain TupleClassActor tuple} object.
 * A reference map is a list of word-scaled offsets relative to an object's origin describing the
 * object slots containing references.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
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
            if (staticFieldActor.kind.isReference) {
                final int fieldIndex = Unsigned.idiv(staticFieldActor.offset(), Word.size());
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
                if (instanceFieldActor.kind.isReference && !instanceFieldActor.isSpecialReference()) {
                    final int fieldIndex = Unsigned.idiv(instanceFieldActor.offset(), Word.size());
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

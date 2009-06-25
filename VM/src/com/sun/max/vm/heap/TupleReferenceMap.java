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

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.type.*;

/**
 * A facility for building and traversing the reference map for a {@linkplain TupleClassActor tuple}
 * object. The
 *
 * @author Bernd Mathiske
 */
public class TupleReferenceMap {

    private AppendableSequence<Integer> offsets = new LinkSequence<Integer>();

    /**
     * Builds a reference map for a given set of static fields. This is the reference map that
     * will be {@linkplain #copyIntoHub(Hub) copied} into the {@linkplain ClassActor#staticHub() static hub}
     * of a class to cover the fields in its {@linkplain ClassActor#staticTuple() static tuple}.
     */
    public TupleReferenceMap(FieldActor[] staticFieldActors) {
        for (FieldActor staticFieldActor : staticFieldActors) {
            if (staticFieldActor.kind == Kind.REFERENCE) {
                offsets.append(staticFieldActor.offset());
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
                if (instanceFieldActor.kind == Kind.REFERENCE && !instanceFieldActor.isSpecialReference()) {
                    offsets.append(instanceFieldActor.offset());
                }
            }
            c = c.superClassActor;
        } while (c != null);
    }

    public static final TupleReferenceMap EMPTY = new TupleReferenceMap(new FieldActor[0]);

    public int numberOfOffsets() {
        return offsets.length();
    }

    public void copyIntoHub(Hub hub) {
        int index = hub.referenceMapStartIndex();
        for (Integer offset : offsets) {
            hub.setInt(index, offset);
            index++;
        }
    }

    public static void visitOriginOffsets(Hub hub, Pointer origin, PointerOffsetVisitor offsetVisitor) {
        final int n = hub.referenceMapStartIndex() + hub.referenceMapLength();
        for (int i = hub.referenceMapStartIndex(); i < n; i++) {
            final int offset = hub.getInt(i);
            offsetVisitor.visitPointerOffset(origin, offset);
        }
    }
}

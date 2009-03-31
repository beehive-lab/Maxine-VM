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
 * @author Bernd Mathiske
 */
public class TupleReferenceMap {

    private AppendableSequence<Integer> _offsets = new LinkSequence<Integer>();

    public TupleReferenceMap(FieldActor[] staticFieldActors) {
        for (FieldActor staticFieldActor : staticFieldActors) {
            if (staticFieldActor.kind() == Kind.REFERENCE) {
                _offsets.append(staticFieldActor.offset());
            }
        }
    }

    public TupleReferenceMap(ClassActor classActor) {
        ClassActor c = classActor;
        do {
            for (FieldActor dynamicFieldActor : c.localInstanceFieldActors()) {
                if (dynamicFieldActor.kind() == Kind.REFERENCE && !dynamicFieldActor.isSpecialReference()) {
                    _offsets.append(dynamicFieldActor.offset());
                }
            }
            c = c.superClassActor();
        } while (c != null);

    }

    public static final TupleReferenceMap EMPTY = new TupleReferenceMap(new FieldActor[0]);

    public int numberOfOffsets() {
        return _offsets.length();
    }

    public void copyIntoHub(Hub hub) {
        int index = hub.referenceMapStartIndex();
        for (Integer offset : _offsets) {
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

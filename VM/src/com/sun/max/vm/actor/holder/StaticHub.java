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
/*VCSID=16f2b4ac-6daa-4756-b11b-ac1f28c9e12e*/
package com.sun.max.vm.actor.holder;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.heap.*;

/**
 * @author Bernd Mathiske
 */
public class StaticHub extends Hub {

    StaticHub(Size tupleSize, ClassActor classActor, TupleReferenceMap referenceMap) {
        super(tupleSize, classActor, referenceMap);
    }

    /**
     * Static Hub.
     */
    StaticHub expand(TupleReferenceMap referenceMap) {
        final StaticHub hub = (StaticHub) expand();
        referenceMap.copyIntoHub(hub);
        return hub;
    }

    @Override
    public FieldActor findFieldActor(int offset) {
        return classActor().findStaticFieldActor(offset);
    }
}

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
/*VCSID=1d3452f5-1817-4eb7-8253-e1ea4e307d15*/
package com.sun.max.vm.verifier.types;

import com.sun.max.vm.actor.holder.*;

/**
 * Represents object types for which the corresponding ClassActor already exists. That is,
 * {@linkplain #resolve() resolving} this verification type is guaranteed not to cause class loading.
 *
 * @author Doug Simon
 */
public class ResolvedObjectType extends ObjectType implements ResolvedType {

    private final ClassActor _classActor;

    public ResolvedObjectType(ClassActor classActor) {
        super(classActor.typeDescriptor(), null);
        assert !classActor.isArrayClassActor();
        _classActor = classActor;
    }

    @Override
    public ClassActor resolve() {
        return _classActor;
    }
}

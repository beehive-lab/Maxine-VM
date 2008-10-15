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
package com.sun.max.vm.verifier;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

/**
 * An instance of {@code ClassVerifier} is created to verify the methods in a given class.
 * 
 * @author David Liu
 * @author Doug Simon
 */
public abstract class ClassVerifier extends Verifier {

    private final ClassActor _classActor;

    /**
     * Gets the class being verified by this verifier.
     */
    public ClassActor classActor() {
        return _classActor;
    }

    protected ClassVerifier(ClassActor classActor) {
        super(classActor.constantPool());
        _classActor = classActor;
    }

    /**
     * Performs bytecode verification for all methods in {@linkplain #classActor() the given class} that have a non-null
     * {@link ClassMethodActor#codeAttribute() code attribute}.
     */
    public synchronized void verify() {
        for (MethodActor methodActor : classActor().getLocalMethodActors()) {
            if (methodActor instanceof ClassMethodActor) {
                final ClassMethodActor classMethodActor = (ClassMethodActor) methodActor;
                classMethodActor.verify(this);
            }
        }
    }

    /**
     * Performs bytecode verification on a given method.
     */
    public abstract CodeAttribute verify(ClassMethodActor classMethodActor, CodeAttribute codeAttribute);

}

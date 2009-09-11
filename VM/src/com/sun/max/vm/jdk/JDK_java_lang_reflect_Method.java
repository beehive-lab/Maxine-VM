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
package com.sun.max.vm.jdk;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import sun.reflect.annotation.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Method substitutions for {@link Method}.
 *
 */
@METHOD_SUBSTITUTIONS(Method.class)
public final class JDK_java_lang_reflect_Method {

    private JDK_java_lang_reflect_Method() {
    }

    /**
     * Casts this to {@link Method}.
     */
    @UNSAFE_CAST
    private native Method thisMethod();

    /**
     * Gets the method actor associated with this method.
     * @return the method actor for this method
     */
    @INLINE
    private MethodActor thisMethodActor() {
        return MethodActor.fromJava(thisMethod());
    }

    /**
     * Gets the declared annotations for this method.
     * @return a map from the declaring class to its annotations
     */
    @SUBSTITUTE
    private synchronized Map<Class, Annotation> declaredAnnotations() {
        final MethodActor methodActor = thisMethodActor();
        // in java.lang.reflect.Method.declaredAnnotations, the result is cached. Not sure how to do that using substitution.
        return AnnotationParser.parseAnnotations(methodActor.runtimeVisibleAnnotationsBytes(), new ConstantPoolAdapter(methodActor.holder().constantPool()), methodActor.holder().toJava());
    }
}

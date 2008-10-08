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
package com.sun.max.vm.template;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.runtime.*;

/**
 * Runtime functions for templates. These help keeping the size of complex templates small.
 *
 * @author Laurent Daynes
 */
public class TemplateRuntime {

    public static ClassActor resolveClass(ReferenceResolutionGuard guard) {
        final ConstantPool constantPool = guard.constantPool();
        final int index = guard.constantPoolIndex();
        final ClassActor classActor = constantPool.classAt(index).resolve(constantPool, index);
        guard.set(classActor);
        return classActor;
    }

    public static Object resolveMirror(ReferenceResolutionGuard guard) {
        return resolveClass(guard).mirror();
    }

    public static Object getClassMirror(ClassActor classActor) {
        return classActor.mirror();
    }
}

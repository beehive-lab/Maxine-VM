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
package com.sun.max.vm.compiler;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * This interface specifies the interface between a dynamic compiler
 * and the rest of the virtual machine, including methods to create
 * entries for vtables, compile methods, and stack walking.
 *
 * @author Laurent Daynes
 * @author Ben L. Titzer
 * @author Thomas Wuerthinger
 */
public interface RuntimeCompilerScheme extends VMScheme {

    /**
     * Compiles a method to an internal representation.
     *
     * @param classMethodActor the method to compile
     * @return a reference to the target method created by this compiler for {@code classMethodActor}
     */
    TargetMethod compile(ClassMethodActor classMethodActor);

    /**
     * Gets the exact subtype of {@link TargetMethod} produces by this compiler.
     *
     * @param <Type> the exact type of the object returned by {@link #compile(ClassMethodActor)}
     */
    <Type extends TargetMethod> Class<Type> compiledType();

    CallEntryPoint calleeEntryPoint();
}

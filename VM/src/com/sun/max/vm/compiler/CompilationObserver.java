/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * This interface represents an observer that can be installed with a {@link CompilationScheme} which
 * is notified before and after compilation of methods.
 *
 * @author Ben L. Titzer
 */
public interface CompilationObserver {

    /**
     * This method allows an observer to be notified before the compilation of a method begins.
     * @param classMethodActor the method being compiled
     * @param directive the directive controlling the compilation
     * @param compiler the compiler performing the compilation
     */
    void observeBeforeCompilation(ClassMethodActor classMethodActor, CompilationDirective directive, DynamicCompilerScheme compiler);

    /**
     * This method allows an observer to be notified after the compilation of a method completes.
     * @param classMethodActor the method being compiled
     * @param directive the directive controlling the compilation
     * @param compiler the compiler performing the compilation
     * @param targetMethod the target method produced by the compilation; <code>null</code> if the compilation
     * was aborted or failed to produce a target method
     */
    void observeAfterCompilation(ClassMethodActor classMethodActor, CompilationDirective directive, DynamicCompilerScheme compiler, TargetMethod targetMethod);
}

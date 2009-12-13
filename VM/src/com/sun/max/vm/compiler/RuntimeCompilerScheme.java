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
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
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
     * Compiles a method to an internal representation (typically a {@link TargetMethod}).
     *
     * @param classMethodActor the method to compile
     * @return a reference to the IR method created by this compiler for the specified method
     */
    TargetMethod compile(ClassMethodActor classMethodActor);

    /**
     * Walks a frame for a target method that was produced by this compiler.
     *
     * @param current the cursor for the current frame
     * @param callee the cursor for the callee frame
     * @param calleeMethod the target method whose frame is below this frame being walked (i.e. the frame of {@code
     *            targetMethod}'s callee). This will be null if {@code targetMethod} called a native function.
     * @param purpose the purpose of this stack walk
     * @param context the context for the stack walk
     * @return whether stack walking may continue after executing this method
     */
    boolean walkFrame(StackFrameWalker.Cursor current, StackFrameWalker.Cursor callee, TargetMethod calleeMethod, Purpose purpose, Object context);
}

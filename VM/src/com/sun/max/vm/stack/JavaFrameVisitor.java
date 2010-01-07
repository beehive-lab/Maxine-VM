/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.stack;

import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.value.Value;

/**
 * This interface represents a visitor of Java frames on a stack.
 *
 * @author Ben L. Titzer
 */
public interface JavaFrameVisitor {

    /**
     * This method is called by each frame to determine whether to recover the debugging values.
     * Because generating the values for each frame may be much more expensive than simply determining
     * the method location, this method allows the common case to avoid generating unneeded debug values.
     * @return {@code true} if the frame should generate the debugging values
     */
    boolean generateDebugValues();

    /**
     * This method is called by each frame as it is visited to enumerate the Java stack frames corresponding
     * to the VM-level call stack. This method is called for each frame, from the topmost frame down.
     * The format of the debug values, if they are generated is as follows:
     * <p>
     * Elements 0...maxlocals-1 contain the local variables
     * <br>
     * Elements maxlocals...debugValues.length-1 contain the operand stack
     *
     * @param classMethodActor the method actor corresponding to the Java method
     * @param bci the bytecode index in the method actor
     * @param debugValues the debugging values, containing the actual runtime values of the stack, if requested
     * @return {@code true} if the stack walk should continue
     */
    boolean visit(ClassMethodActor classMethodActor, int bci, Value[] debugValues);
}

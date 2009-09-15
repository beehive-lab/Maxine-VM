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
package com.sun.max.vm.compiler.target;

import com.sun.max.lang.Arrays;
import com.sun.max.program.ProgramError;
import com.sun.max.util.*;

/**
 * This class implements utility functions for transitioning a {@link com.sun.max.vm.actor.member.ClassMethodActor}
 * from one target state to another as a result of compilation.
 *
 * @author Ben L. Titzer
 */
public class TargetState {
    private static final TargetMethod[] NOT_COMPILED = {};

    public static int targetMethodCount(Object targetState) {
        if (targetState == null || targetState instanceof Throwable) {
            // not compiled yet
            return 0;
        } else if (targetState instanceof TargetMethod) {
            // only compiled once
            return 1;
        } else if (targetState instanceof TargetMethod[]) {
            // compiled multiple times
            return ((TargetMethod[]) targetState).length;
        } else if (targetState instanceof Compilation) {
            // currently being compiled, return previous count of target methods
            return targetMethodCount(((Compilation) targetState).previousTargetState);
        }
        return 0;
    }

    public static TargetMethod currentTargetMethod(Object targetState) {
        if (targetState == null) {
            // not compiled yet
            return null;
        } else if (targetState instanceof TargetMethod) {
            // only compiled once
            return (TargetMethod) targetState;
        } else if (targetState instanceof TargetMethod[]) {
            // compiled multiple times, return latest
            return ((TargetMethod[]) targetState)[0];
        } else if (targetState instanceof Compilation) {
            // currently being compiled, return any previous target method
            return currentTargetMethod(((Compilation) targetState).previousTargetState);
        } else if (targetState instanceof Throwable) {
            throw Exceptions.cast(Error.class, (Throwable) targetState);
        }
        return null;
    }

    public static TargetMethod[] targetMethodHistory(Object targetState) {
        if (targetState == null) {
            // not compiled yet
            return NOT_COMPILED;
        } else if (targetState instanceof TargetMethod) {
            // only compiled once
            return new TargetMethod[] {(TargetMethod) targetState};
        } else if (targetState instanceof TargetMethod[]) {
            // compiled multiple times
            return (TargetMethod[]) targetState;
        } else if (targetState instanceof Compilation) {
            // currently being compiled
            return targetMethodHistory(((Compilation) targetState).previousTargetState);
        } else if (targetState instanceof Throwable) {
            throw Exceptions.cast(Error.class, (Throwable) targetState);
        }
        return NOT_COMPILED;
    }

    public static Object addTargetMethod(TargetMethod targetMethod, Object targetState) {
        if (targetState == null) {
            // not compiled yet
            return targetMethod;
        } else if (targetState instanceof TargetMethod) {
            // only compiled once, make into an array
            return new TargetMethod[] {targetMethod, (TargetMethod) targetState};
        } else if (targetState instanceof TargetMethod[]) {
            // compiled multiple times, make this the latest
            return Arrays.prepend((TargetMethod[]) targetState, targetMethod);
        }
        throw ProgramError.unexpected("Unknown or invalid TargetState: " + targetState);
    }
}

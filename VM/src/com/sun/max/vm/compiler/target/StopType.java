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
/*VCSID=caed9843-e3eb-45db-931e-525b6dfaaf16*/
package com.sun.max.vm.compiler.target;

import com.sun.max.collect.*;

/**
 * Categorization of stops. Stops are target code positions for which the locations of object references are precisely known.
 *
 * @author Doug Simon
 */
public enum StopType {
    /**
     * A position corresponding to a direct call (e.g. target method is known at compile time). The locations of object
     * references on the stack are precisely known at an indirect call.
     */
    DIRECT_CALL {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            if (n < 0 || n >= targetMethod.numberOfDirectCalls()) {
                throw new IllegalArgumentException();
            }
            return n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfDirectCalls();
        }
    },

    /**
     * A position corresponding to a register indirect call (e.g. late binding call, virtual/interface call). The
     * locations of object references on the stack are precisely known at an indirect call.
     */
    INDIRECT_CALL {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            return targetMethod.numberOfDirectCalls() + n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfIndirectCalls();
        }
    },

    /**
     * A safepoint is a position at which the GC can stop a thread. The locations of object references on both the stack
     * and in the registers are precisely known at a safepoint.
     */
    SAFEPOINT {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            return targetMethod.numberOfDirectCalls() + targetMethod.numberOfIndirectCalls() + n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfSafepoints();
        }
    },

    /**
     * A guardpoint is a position at which a trace can exit. The locations of object references on both the stack
     * and in the registers are precisely known at a guardpoint.
     */
    GUARDPOINT {
        @Override
        public int stopPositionIndex(TargetMethod targetMethod, int n) {
            return targetMethod.numberOfDirectCalls() + targetMethod.numberOfIndirectCalls() + targetMethod.numberOfSafepoints() + n;
        }

        @Override
        public int numberOfStopPositions(TargetMethod targetMethod) {
            return targetMethod.numberOfGuardpoints();
        }
    };

    public static final IndexedSequence<StopType> VALUES = new ArraySequence<StopType>(values());

    /**
     * Gets the index of the {@code n}th stop of this type in the {@linkplain TargetMethod#stopPositions() stop positions array} of a target method.
     */
    public abstract int stopPositionIndex(TargetMethod targetMethod, int n) throws IllegalArgumentException;

    /**
     * Gets the number of stops of this type in a given target method.
     */
    public abstract int numberOfStopPositions(TargetMethod targetMethod);

    /**
     * Gets the position of the {@code n}th stop of this type in a given target method.
     */
    public int stopPosition(TargetMethod targetMethod, int n) {
        return targetMethod.stopPosition(stopPositionIndex(targetMethod, n));
    }
}

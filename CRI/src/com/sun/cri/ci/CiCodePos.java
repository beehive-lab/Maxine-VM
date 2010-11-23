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
package com.sun.cri.ci;

import com.sun.cri.ri.*;

/**
 * Represents a code position, that is, a chain of inlined methods with bytecode
 * locations, that is communicated from the compiler to the runtime system. A code position
 * can be used by the runtime system to reconstruct a source-level stack trace
 * for exceptions and to create stack frames for deoptimization (switching from
 * optimized code to deoptimized code).
 *
 * @author Ben L. Titzer
 */
public class CiCodePos {
    /**
     * The position where this position has been called, {@code null} if none.
     */
    public final CiCodePos caller;

    /**
     * The runtime interface method for this position.
     */
    public final RiMethod method;

    /**
     * The location within the method, as a bytecode index. The constant
     * {@code -1} may be used to indicate the location is unknown, for example
     * within code synthesized by the compiler.
     */
    public final int bci;

    /**
     * Constructs a new position with the given position as the parent, the given method, and the given
     * bytecode index.
     * @param caller the parent position
     * @param method the method
     * @param bci the bytecode index within the method
     */
    public CiCodePos(CiCodePos caller, RiMethod method, int bci) {
        this.caller = caller;
        this.method = method;
        this.bci = bci;
    }

    /**
     * Converts this code position to a string representation.
     * @return a string representation of this code position
     */
    @Override
    public String toString() {
        return CiUtil.append(new StringBuilder(100), this).toString();
    }

    /**
     * Deep equality test.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CiCodePos) {
            CiCodePos other = (CiCodePos) obj;
            if (other.method.equals(method) && other.bci == bci) {
                if (caller == null) {
                    return other.caller == null;
                }
                return caller.equals(other.caller);
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return bci;
    }
}

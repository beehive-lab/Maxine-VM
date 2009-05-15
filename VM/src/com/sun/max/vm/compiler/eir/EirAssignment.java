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
package com.sun.max.vm.compiler.eir;

import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 * @author Thomas Wuerthinger
 */
public interface EirAssignment {

    /**
     * The type of an assignment, used for debugging purposes only.
     *
     * @author Thomas Wuerthinger
     */
    public static enum Type {

        /** Normal assignment that already existed before register allocation. */
        NORMAL,

        /** Assignment inserted because of an interval split. */
        INTERVAL_SPLIT,

        /** Assignment inserted because data flow was resolved between block boundaries. */
        DATA_FLOW_RESOLVED,

        /** Assignment inserted because a variable was split at an operand where a fixed location is required. */
        FIXED_SPLIT,

        /** Assignment inserted because a variable was rescued to a certain location that is accessible after a possible exception occurs. */
        EXCEPTION_EDGE_RESCUED,

        /** Assignment inserted right after the catch to resolve the problem when a variable is at a different location in the exception block. */
        EXCEPTION_EDGE_RESOLVED
    }

    Kind kind();

    EirOperand destinationOperand();

    EirOperand sourceOperand();

    Type type();

    void setType(Type type);
}

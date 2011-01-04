/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir;

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
        EXCEPTION_EDGE_RESOLVED,

        /** Assignment inserted right after one and only definition of a variable to its spill slot. */
        SPILL_SLOT_DEFINITION
    }

    Kind kind();

    EirOperand destinationOperand();

    EirOperand sourceOperand();

    Type type();

    void setType(Type type);
}

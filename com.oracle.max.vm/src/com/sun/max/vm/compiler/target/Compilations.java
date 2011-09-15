/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.max.vm.compiler.target.Compilations.Attr.*;

import com.sun.max.annotate.*;

/**
 * Manages the compilations of a method, organized by attributes.
 * Depending on the compilers in use, the same target method
 * may be assigned to {@link #interpreterCompatible} and {@link #optimized}.
 */
public class Compilations {

    /**
     * Attributes describing a target method. A compilation request or retrieval of a target
     * method for a bytecode method can be refined by a mask of these attributes.
     */
    public static enum Attr {
        /**
         * Request for an {@link TargetMethod#isInterpreterCompatible() interpreter compatible} target method.
         */
        INTERPRETER_COMPATIBLE,

        /**
         * Request for an optimized target method.
         */
        OPTIMIZE;

        public static final int NONE = 0;

        public boolean isSet(int flags) {
            return (flags & mask) != 0;
        }

        public final int mask = 1 << ordinal();
    }

    public static final Compilations EMPTY = new Compilations();

    /**
     * Compiled code that is {@linkplain TargetMethod#isInterpreterCompatible() interpreter compatible}.
     */
    @INSPECTED
    public final TargetMethod interpreterCompatible;

    /**
     * Compiled code that is optimized.
     */
    @INSPECTED
    public final TargetMethod optimized;

    private Compilations() {
        interpreterCompatible = null;
        optimized = null;
    }

    /**
     * Creates an object encapsulating the compiled versions of a method.
     */
    public Compilations(TargetMethod interpreterCompatible, TargetMethod optimized) {
        assert interpreterCompatible != null || optimized != null;
        this.interpreterCompatible = interpreterCompatible;
        this.optimized = optimized;
    }

    /**
     * Gets a target method that compatible with a given set of attributes.
     * If {@code flags == 0}, then {@link #optimized} is returned if
     * it is non-null and {@linkplain TargetMethod#invalidated() valid}.
     * Otherwise, {@link #interpreterCompatible} is returned.
     *
     * @param flags a mask of {@link Compilations.Attr} values
     */
    public TargetMethod currentTargetMethod(int flags) {
        if (INTERPRETER_COMPATIBLE.isSet(flags)) {
            return interpreterCompatible;
        } else if (OPTIMIZE.isSet(flags)) {
            if (optimized != null && optimized.invalidated() == null) {
                return optimized;
            } else {
                return null;
            }
        }

        // Always prefer optimized version if no specific version was requested
        if (optimized != null && optimized.invalidated() == null) {
            return optimized;
        }
        assert interpreterCompatible == null || interpreterCompatible.invalidated() == null;
        return interpreterCompatible;
    }

    @Override
    public String toString() {
        return "Compilations[interpreterCompatible=" + interpreterCompatible + ", optimized=" + optimized + "]";
    }

    /**
     * Gets the compiled code represented by a given compiled state object.
     */
    public static TargetMethod currentTargetMethod(Object compiledState, int flags) {
        if (compiledState instanceof Compilations) {
            // compiled
            return ((Compilations) compiledState).currentTargetMethod(flags);
        } else {
            assert compiledState instanceof Compilation : "unknown compiled state type: " + compiledState.getClass();
            // currently being compiled, return any previous target method
            return currentTargetMethod(((Compilation) compiledState).prevCompilations, flags);
        }
    }
}

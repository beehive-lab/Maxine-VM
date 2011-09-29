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

import com.sun.max.annotate.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;

/**
 * Manages the compilations of a method, organized by {@link Nature}.
 */
public class Compilations {

    public static final Compilations EMPTY = new Compilations();

    /**
     * Compiled code that is {@linkplain TargetMethod#isBaseline() baseline}.
     */
    @INSPECTED
    public final TargetMethod baseline;

    /**
     * Compiled code that is optimized.
     */
    @INSPECTED
    public final TargetMethod optimized;

    private Compilations() {
        baseline = null;
        optimized = null;
    }

    /**
     * Creates an object encapsulating the compiled versions of a method.
     */
    public Compilations(TargetMethod baseline, TargetMethod optimized) {
        assert baseline != null || optimized != null;
        this.baseline = baseline;
        this.optimized = optimized;
    }

    /**
     * Gets a target method that matches a given nature.
     *
     * @param nature the specific type of target method required or {@code null} if any target method is acceptable
     */
    public TargetMethod currentTargetMethod(RuntimeCompiler.Nature nature) {
        if (nature == Nature.BASELINE) {
            return baseline;
        } else if (nature == Nature.OPT) {
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
        return baseline;
    }

    @Override
    public String toString() {
        return "Compilations[baseline=" + baseline + ", optimized=" + optimized + "]";
    }

    /**
     * Gets the compiled code represented by a given compiled state object.
     */
    public static TargetMethod currentTargetMethod(Object compiledState, RuntimeCompiler.Nature nature) {
        if (compiledState instanceof Compilations) {
            // compiled
            return ((Compilations) compiledState).currentTargetMethod(nature);
        } else {
            assert compiledState instanceof Compilation : "unknown compiled state type: " + compiledState.getClass();
            // currently being compiled, return any previous target method
            return currentTargetMethod(((Compilation) compiledState).prevCompilations, nature);
        }
    }
}

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
package com.sun.max.vm.compiler;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.Compilations.Attr;

/**
 * The interface implemented by a compiler that translates {@link ClassMethodActor}s into {@link TargetMethod}s.
 */
public interface RuntimeCompiler {

    /**
     * A map from aliases to qualified class names for the known compilers.
     */
    @HOSTED_ONLY
    Map<String, String> aliases = Utils.addEntries(new HashMap<String, String>(),
                    "T1X", "com.sun.max.vm.t1x.T1X",
                    "C1X", "com.sun.max.vm.compiler.c1x.C1X");

    @HOSTED_ONLY
    OptionSet compilers = new OptionSet();

    /**
     * The option whose value (if non-null) specifies the class name of the optimizing compiler to use.
     */
    @HOSTED_ONLY
    Option<String> optimizingCompilerOption = compilers.newStringOption("opt", "C1X", "Specifies the class name of the optimizing compiler.");
    /**
     * The option whose value (if non-null) specifies the class name of the baseline compiler to use.
     */
    @HOSTED_ONLY
    Option<String> baselineCompilerOption = compilers.newStringOption("baseline", "T1X", "Specifies the baseline compiler class.");

    /**
     * Performs any specific initialization when entering a given VM phase.
     *
     * @param phase the VM phase that has just been entered
     */
    void initialize(MaxineVM.Phase phase);

    /**
     * Compiles a method to an internal representation.
     *
     * @param classMethodActor the method to compile
     * @param install specifies if the method should be installed in the code cache. Only methods in a code region can
     *            be executed. This option exists for the purpose of testing or benchmarking a compiler at runtime
     *            without polluting the code cache.
     * @param stats externally supplied statistics object to be used if not {@code null}
     * @return a reference to the target method created by this compiler for {@code classMethodActor}
     */
    TargetMethod compile(ClassMethodActor classMethodActor, boolean install, CiStatistics stats);

    /**
     * Determines if this compiler can support a {@linkplain CompilationScheme#synchronousCompile(ClassMethodActor, int) compilation request}
     * with the {@link Attr#INTERPRETER_COMPATIBLE} flag set.
     */
    boolean supportsInterpreterCompatibility();

    /**
     * Informs this compiler that the VM does not support deoptimization.
     */
    @HOSTED_ONLY
    void deoptimizationNotSupported();

    /**
     * Resets any metrics gathered by this compiler.
     */
    void resetMetrics();

    CallEntryPoint calleeEntryPoint();
}

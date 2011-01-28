/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.c1x.*;
import com.sun.max.annotate.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.type.*;

/**
 * Encapsulates the mechanism (or mechanisms) by which methods are prepared for execution.
 * Normally this means compiling (or recompiling) a method or creating an interpreter
 * stub.
 *
 * @author Ben L. Titzer
 */
public interface CompilationScheme extends VMScheme {

    /**
     * Temporary flag to disable alignment check when code patching. The alignment requirement
     * is satisfied by C1X (see {@link C1XOptions#AlignCallsForPatching}) but not yet by
     * CPS and the template JIT.
     */
    boolean CODE_PATCHING_ALIGMMENT_IS_GUARANTEED = System.getProperty("non-constant value to fool Eclipse") != null;

    @HOSTED_ONLY
    OptionSet compilers = new OptionSet();

    /**
     * The option whose value (if non-null) specifies the class name of the optimizing compiler to use.
     */
    @HOSTED_ONLY
    Option<String> optimizingCompilerOption = compilers.newStringOption("opt", C1XCompilerScheme.class.getName(), "Specifies the optimizing compiler class.");

    /**
     * The option whose value (if non-null) specifies the class name of the baseline compiler to use.
     */
    @HOSTED_ONLY
    Option<String> baselineCompilerOption = compilers.newStringOption("baseline", "com.sun.max.vm.cps.jit.amd64.AMD64JitCompiler", "Specifies the baseline compiler class.");

    /**
     * This method makes a target method for the specified method actor. If the method is already compiled, it will
     * return the current target method for the specified method. If the method is not compiled, it will perform
     * compilation according to this compilation scheme's internal policies and return the new target method. Note that
     * this method may return {@code null} if the internal compilation policy rejects compilation of the method (e.g. while
     * bootstrapping or at runtime if there is an interpreter installed). This method is <i>synchronous</i> in the
     * sense that it will wait for compilation to complete if this compilation scheme uses multiple background
     * compilation threads.
     *
     * @param classMethodActor the method for which to make the target method
     * @return the currently compiled version of a target method, if it exists; a new compiled version of the specified
     *         method according to the internal policies if it is not already compiled; null if the compilation policy
     *         denies compilation of the specified method
     */
    TargetMethod synchronousCompile(ClassMethodActor classMethodActor);

    /**
     * This method queries whether this compilation scheme is currently performing a compilation or has queued
     * compilations. This is necessary, for example, during bootstrapping to ensure that all compilations have
     * finished before proceeding to the next step in creating the image.
     *
     * @return true if there are any methods that are scheduled to be compiled that have not been completed yet
     */
    boolean isCompiling();

    boolean needsAdapters();

    /**
     * Adds a compilation observer to this compilation scheme. The observer will be notified before and after each
     * compilation.
     * @param observer the observer to add to this compilation scheme.
     *
     */
    void addObserver(CompilationObserver observer);

    /**
     * Removes a compilation observer from this compilation scheme. The observer will no longer be notified
     * before and after each compilation.
     * @param observer the observer to remove from this compilation scheme.
     *
     */
    void removeObserver(CompilationObserver observer);

    String description();

    /**
     * This class provides a facade for the {@code CompilationScheme} interface, simplifying usage. It provides a number
     * of utilities to, for example, compile a method, get a method's current entrypoint, reset its method state, etc.
     *
     * @author Ben L. Titzer
     */
    public final class Static {
        private Static() {
        }

        /**
         * Compile a method and return an address that represents its entrypoint for the specified call entrypoint. This
         * method performs a synchronous compile (i.e. waits for the compilation to complete before returning the
         * entrypoint).
         *
         * @param classMethodActor the method to compile
         * @param callEntryPoint the call entrypoint into the target code (@see CallEntryPoint)
         * @return an address representing the entrypoint to the compiled code of the specified method
         */
        public static Address compile(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
            TargetMethod current;
            Object targetState = classMethodActor.targetState;
            if (targetState instanceof TargetMethod) {
                // fast path: method is already compiled just once
                current = (TargetMethod) targetState;
            } else {
                if (MaxineVM.isHosted() && CPSCompiler.Static.compiler().compiledType() == null) {
                    return MethodID.fromMethodActor(classMethodActor).asAddress();
                }
                // slower path: method has not been compiled, or been compiled more than once
                current = vmConfig().compilationScheme().synchronousCompile(classMethodActor);
            }
            return current.getEntryPoint(callEntryPoint).asAddress();
        }

        /**
         * Get the entrypoint to a method that has already been compiled. This is needed, for example, at startup, to
         * get the entrypoint address of some critical VM methods.
         *
         * @param classMethodActor the method for which to get the entrypoint
         * @param callEntryPoint the call entrypoint for which to get the address (@see CallEntryPoint)
         * @return an address representing the entrypoint to the compiled code of the specified method
         */
        public static Address getCriticalEntryPoint(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
            return classMethodActor.currentTargetMethod().getEntryPoint(callEntryPoint).asAddress();
        }

        /**
         * Get the current target method for the specified class method actor.
         *
         * @param classMethodActor the method for which to get the target method
         * @return a reference to the current target method, if it exists; {@code null} otherwise
         */
        public static TargetMethod getCurrentTargetMethod(ClassMethodActor classMethodActor) {
            return classMethodActor.currentTargetMethod();
        }

        /**
         * Reset the method state, destroying any previous information about the compilation of the method. This method
         * should only be used in very specific circumstances to force recompilation of a method and is NOT FOR GENERAL
         * USE.
         *
         * @param classMethodActor the method for which to reset the method state
         */
        @HOSTED_ONLY
        public static void resetMethodState(ClassMethodActor classMethodActor) {
            classMethodActor.targetState = null;
        }

        public static void instrumentationCounterOverflow(MethodProfile mpo, int mpoIndex) {
            ClassMethodActor classMethodActor = (ClassMethodActor) mpo.method;
            TargetMethod oldMethod = TargetState.currentTargetMethod(classMethodActor.targetState);
            TargetMethod newMethod = vmConfig().compilationScheme().synchronousCompile(classMethodActor);
            if (newMethod != oldMethod) {
                oldMethod.forwardTo(newMethod);
            }
        }

    }

    /**
     * Collection of methods (the public ones) to be called by all implementations when
     * specified events occur; these supports certain Inspector services.
     *
     * @author Michael Van De Vanter
     */
    public static final class Inspect {

        /**
         * Announces that a compilation is being started; must be called for
         * certain Inspector services to work.
         *
         * @param method a method about to be compiled
         */
        public static void notifyCompilationStart(ClassMethodActor method) {
            InspectableCodeInfo.notifyCompilationEvent(method, null);
        }

        /**
         * Announces that a compilation has just been completed; must be called for
         * certain Inspector services to work.
         *
         * @param targetMethod a compilation that was just completed.
         */
        public static void notifyCompilationComplete(TargetMethod targetMethod) {
            InspectableCodeInfo.notifyCompilationEvent(targetMethod.classMethodActor, targetMethod);
            inspectableCompilationComplete(targetMethod);
        }

        /**
         * An empty method whose purpose is to be interrupted by the Inspector
         * at the conclusion of a method compilation.
         * <br>
         * This particular method is intended for use by users of the Inspector, and
         * is separate from a method used by the Inspector for internal use.
         * <br>
         * <strong>Important:</strong> The Inspector assumes that this method is loaded
         * and compiled in the boot image and that it will never be dynamically recompiled.
         *
         * @param targetMethod the method compilation just created
         */
        @INSPECTED
        @NEVER_INLINE
        private static void inspectableCompilationComplete(TargetMethod targetMethod) {
        }

        // Ensure that the above method is compiled into the boot image so that it can be inspected conveniently
        private static CriticalMethod inspectableCompilationCompleteCriticalMethod =
            new CriticalMethod(CompilationScheme.Inspect.class, "inspectableCompilationComplete", SignatureDescriptor.create(void.class, TargetMethod.class));
    }
}

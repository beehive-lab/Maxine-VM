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
package com.sun.max.vm.compiler;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.MethodProfile;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;

/**
 * This interface represents a compilation system that coordinations compilation and
 * recompilation between multiple compilers for the rest of the VM, e.g. in response
 * to dynamic feedback.
 *
 * @author Ben L. Titzer
 */
public interface CompilationScheme extends VMScheme {
    /**
     * An enum that selects between different runtime behavior of the compilation scheme.
     */
    public enum Mode {
        /**
         * Use the JIT compiler only (except for unsafe code).
         */
        JIT,

        /**
         * Use the interpreter only (except for unsafe code).
         */
        INTERPRETED,

        /**
         * Use the optimizing compiler only (except for some invocation stubs).
         */
        OPTIMIZED,

        /**
         * Use both compilers according to dynamic feedback.
         */
        MIXED,

        /**
         * Use the JIT compiler if possible when creating the boot image.
         */
        PROTOTYPE_JIT
    }

    /**
     * @return the operation mode in effect
     */
    Mode mode();

    /**
     * Set the operating mode for this compilation scheme.
     * @param mode the new mode
     */
    void setMode(Mode mode);

    /**
     * This method makes a target method for the specified method actor. If the method is already compiled, it will
     * return the current target method for the specified method. If the method is not compiled, it will perform
     * compilation according to this compilation scheme's internal policies and return the new target method. Note that
     * this method may return {@code null} if the internal compilation policy rejects compilation of the method (e.g. at
     * prototyping time or at runtime if there is an interpreter installed). This method is <i>synchronous</i> in the
     * sense that it will wait for compilation to complete if this compilation scheme uses multiple background
     * compilation threads.
     *
     * @param classMethodActor the method for which to make the target method
     * @return the currently compiled version of a target method, if it exists; a new compiled version of the specified
     *         method according to the internal policies if it is not already compiled; null if the compilation policy
     *         denies compilation of the specified method
     */
    TargetMethod synchronousCompile(ClassMethodActor classMethodActor);

    TargetMethod synchronousCompile(ClassMethodActor classMethodActor, RuntimeCompilerScheme compiler);

    /**
     * This method queries whether this compilation scheme is currently performing a compilation or has queued
     * compilations. This is necessary, for example, during prototyping time to ensure that all compilations have
     * finished before proceeding to the next step in creating the image.
     *
     * @return true if there are any methods that are scheduled to be compiled that have not been completed yet
     */
    boolean isCompiling();

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
                if (MaxineVM.isPrototyping() && !TargetMethod.class.isAssignableFrom(VMConfiguration.target().compilerScheme().irGenerator().irMethodType)) {
                    return MethodID.fromMethodActor(classMethodActor).asAddress();
                }
                // slower path: method has not been compiled, or been compiled more than once
                current = VMConfiguration.target().compilationScheme().synchronousCompile(classMethodActor);
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
        public static void resetMethodState(ClassMethodActor classMethodActor) {
            classMethodActor.targetState = null;
        }

        /**
         * Reset the method state and force a recompile of the specified method. This method is NOT RECOMMENDED
         * FOR GENERAL USE.
         *
         * @param classMethodActor the method to recompile
         * @return the new target method for specified method
         */
        public static TargetMethod forceFreshCompile(ClassMethodActor classMethodActor) {
            resetMethodState(classMethodActor);
            compile(classMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
            return getCurrentTargetMethod(classMethodActor);
        }

        public static void instrumentationCounterOverflow(MethodProfile mpo, int mpoIndex) {
            // TODO: re-implement re-compilation
        }
    }
}

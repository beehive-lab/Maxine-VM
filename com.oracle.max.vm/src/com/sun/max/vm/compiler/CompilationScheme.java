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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.intrinsics.Infopoints.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

/**
 * Encapsulates the mechanism (or mechanisms) by which methods are prepared for execution.
 * Normally this means compiling (or recompiling) a method or creating an interpreter
 * stub.
 */
public interface CompilationScheme extends VMScheme {

    /**
     * Produces a target method for the specified method actor. If another thread is currently
     * compiling {@code cma}, then the result of that compilation is returned. Otherwise,
     * a new compilation is scheduled and its result is returned. Either way, this methods
     * waits for the result of a compilation to return it.
     *
     * @param cma the method for which to make the target method
     * @param nature the specific type of target method required or {@code null} if any target method is acceptable
     * @return a newly compiled version of a {@code cma}
     * @throws InteralError if an uncaught exception is thrown during compilation
     */
    TargetMethod synchronousCompile(ClassMethodActor cma, Nature nature);

    boolean needsAdapters();

    String description();

    boolean isDeoptSupported();

    /**
     * This class provides a facade for the {@code CompilationScheme} interface, simplifying usage. It provides a number
     * of utilities to, for example, compile a method, get a method's current entrypoint, reset its method state, etc.
     *
     */
    public final class Static {
        private Static() {
        }

        /**
         * Gets compiled code for a given method.
         *
         * @param classMethodActor the method to compile
         * @param nature the specific type of target method required or {@code null} if any target method is acceptable
         * @return the compiled method
         */
        public static TargetMethod compile(ClassMethodActor classMethodActor, RuntimeCompiler.Nature nature) {
            TargetMethod currentTargetMethod = Compilations.currentTargetMethod(classMethodActor.compiledState, nature);
            if (currentTargetMethod != null) {
                // fast path: a suitable compiled version of method is available
                return currentTargetMethod;
            }

            // slow path: a suitable compiled version of method is *not* available
            return vmConfig().compilationScheme().synchronousCompile(classMethodActor, nature);
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
         * Reset the compiled state for a given method. This method
         * should only be used in very specific circumstances to force recompilation of a method and is NOT FOR GENERAL
         * USE.
         *
         * @param classMethodActor the method for which to reset the method state
         */
        @HOSTED_ONLY
        public static void resetCompiledState(ClassMethodActor classMethodActor) {
            classMethodActor.compiledState = Compilations.EMPTY;
        }

        /**
         * Helper class for patching any direct call sites on the stack corresponding to a target method
         * being replaced by a recompiled version.
         */
        static class DirectCallPatcher extends RawStackFrameVisitor {

            /**
             * The maximum number of frames to search for a patchable direct call site.
             */
            static final int FRAME_SEARCH_LIMIT = 10;

            private final Address from1;
            private final Address from2;
            private final Address to1;
            private final Address to2;
            int frameCount;

            public DirectCallPatcher(TargetMethod oldMethod, TargetMethod newMethod) {
                from1 = oldMethod.getEntryPoint(BASELINE_ENTRY_POINT).asAddress();
                to1 = newMethod.getEntryPoint(BASELINE_ENTRY_POINT).asAddress();
                from2 = oldMethod.getEntryPoint(OPTIMIZED_ENTRY_POINT).asAddress();
                to2 = newMethod.getEntryPoint(OPTIMIZED_ENTRY_POINT).asAddress();
            }

            @Override
            public boolean visitFrame(Cursor current, Cursor callee) {
                if (platform().isa == ISA.AMD64) {
                    if (current.isTopFrame()) {
                        return true;
                    }
                    Pointer ip = current.ip();
                    Pointer callSite = ip.minus(AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);
                    if ((callSite.readByte(0) & 0xFF) == AMD64TargetMethodUtil.RIP_CALL) {
                        Pointer target = ip.plus(callSite.readInt(1));
                        if (target.equals(from1)) {
                            logStaticCallPatch(current, callSite, to1);
                            AMD64TargetMethodUtil.mtSafePatchCallDisplacement(current.targetMethod(), callSite, to1);
                            // Stop traversing the stack after a direct call site has been patched
                            return false;
                        }
                        if (target.equals(from2)) {
                            logStaticCallPatch(current, callSite, to2);
                            AMD64TargetMethodUtil.mtSafePatchCallDisplacement(current.targetMethod(), callSite, to2);
                            // Stop traversing the stack after a direct call site has been patched
                            return false;
                        }
                    }
                    if (++frameCount > FRAME_SEARCH_LIMIT) {
                        logNoFurtherStaticCallPatching();
                        return false;
                    }
                    return true;
                }
                throw FatalError.unimplemented();
            }
        }

        /**
         * Handles an instrumentation counter overflow upon entry to a profiled method.
         * This method must be called on the thread that overflowed the counter.
         *
         * @param mpo profiling object (including the method itself)
         * @param receiver the receiver object of the profiled method. This will be {@code null} if the profiled method is static.
         */
        public static void instrumentationCounterOverflow(MethodProfile mpo, Object receiver) {
            if (Heap.isAllocationDisabledForCurrentThread()) {
                logCounterOverflow(mpo, "Stopped recompilation because allocation is currently disabled");
                // We don't want to see another counter overflow in the near future
                mpo.entryCount = 1000;
                return;
            }
            if (Compilation.isCompilationRunningInCurrentThread()) {
                logCounterOverflow(mpo, "Stopped recompilation because compilation is running in current thread");
                // We don't want to see another counter overflow in the near future
                mpo.entryCount = 1000;
                return;
            }

            ClassMethodActor classMethodActor = mpo.method.classMethodActor;
            TargetMethod oldMethod = mpo.method;
            TargetMethod newMethod = Compilations.currentTargetMethod(classMethodActor.compiledState, null);

            if (oldMethod == newMethod || newMethod == null) {
                if (!(classMethodActor.compiledState instanceof Compilation)) {
                    // There is no newer compiled version available yet that we could just patch to, so recompile
                    logCounterOverflow(mpo, "");
                    try {
                        newMethod = vmConfig().compilationScheme().synchronousCompile(classMethodActor, Nature.OPT);
                    } catch (InternalError e) {
                        if (VMOptions.verboseOption.verboseCompilation) {
                            e.printStackTrace(Log.out);
                        }
                        // Optimization failed - stay with the baseline method. By not resetting the counter,
                        // the next counter overflow (due to integer wrapping) will be a while away.
                        return;
                    }
                }
            }


            if (oldMethod == newMethod || newMethod == null) {
                // No compiled method available yet, maybe compilation is pending.
                // We don't want to see another counter overflow in the near future.
                mpo.entryCount = 10000;
            } else {
                assert newMethod != null : oldMethod;
                logPatching(classMethodActor, oldMethod, newMethod);
                mpo.entryCount = 0;

                if (receiver != null) {
                    Address from = oldMethod.getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
                    Address to = newMethod.getEntryPoint(VTABLE_ENTRY_POINT).asAddress();

                    // Simply overwrite all vtable slots containing 'oldMethod' with 'newMethod'.
                    // These updates can be made atomically without need for a lock.
                    Hub hub = ObjectAccess.readHub(receiver);
                    for (int i = 0; i < hub.vTableLength(); i++) {
                        int index = Hub.vTableStartIndex() + i;
                        if (hub.getWord(index).equals(from)) {
                            logDispatchTablePatch(classMethodActor, from, to, hub, index, "vtable");
                            hub.setWord(index, to);
                        }
                    }

                    for (int i = 0; i < hub.iTableLength; i++) {
                        int index = hub.iTableStartIndex + i;
                        if (hub.getWord(index).equals(from)) {
                            logDispatchTablePatch(classMethodActor, from, to, hub, index, "itable");
                            hub.setWord(index, to);
                        }
                    }
                }

                // Look for a static call to 'oldMethod' and patch it.
                // This occurs even if 'classMethodActor' is non-static
                // as it may have been called directly.
                DirectCallPatcher patcher = new DirectCallPatcher(oldMethod, newMethod);
                new VmStackFrameWalker(VmThread.current().tla()).inspect(Pointer.fromLong(here()),
                                VMRegister.getCpuStackPointer(),
                                VMRegister.getCpuFramePointer(),
                                patcher);
            }
        }

        private static void logCounterOverflow(MethodProfile mpo, String msg) {
            if (VMOptions.verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.print(": Invocation counter overflow of ");
                Log.printMethod(mpo.method, false);
                Log.print(" counter ");
                Log.print(mpo.entryCount);
                Log.print("  ");
                Log.print(msg);
                Log.println();
                Log.unlock(lockDisabledSafepoints);
            }
        }

        private static void logPatching(ClassMethodActor classMethodActor, TargetMethod oldMethod, TargetMethod newMethod) {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.print(": Patching for method ");
                Log.printMethod(classMethodActor, false);
                Log.print(" oldMethod ");
                Log.print(oldMethod.getEntryPoint(BASELINE_ENTRY_POINT));
                Log.print(" newMethod ");
                Log.print(newMethod.getEntryPoint(BASELINE_ENTRY_POINT));
                Log.println();
                Log.unlock(lockDisabledSafepoints);
            }
        }

        private static void logDispatchTablePatch(ClassMethodActor classMethodActor, final Address from, final Address to, Hub hub, int index, String table) {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.print(": Patching ");
                Log.print(hub.classActor.name());
                Log.print('.');
                Log.print(table);
                Log.print('[');
                Log.print(index);
                Log.print("] {");
                Log.printMethod(classMethodActor, false);
                Log.print("} ");
                Log.print(from);
                Log.print(" -> ");
                Log.println(to);
                Log.unlock(lockDisabledSafepoints);
            }
        }

        private static void logStaticCallPatch(Cursor current, Pointer callSite, Address to) {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.print(": Patching static call at ");
                Log.printLocation(current.targetMethod(), callSite, false);
                Log.print(" to ");
                Log.println(to);
                Log.unlock(lockDisabledSafepoints);
            }
        }

        private static void logNoFurtherStaticCallPatching() {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.printCurrentThread(false);
                Log.println(": No further patching of static calls");
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }
}

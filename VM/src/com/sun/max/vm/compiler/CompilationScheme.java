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

import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.VMOptions.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
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
     */
    public final class Static {
        private Static() {
        }

        /**
         * Compiles a method.
         *
         * @param classMethodActor the method to compile
         * @return the compiled method
         */
        public static TargetMethod compile(ClassMethodActor classMethodActor) {
            TargetMethod current;
            Object targetState = classMethodActor.targetState;
            if (targetState instanceof TargetMethod) {
                // fast path: method is already compiled just once
                current = (TargetMethod) targetState;
            } else {
                // slower path: method has not been compiled, or been compiled more than once
                current = vmConfig().compilationScheme().synchronousCompile(classMethodActor);
            }
            return current;
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

        /**
         * Helper class for patching any direct call sites on the stack corresponding to a target method
         * being replaced by a recompiled version.
         */
        static class DirectCallPatcher extends RawStackFrameVisitor {

            /**
             * The maximum number of frames to search for a patchable direct call site.
             */
            static final int FRAME_SEARCH_LIMIT = 10;

            final Address from;
            final Address to;
            int frameCount;

            public DirectCallPatcher(Address oldAddr, Address newAddr) {
                this.from = oldAddr;
                this.to = newAddr;
            }

            @Override
            public boolean visitFrame(Cursor current, Cursor callee) {
                if (platform().isa == ISA.AMD64) {
                    if (current.isTopFrame()) {
                        return true;
                    }
                    Pointer ip = current.ip();
                    Pointer callSite = ip.minus(AMD64TargetMethodUtil.RIP_CALL_INSTRUCTION_SIZE);
                    if (callSite.readByte(0) == AMD64TargetMethodUtil.RCALL) {
                        Pointer target = ip.plus(callSite.readInt(1));
                        if (target.equals(from)) {
                            logStaticCallPatch(current, callSite, to);
                            AMD64TargetMethodUtil.mtSafePatchCallDisplacement(current.targetMethod(), callSite, to);
                            // Stop traversing the stack after a direct call site has been patched
                            return false;
                        }
                    }
                    if (++frameCount > FRAME_SEARCH_LIMIT) {
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
            ClassMethodActor classMethodActor = mpo.method;
            TargetMethod oldMethod = TargetState.currentTargetMethod(classMethodActor.targetState);
            TargetMethod newMethod = vmConfig().compilationScheme().synchronousCompile(classMethodActor);

            if (newMethod != oldMethod) {
                final Address from = oldMethod.getEntryPoint(VTABLE_ENTRY_POINT).asAddress();
                final Address to = newMethod.getEntryPoint(VTABLE_ENTRY_POINT).asAddress();

                if (receiver != null) {
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
                DirectCallPatcher patcher = new DirectCallPatcher(from, to);
                new VmStackFrameWalker(VmThread.current().tla()).inspect(Pointer.fromLong(here()),
                                VMRegister.getCpuStackPointer(),
                                VMRegister.getCpuFramePointer(),
                                patcher);
            }
        }

        public static void logDispatchTablePatch(ClassMethodActor classMethodActor, final Address from, final Address to, Hub hub, int index, String table) {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("Patching ");
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

        public static void logStaticCallPatch(Cursor current, Pointer callSite, Address to) {
            if (verboseOption.verboseCompilation) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("Patching call at ");
                Log.printMethod(current.targetMethod(), false);
                Log.print('+');
                Log.print(callSite.minus(current.targetMethod().codeStart()).toInt());
                Log.print(" to ");
                Log.println(to);
                Log.unlock(lockDisabledSafepoints);
            }
        }
    }
}

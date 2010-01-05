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
package com.sun.max.vm.compiler.snippet;

import static com.sun.max.vm.runtime.Safepoint.*;
import static com.sun.max.vm.runtime.VMRegister.*;
import static com.sun.max.vm.stack.JavaFrameAnchor.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.interpreter.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Snippets that are used in {@linkplain NativeStubGenerator native method stubs}.
 *
 * @author Doug Simon
 * @author Hannes Payer
 */
public abstract class NativeStubSnippet extends NonFoldableSnippet {

    /**
     * Finds the address of the native function for a {@code native} Java method.
     * <p>
     * This snippet does not subclass {@link NonFoldableSnippet} as it needs to be foldable when executing an
     * {@linkplain IrInterpreter IR interpreter}.
     */
    public static final class LinkNativeMethod extends Snippet {
        @Override
        public boolean isFoldable(IrValue[] arguments) {
            if (MaxineVM.isHosted() || !super.isFoldable(arguments)) {
                return false;
            }
            try {
                final ClassMethodActor classMethodActor = (ClassMethodActor) arguments[0].value().asObject();
                return !classMethodActor.nativeFunction.link().isZero();
            } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                return false;
            }
        }

        @SNIPPET
        public static Word linkNativeMethod(ClassMethodActor classMethodActor) {
            return classMethodActor.nativeFunction.link();
        }

        public static final LinkNativeMethod SNIPPET = new LinkNativeMethod();
    }

    private static final VmThreadLocal NATIVE_CALLS_DISABLED = new VmThreadLocal("NATIVE_CALLS_DISABLED", false, "");

    /**
     * Disables calling native methods on the current thread. This state is recursive. That is,
     * natives calls are only re-enabled once {@link #enableNativeCallsForCurrentThread()} is
     * called the same number of times as this method has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #enableNativeCallsForCurrentThread()}
     * are unbalanced.
     *
     * Note: This feature is only provided as a debugging aid. It imposes an overhead (a test and branch on a VM thread local)
     * on every native call. It could be removed or disabled in a product build of the VM once GC is debugged.
     */
    public static void disableNativeCallsForCurrentThread() {
        final Address value = NATIVE_CALLS_DISABLED.getConstantWord().asAddress();
        NATIVE_CALLS_DISABLED.setConstantWord(value.plus(1));
    }

    /**
     * Re-enables calling native methods on the current thread. This state is recursive. That is,
     * native calls are only re-enabled once this method is called the same number of times as
     * {@link #disableNativeCallsForCurrentThread()} has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #disableNativeCallsForCurrentThread()}
     * are unbalanced.
     */
    public static void enableNativeCallsForCurrentThread() {
        final Address value = NATIVE_CALLS_DISABLED.getConstantWord().asAddress();
        if (value.isZero()) {
            FatalError.unexpected("Unbalanced calls to disable/enable native calls for current thread");
        }
        NATIVE_CALLS_DISABLED.setConstantWord(value.minus(1));
    }

    /**
     * Performs any operations necessary immediately before entering native code.
     */
    public static final class NativeCallPrologue extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallPrologue() {
            Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
            Word previousAnchor = enabledVmThreadLocals.getWord(LAST_JAVA_FRAME_ANCHOR.index);
            Pointer anchor = JavaFrameAnchor.create(getCpuStackPointer(), getCpuFramePointer(), getInstructionPointer(), previousAnchor);

            nativeCallPrologue0(enabledVmThreadLocals, anchor);
        }

        /**
         * Makes the transition from the 'in Java' state to the 'in native' state.
         *
         * @param enabledVmThreadLocals the safepoints-triggered VM thread locals for the current thread
         * @param anchor the value to which {@link VmThreadLocal#LAST_JAVA_FRAME_ANCHOR} will be set just before
         *            the transition is made
         */
        @INLINE
        public static void nativeCallPrologue0(Pointer enabledVmThreadLocals, Word anchor) {
            if (!NATIVE_CALLS_DISABLED.getConstantWord().isZero()) {
                FatalError.unexpected("Calling native code while native calls are disabled");
            }

            // Update the last Java frame anchor for the current thread:
            enabledVmThreadLocals.setWord(LAST_JAVA_FRAME_ANCHOR.index, anchor);

            if (Safepoint.UseCASBasedGCMutatorSynchronization) {
                enabledVmThreadLocals.setWord(MUTATOR_STATE.index, THREAD_IN_NATIVE);
            } else {
                MemoryBarrier.memopStore();
                // The following store must be last:
                enabledVmThreadLocals.setWord(MUTATOR_STATE.index, THREAD_IN_NATIVE);
            }
        }

        public static final NativeCallPrologue SNIPPET = new NativeCallPrologue();
    }

    /**
     * Performs any operations necessary immediately after returning from native code.
     */
    public static final class NativeCallEpilogue extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallEpilogue() {
            Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
            Pointer anchor = enabledVmThreadLocals.getWord(LAST_JAVA_FRAME_ANCHOR.index).asPointer();
            nativeCallEpilogue0(enabledVmThreadLocals, PREVIOUS.get(anchor));
        }

        /**
         * Makes the transition from the 'in native' state to the 'in Java' state, blocking on a
         * spin lock if a GC is currently underway.
         *
         * @param enabledVmThreadLocals the safepoints-triggered VM thread locals for the current thread
         * @param anchor the value to which {@link VmThreadLocal#LAST_JAVA_FRAME_ANCHOR} will be set just after
         *            the transition is made
         */
        @INLINE
        public static void nativeCallEpilogue0(Pointer enabledVmThreadLocals, Pointer anchor) {
            spinUntilGCFinished(enabledVmThreadLocals);
            enabledVmThreadLocals.setWord(LAST_JAVA_FRAME_ANCHOR.index, anchor);
        }

        /**
         * This methods spins in a busy loop while a garbage collection is currently running.
         */
        @INLINE
        @NO_SAFEPOINTS("Cannot take a trap while GC is running")
        private static void spinUntilGCFinished(Pointer enabledVmThreadLocals) {
            if (UseCASBasedGCMutatorSynchronization) {
                while (true) {
                    if (enabledVmThreadLocals.getWord(MUTATOR_STATE.index).equals(THREAD_IN_NATIVE)) {
                        if (enabledVmThreadLocals.compareAndSwapWord(MUTATOR_STATE.offset, THREAD_IN_NATIVE, THREAD_IN_JAVA).equals(THREAD_IN_NATIVE)) {
                            break;
                        }
                    }
                    SpecialBuiltin.pause();
                }
            } else {
                while (true) {
                    // Signal that we intend to go back into Java:
                    enabledVmThreadLocals.setWord(MUTATOR_STATE.index, THREAD_IN_JAVA);

                    // Ensure that the GC sees the above state transition:
                    MemoryBarrier.storeLoad();

                    // Ask if GC is in progress:
                    if (enabledVmThreadLocals.getWord(GC_STATE.index).isZero()) {
                        // If GC was not in progress then the state transition above was valid (common case)
                        return;
                    }

                    // GC is in progress (same one or a subsequent one) so above state transition is invalid
                    // so undo it and spin until GC is finished and then retry transition
                    enabledVmThreadLocals.setWord(MUTATOR_STATE.index, THREAD_IN_NATIVE);
                    while (!enabledVmThreadLocals.getWord(GC_STATE.index).isZero()) {
                        // Spin without doing unnecessary stores
                        SpecialBuiltin.pause();
                    }
                }
            }
        }

        public static final NativeCallEpilogue SNIPPET = new NativeCallEpilogue();
    }

    /**
     * Saves information about the last Java caller for direct/C_FUNCTION calls.
     * Used by the Inspector for debugging.
     *
     * ATTENTION: If this is ever used for anything else than the inspector,
     *            use memory barriers properly.
     */
    public static final class NativeCallPrologueForC extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallPrologueForC() {
            Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
            Word previousAnchor = enabledVmThreadLocals.getWord(LAST_JAVA_FRAME_ANCHOR.index);
            Pointer anchor = JavaFrameAnchor.create(VMRegister.getCpuStackPointer(), VMRegister.getCpuFramePointer(), VMRegister.getInstructionPointer(), previousAnchor);
            enabledVmThreadLocals.setWord(LAST_JAVA_FRAME_ANCHOR.index, anchor);
        }

        public static final NativeCallPrologueForC SNIPPET = new NativeCallPrologueForC();
    }

    public static final class NativeCallEpilogueForC extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallEpilogueForC() {
            Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord().asPointer();
            Pointer anchor = enabledVmThreadLocals.getWord(LAST_JAVA_FRAME_ANCHOR.index).asPointer();
            enabledVmThreadLocals.setWord(LAST_JAVA_FRAME_ANCHOR.index, PREVIOUS.get(anchor));
        }

        public static final NativeCallEpilogueForC SNIPPET = new NativeCallEpilogueForC();
    }
}

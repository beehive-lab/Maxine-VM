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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Snippets that are used in {@linkplain NativeStubGenerator native method stubs}.
 *
 * @author Doug Simon
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
            if (MaxineVM.isPrototyping() || !super.isFoldable(arguments)) {
                return false;
            }
            try {
                final ClassMethodActor classMethodActor = (ClassMethodActor) arguments[0].value().asObject();
                return !classMethodActor.nativeFunction().isZero();
            } catch (UnsatisfiedLinkError unsatisfiedLinkError) {
                return false;
            }
        }

        @SNIPPET
        public static Word linkNativeMethod(ClassMethodActor classMethodActor) {
            return classMethodActor.nativeFunction();
        }

        public static final LinkNativeMethod SNIPPET = new LinkNativeMethod();
    }

    /**
     * Performs any operations necessary immediately before entering native code.
     */
    public static final class NativeCallPrologue extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static Word nativeCallPrologue() {
            final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
            LAST_JAVA_CALLER_FRAME_POINTER.setVariableWord(vmThreadLocals, VMRegister.getCpuFramePointer());
            LAST_JAVA_CALLER_STACK_POINTER.setVariableWord(vmThreadLocals, VMRegister.getCpuStackPointer());
            MemoryBarrier.storeStore(); // The following store must be last:
            LAST_JAVA_CALLER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, VMRegister.getInstructionPointer());
            return vmThreadLocals;
        }

        public static final NativeCallPrologue SNIPPET = new NativeCallPrologue();
    }

    /**
     * Performs any operations necessary immediately after returning from native code.
     */
    public static final class NativeCallEpilogue extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallEpilogue(Pointer vmThreadLocals) {
            Safepoint.hard();

            // Set the current instruction pointer in TLS to zero to indicate the transition back into Java code
            LAST_JAVA_CALLER_INSTRUCTION_POINTER.setVariableWord(vmThreadLocals, Word.zero());
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
        public static Word nativeCallPrologueForC() {
            final Pointer vmThreadLocals = VmThread.currentVmThreadLocals();
            LAST_JAVA_CALLER_FRAME_POINTER_FOR_C.setVariableWord(vmThreadLocals, VMRegister.getCpuFramePointer());
            LAST_JAVA_CALLER_STACK_POINTER_FOR_C.setVariableWord(vmThreadLocals, VMRegister.getCpuStackPointer());
            LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C.setVariableWord(vmThreadLocals, VMRegister.getInstructionPointer());
            return vmThreadLocals;
        }

        public static final NativeCallPrologueForC SNIPPET = new NativeCallPrologueForC();
    }

    public static final class NativeCallEpilogueForC extends NativeStubSnippet {
        @SNIPPET
        @INLINE
        public static void nativeCallEpilogueForC(Pointer vmThreadLocals) {
            // Set the current instruction pointer in TLS to zero to indicate the transition back into Java code
            LAST_JAVA_CALLER_INSTRUCTION_POINTER_FOR_C.setVariableWord(vmThreadLocals, Word.zero());
        }

        public static final NativeCallEpilogueForC SNIPPET = new NativeCallEpilogueForC();
    }

}

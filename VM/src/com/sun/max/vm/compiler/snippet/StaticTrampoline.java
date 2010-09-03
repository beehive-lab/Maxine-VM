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

import static com.sun.max.vm.VMConfiguration.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;

/**
 * Implements dynamic linking of calls to static and special methods
 * by instruction patching and then returning to right before the respective call instruction.
 *
 * @author Bernd Mathiske
 */
public final class StaticTrampoline extends Snippet {

    private StaticTrampoline() {
        new CriticalMethod(executable, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
    }

    /**
     * Method called at static trampoline call site.
     * Forward the call to the static trampoline of the compiler. This one knows the detail of the calling convention being use, and can locate
     * the address of the caller, and perform the actual patch of the call instruction.
     */
    @SNIPPET
    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.STATIC)
    @NEVER_INLINE
    public static void staticTrampoline() throws Throwable {
        vmConfig().bootCompilerScheme().staticTrampoline();
    }

    private static final StaticTrampoline snippet = new StaticTrampoline();

    public static StaticTrampoline snippet() {
        return snippet;
    }

    @RESET
    private static Pointer codeStart = Pointer.zero();
    @RESET
    private static Pointer jitEntryPoint;
    @RESET
    private static Pointer optEntryPoint;

    public static Pointer codeStart() {
        if (codeStart.isZero()) {
            codeStart = CompilationScheme.Static.getCurrentTargetMethod(snippet.executable).codeStart();
            optEntryPoint = codeStart.plus(CallEntryPoint.OPTIMIZED_ENTRY_POINT.offset());
            jitEntryPoint = codeStart.plus(CallEntryPoint.JIT_ENTRY_POINT.offset());
        }
        return codeStart;
    }

    public static boolean isEntryPoint(Pointer instructionPointer) {
        codeStart(); // Ensures static fields are (re)initialized
        return instructionPointer.equals(optEntryPoint) || instructionPointer.equals(jitEntryPoint);
    }
}

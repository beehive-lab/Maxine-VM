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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.runtime.*;

/**
 * Implements dynamic linking of calls to static and special methods
 * by instruction patching and then returning to right before the respective call instruction.
 *
 * @author Bernd Mathiske
 */
public final class StaticTrampoline extends NonFoldableSnippet {

    private final CriticalMethod _classMethodActor = new CriticalMethod(classMethodActor(), CallEntryPoint.OPTIMIZED_ENTRY_POINT);

    private StaticTrampoline() {
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
        VMConfiguration.target().compilerScheme().staticTrampoline();
    }

    private static final StaticTrampoline _snippet = new StaticTrampoline();

    public static StaticTrampoline snippet() {
        return _snippet;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private static Pointer _codeStart = Pointer.zero();

    public static Pointer codeStart() {
        if (_codeStart.isZero()) {
            _codeStart = CompilationScheme.Static.getCurrentTargetMethod(_snippet.classMethodActor()).codeStart();
        }
        return _codeStart;
    }
}

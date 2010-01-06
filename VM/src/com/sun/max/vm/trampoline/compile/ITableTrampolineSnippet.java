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
package com.sun.max.vm.trampoline.compile;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * Implements dynamic linking of calls through iTables .
 *
 * @author Bernd Mathiske
 * @author Laurent Daynes
 */
public final class ITableTrampolineSnippet extends Snippet {

    private ITableTrampolineSnippet() {
    }

    private static final ITableTrampolineSnippet snippet = new ITableTrampolineSnippet();

    private static final TrampolineGenerator trampolineGenerator = new RecompileTrampolineGenerator.VtableTrampolineGenerator(snippet.executable);

    @SNIPPET
    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.INTERFACE)
    private static Address iTableTrampolineSnippet(Object receiver) throws Throwable {
        final DynamicTrampoline dynamicTrampoline = RecompileTrampolineGenerator.ItableTrampolineGenerator.iTableTrampoline();
        return dynamicTrampoline.trampolineReturnAddress(receiver, VMRegister.getCpuStackPointer());
    }

    public static synchronized Address makeCallEntryPoint(int vTableIndex) {
        return trampolineGenerator.makeCallEntryPoint(vTableIndex);
    }

    public static synchronized boolean isITableTrampoline(MethodActor classMethodActor) {
        return classMethodActor == snippet.executable;
    }

    public static synchronized boolean isITableTrampolineInstructionPointer(Address instructionPointer) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod == null) {
            return false;
        }
        return targetMethod.classMethodActor() == snippet.executable;
    }
}

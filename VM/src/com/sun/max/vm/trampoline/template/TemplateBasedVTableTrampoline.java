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
package com.sun.max.vm.trampoline.template;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;

/**
 * Implements Just-in-time compilation of virtual method on first use.
 * All entries of virtual tables (<em>vtables</em>) are initialized with a vtable trampoline
 * (except for those that can be inherited from super-classes and aren't overridden).
 * Upon first invocation of a virtual method, execution is routed to the trampoline which
 * compiles the method (if not already done), sets the vtable entry appropriately and re-executes
 * the virtual call which this time dispatches to compiled code.
 *
 * Virtual methods with the same vtable index share the same trampoline. The trampolines
 * for virtual methods with different vtable indices differ only by a single immediate operand
 * holding the vtable index. Thus, to build a new trampoline, one can just take a copy of a template and
 * edit the instruction to change the vtable index immediate operand accordingly.
 *
 * @see TemplateBasedTrampolineGenerator
 *
 * @author Laurent Daynes
 */
public final class TemplateBasedVTableTrampoline extends Snippet {

    private CriticalMethod classMethodActor = new CriticalMethod(executable, CallEntryPoint.OPTIMIZED_ENTRY_POINT);

    private TemplateBasedVTableTrampoline() {
    }

    private static final TemplateBasedVTableTrampoline snippet = new TemplateBasedVTableTrampoline();
    /**
     * Template-based generator for trampoline code.
     */
    private static final TrampolineGenerator trampolineGenerator = new TemplateBasedTrampolineGenerator.VtableTrampolineGenerator(snippet.executable);

    /**
     * Place holder that will become a reference literal in a compilation of {@link #templateBasedVTableTrampoline(Object)}
     * that can be subsequently patched.
     */
    private static final DynamicTrampoline DYNAMIC_TRAMPOLINE = new DynamicTrampoline(0, null);

    @SNIPPET
    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.VIRTUAL)
    private static Address templateBasedVTableTrampoline(Object receiver) throws Throwable {
        return DYNAMIC_TRAMPOLINE.trampolineReturnAddress(receiver, VMRegister.getCpuStackPointer());
    }

    public static synchronized Address makeCallEntryPoint(int vTableIndex) {
        return trampolineGenerator.makeCallEntryPoint(vTableIndex);
    }

    public static boolean isVTableTrampoline(MethodActor classMethodActor) {
        return classMethodActor == snippet.executable;
    }

    public static boolean isVTableTrampolineInstructionPointer(Address instructionPointer) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod == null) {
            return false;
        }
        return targetMethod.classMethodActor() == snippet.executable;
    }
}

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
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.trampoline.*;


/**
 * Implements Just-in-time compilation of interface implementation on first use.
 * All entries of interface tables (<em>itables</em>) are initialized with a itable trampoline.
 * Upon first invocation of an interface, execution is routed to the trampoline which selects
 * the appropriate method implementing the interface and compiles it if not already done,
 * set the itable entry appropriately and re-execute
 * the interface call which this time dispatch to compiled code.
 *
 * Interfaces with the same itable index share the same trampoline. The trampolines
 * for virtual methods with different vtable index differs only by a single immediate operand
 * holding the vtable index. Thus, to build a new trampoline, one can just take a copy of a template and
 * edit the instruction to change the vtable index immediate operand accordingly.
 * @see TemplateBasedTrampolineGenerator
 * @author Laurent Daynes
 */
public final class TemplateBasedITableTrampoline extends NonFoldableSnippet {
    private TemplateBasedITableTrampoline() {
        super();
    }

    private static final TemplateBasedITableTrampoline SNIPPET = new TemplateBasedITableTrampoline();

    /**
     * Template-based generator for trampoline code.
     */
    private static final TrampolineGenerator _trampolineGenerator = new TemplateBasedTrampolineGenerator.ItableTrampolineGenerator(SNIPPET.classMethodActor());

    /*
     * Template for a dynamic trampoline.
     */
    private static final DynamicTrampoline DYNAMIC_TRAMPOLINE = new ITableTrampoline(0, null);

    private static void restoreReceiver(Object receiver) {
    }

    @SNIPPET
    @TRAMPOLINE(invocation = TRAMPOLINE.Invocation.INTERFACE)
    private static Address templateBasedITableTrampoline(Object receiver) throws Throwable {
        return DYNAMIC_TRAMPOLINE.trampolineReturnAddress(receiver, VMRegister.getCpuStackPointer());
    }

    public static synchronized Address makeCallEntryPoint(int iIndex) {
        return _trampolineGenerator.makeCallEntryPoint(iIndex);
    }

    public static synchronized boolean isITableTrampoline(MethodActor classMethodActor) {
        return classMethodActor == SNIPPET.classMethodActor();
    }

    public static synchronized boolean isITableTrampolineInstructionPointer(Address instructionPointer) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod == null) {
            return false;
        }
        return targetMethod.classMethodActor() == SNIPPET.classMethodActor();
    }
}


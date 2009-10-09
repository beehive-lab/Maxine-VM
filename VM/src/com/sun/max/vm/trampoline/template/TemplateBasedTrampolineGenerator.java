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
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.trampoline.*;

/**
 * Generic trampoline generator for method dispatch.
 * Trampolines for both interface and method are just a small sequence of code
 * that calls a compiler's method with a receiver and a key that identifies the
 * method to dispatch to. Trampolines differ only by the value of that key (typically, an index in a table) and the method
 * of the compiler that is called to handle method resolution (and possibly, compilation).
 * Thus trampolines can be generated using a template-based approach, wherein an immediate operand
 * holding the value of the key can be replaced. This template generator takes advantage of this and works
 * by duplicating a generic trampoline method, and editing the one instruction operand holding the key with the
 * new wanted value.
 *
 * @author Laurent Daynes
 */
public abstract class TemplateBasedTrampolineGenerator extends TrampolineGenerator {

    protected final ClassMethodActor trampolineClassMethodActor;

    TemplateBasedTrampolineGenerator(ClassMethodActor trampolineClassMethodActor) {
        this.trampolineClassMethodActor = trampolineClassMethodActor;
    }

    @CONSTANT_WHEN_NOT_ZERO
    private CPSTargetMethod template;

    private synchronized void generateTemplate() {
        if (MaxineVM.isHosted()) {
            // The template is created at prototyping time only.
            if (template == null) {
                template = (CPSTargetMethod) CompilationScheme.Static.forceFreshCompile(trampolineClassMethodActor);
                assert template.referenceLiterals().length == 1;
            }
        }
    }

    protected abstract DynamicTrampoline allocateTrampoline(int dispatchTableIndex, TargetMethod trampoline);

    /**
     * This method creates a new trampoline for the specified table index (either virtual
     * table index or interface table index).
     * @param tableIndex the table index for which to create a trampoline
     * @return a new dynamic trampoline object for the specified table index
     */
    @Override
    public DynamicTrampoline createTrampoline(int tableIndex) {
        if (MaxineVM.isHosted() && template == null) {
            generateTemplate();
        }
        // Clone the template.
        final TargetMethod trampoline = template.duplicate();
        if (!MaxineVM.isHosted()) {
            trampoline.linkDirectCalls();
        }
        final DynamicTrampoline dynamicTrampoline = allocateTrampoline(tableIndex, trampoline);
        // Fix the clone's literal constant holding the index to the dispatch table
        final Object[] literals = trampoline.referenceLiterals();
        assert literals.length == 1 && literals[0] instanceof DynamicTrampoline;
        literals[0] = dynamicTrampoline;
        return dynamicTrampoline;
    }

    public static class VtableTrampolineGenerator extends TemplateBasedTrampolineGenerator {
        public VtableTrampolineGenerator(ClassMethodActor trampolineClassMethodActor) {
            super(trampolineClassMethodActor);
        }
        @Override
        protected DynamicTrampoline allocateTrampoline(int dispatchTableIndex, TargetMethod trampoline) {
            return new VTableTrampoline(dispatchTableIndex, trampoline);
        }
    }

    public static class ItableTrampolineGenerator extends TemplateBasedTrampolineGenerator  {
        public ItableTrampolineGenerator(ClassMethodActor trampolineClassMethodActor) {
            super(trampolineClassMethodActor);
        }
        @Override
        protected DynamicTrampoline allocateTrampoline(int dispatchTableIndex, TargetMethod trampoline) {
            return new ITableTrampoline(dispatchTableIndex, trampoline);
        }
    }
}

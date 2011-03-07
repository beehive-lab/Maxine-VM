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
package com.sun.max.vm.cps.jit;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.cps.template.*;
import com.sun.max.vm.cps.template.source.*;

/**
 * Target code generator based on template. The code generator uses a simple bytecode to target translator that produces code by merely
 * assembling templates of native code.
 *
 * @author Laurent Daynes
 */
public abstract class TemplateBasedTargetGenerator extends TargetGenerator {

    @CONSTANT_WHEN_NOT_ZERO
    private TemplateTable templateTable;

    public TemplateTable templateTable() {
        return templateTable;
    }

    @HOSTED_ONLY
    public void initializeTemplateTable(TemplateTable templateTable) {
        if (this.templateTable == null) {
            this.templateTable = templateTable;
        }
    }

    @HOSTED_ONLY
    public void initializeTemplateTable(Class... templateSources) {
        initializeTemplateTable(new TemplateTable(templateSources));
    }

    @HOSTED_ONLY
    public void initialize() {
        try {
            initializeTemplateTable(BytecodeTemplateSource.class);
        } catch (Throwable throwable) {
            ProgramError.unexpected("FAILED TO INITIALIZE TEMPLATE TABLE", throwable);
        }
    }

    protected TemplateBasedTargetGenerator(RuntimeCompiler dynamicCompilerScheme, ISA isa) {
        super(dynamicCompilerScheme, isa);
    }

    protected abstract BytecodeToTargetTranslator makeTargetTranslator(ClassMethodActor classMethodActor);

    @Override
    protected void generateIrMethod(CPSTargetMethod targetMethod) {
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();

        final BytecodeToTargetTranslator codeGenerator = makeTargetTranslator(classMethodActor);

        // emit prologue
        Adapter adapter = codeGenerator.emitPrologue();

        // emit instrumentation for the method entry
        codeGenerator.emitEntrypointInstrumentation();

        codeGenerator.emitMethodTrace();

        // Translate bytecode into native code
        codeGenerator.generate();

        codeGenerator.emitEpilogue();

        // Produce target method
        final Object[] referenceLiterals = codeGenerator.packReferenceLiterals();
        codeGenerator.buildExceptionHandlingInfo();

        final Stops stops = codeGenerator.packStops();

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(
                        0,
                        (referenceLiterals == null) ? 0 : referenceLiterals.length,
                        codeGenerator.codeBuffer.currentPosition());
        Code.allocate(targetBundleLayout, targetMethod);

        codeGenerator.setGenerated(
                        targetMethod,
                        stops,
                        null, // java frame descriptors
                        null, // no scalar literals ever
                        referenceLiterals,
                        codeGenerator.codeBuffer.buffer(),
                        codeGenerator.targetABI());

        if (!MaxineVM.isHosted()) {
            // at target runtime, each method gets linked individually right after generating it:
            targetMethod.linkDirectCalls(adapter);
        }
    }
}


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
package com.sun.max.vm.cps.jit;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.source.*;

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

    protected TemplateBasedTargetGenerator(RuntimeCompilerScheme dynamicCompilerScheme, ISA isa) {
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
                        codeGenerator.codeBuffer,
                        codeGenerator.targetABI());

        if (!MaxineVM.isHosted()) {
            // at target runtime, each method gets linked individually right after generating it:
            targetMethod.linkDirectCalls(adapter);
        }

        CompilationScheme.Inspect.notifyCompilationComplete(targetMethod);
    }
}


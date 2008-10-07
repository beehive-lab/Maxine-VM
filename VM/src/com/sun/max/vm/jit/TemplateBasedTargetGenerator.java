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
/*VCSID=a1ab68f1-78bd-4f0a-b777-a79bb2a43aa3*/
package com.sun.max.vm.jit;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.instrument.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.hotpath.*;
import com.sun.max.vm.tele.*;
import com.sun.max.vm.template.*;

/**
 * Target code generator based on template. The code generator uses a simple bytecode to target translator that produces code by merely
 * assembling templates of native code.
 *
 * @author Laurent Daynes
 */
public abstract class TemplateBasedTargetGenerator extends TargetGenerator {

    @CONSTANT_WHEN_NOT_ZERO
    private TemplateTable _templateTable;

    protected TemplateTable templateTable() {
        return _templateTable;
    }

    public void initializeTemplateTable(TemplateTable templateTable) {
        if (_templateTable == null) {
            _templateTable = templateTable;
        }
    }

    public void initializeTemplateTable(Class[] templateSources) {
        initializeTemplateTable(new TemplateTable(templateSources));
    }

    public void initialize() {
        try {
            // Don't want any hardcoded symbolic references to template source classes, so we use reflection to obtain the list of templates.
            // Need to find a better way to do this.
            final Class[] templateSources =
                (Class[]) Class.forName("com.sun.max.vm.template.source.TemplateTableConfiguration").getField("OPTIMIZED_TEMPLATE_SOURCES").get(null);

            initializeTemplateTable(templateSources);
        } catch (Throwable throwable) {
            ProgramError.unexpected("FAILED TO INITIALIZE TEMPLATE TABLE", throwable);
        }
    }

    protected TemplateBasedTargetGenerator(DynamicCompilerScheme dynamicCompilerScheme, InstructionSet instructionSet) {
        super(dynamicCompilerScheme, instructionSet);
    }

    protected TemplateBasedTargetGenerator(TargetGeneratorScheme targetGeneratorScheme, InstructionSet instructionSet, TemplateTable templateTable) {
        super(targetGeneratorScheme, instructionSet);
        _templateTable = templateTable;
    }

    protected abstract BytecodeToTargetTranslator makeTargetTranslator(ClassMethodActor classMethodActor, CompilationDirective compilationDirective);

    @Override
    protected void generateIrMethod(TargetMethod targetMethod, CompilationDirective compilationDirective) {
        final ClassMethodActor classMethodActor = targetMethod.classMethodActor();

        if (Trace.hasLevel(3)) {
            Trace.begin(3, "JIT: " + classMethodActor);
        }

        if (HotpathConfiguration.isEnabled()) {
            if (compilationDirective.traceInstrument()) {
                Debug.print("JIT TRACED: ");
            } else {
                Debug.print("JIT: ");
            }

            Debug.print(classMethodActor.toString());
            Debug.println();
        }

        final MethodInstrumentation methodInstrumentation = VMConfiguration.target().compilationScheme().makeMethodInstrumentation(classMethodActor);
        final BytecodeToTargetTranslator codeGenerator = makeTargetTranslator(classMethodActor, compilationDirective);
        final BytecodeScanner bytecodeScanner = new BytecodeScanner(codeGenerator);

        // emit prologue
        final int optimizedCallerAdapterFrameCodeSize = codeGenerator.emitPrologue();

        // emit instrumentation
        if (methodInstrumentation != null && methodInstrumentation.recompilationAlarm() != null) {
            codeGenerator.emitAlarmCounter(methodInstrumentation.recompilationAlarm().counter());
        }

        // Translate bytecode into native code
        try {
            bytecodeScanner.scan(classMethodActor);
        } catch (RuntimeException runtimeException) {
            throw (InternalError) new InternalError("Error while translating " + bytecodeScanner.getCurrentLocationAsString(classMethodActor)).initCause(runtimeException);
        } catch (Error error) {
            throw (InternalError) new InternalError("Error while translating " + bytecodeScanner.getCurrentLocationAsString(classMethodActor)).initCause(error);
        }

        codeGenerator.emitEpilogue();

        // Produce target method
        final Object[] referenceLiterals = codeGenerator.packReferenceLiterals();
        codeGenerator.buildExceptionHandlingInfo();

        final Stops stops = codeGenerator.packStops();

        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(
                        codeGenerator.numberOfCatchRanges(),
                        stops.numberOfDirectCalls(),
                        stops._numberOfIndirectCalls,
                        stops._numberOfSafepoints,
                        0, // no scalar literals ever
                        (referenceLiterals == null) ? 0 : referenceLiterals.length,
                                        codeGenerator.codeBuffer().currentPosition(),
                                        codeGenerator.frameReferenceMapSize(),
                                        targetMethod.registerReferenceMapSize());
        targetMethod.setSize(targetBundleLayout.bundleSize());
        Code.allocate(targetMethod);
        final TargetBundle targetBundle = new TargetBundle(targetBundleLayout, targetMethod.start());

        final int[] catchRangeOffsets = codeGenerator.catchRangePositions();
        final int[] catchBlockOffsets =  codeGenerator.catchBlockPositions();

        codeGenerator.setGenerated(
                        targetMethod,
                        targetBundle,
                        catchRangeOffsets,
                        catchBlockOffsets,
                        stops,
                        null, // java frame descriptors, TODO
                        null, // no scalar literals ever
                        referenceLiterals,
                        codeGenerator.codeBuffer(),
                        optimizedCallerAdapterFrameCodeSize,
                        codeGenerator.adapterReturnPosition(),
                        null, // inlineDataPositions, TODO
                        codeGenerator.targetABI());

        if (MaxineVM.isPrototyping()) {
            // the compiled prototype links all methods in a separate phase
        } else {
            // at target runtime, each method gets linked individually right after generating it:
            targetMethod.linkDirectCalls();
        }

        if (Trace.hasLevel(3)) {
            Trace.end(3, "JIT: " + classMethodActor);
        }

        BytecodeBreakpointMessage.makeTargetBreakpoints(targetMethod);
    }
}


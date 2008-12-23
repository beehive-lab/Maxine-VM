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
package com.sun.max.vm.compiler.b.c;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.CirTraceObserver.*;
import com.sun.max.vm.compiler.cir.builtin.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.snippet.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class BcCompiler extends BCompiler implements CirGeneratorScheme {

    private final BirToCirTranslator _birToCirTranslator;

    public BcCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        _birToCirTranslator = new BirToCirTranslator(this);
    }

    public CirGenerator cirGenerator() {
        return _birToCirTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return cirGenerator();
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), _birToCirTranslator);
    }

    @Override
    public void createBuiltins(PackageLoader packageLoader) {
        super.createBuiltins(packageLoader);
        packageLoader.loadAndInitializeAllAndInstantiateLeaves(CirBuiltin.class);
    }

    @Override
    public void createSnippets(PackageLoader packageLoader) {
        super.createSnippets(packageLoader);
        packageLoader.loadAndInitializeAllAndInstantiateLeaves(CirSnippet.class);
    }

    @PROTOTYPE_ONLY
    private void translateSnippets(CirVariableFactory cirVariableFactory) {
        Trace.begin(1, "translateSnippets");
        for (int i = 0; i < Snippet.snippets().length(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            try {
                cirGenerator().notifyBeforeGeneration(cirSnippet);
                final ClassMethodActor classMethodActor = cirSnippet.classMethodActor();
                final CirClosure cirClosure = _birToCirTranslator.translateMethod(birGenerator().makeIrMethod(classMethodActor), cirSnippet, cirVariableFactory);
                cirSnippet.setGenerated(cirClosure);
                cirGenerator().setCirMethod(classMethodActor, cirSnippet);
                cirGenerator().notifyAfterGeneration(cirSnippet);
            } catch (Throwable throwable) {
                ProgramError.unexpected("error during snippet translation: " + cirSnippet.name(), throwable);
            }
        }
        Trace.end(1, "translateSnippets");
    }

    @PROTOTYPE_ONLY
    private void optimizeSnippet(Snippet snippet) {
        final CirSnippet cirSnippet = CirSnippet.get(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
        final CirClosure closure = cirSnippet.copyClosure();
        CirOptimizer.apply(cirGenerator(), cirSnippet, closure, CirInliningPolicy.STATIC);
        cirSnippet.setGenerated(closure);
    }

    @PROTOTYPE_ONLY
    private void optimizeSnippets(CirVariableFactory cirVariableFactory) {
        Trace.begin(1, "optimizeSnippets");
        // Need to optimize these first, in case any other snippets make calls that are not inlined.
        optimizeSnippet(MethodSelectionSnippet.SelectVirtualMethod.SNIPPET);
        optimizeSnippet(MethodSelectionSnippet.SelectInterfaceMethod.SNIPPET);
        optimizeSnippet(MethodSelectionSnippet.ReadHub.SNIPPET);

        // Each snippet optimization must proceed without encountering prior folding,
        // so store all results on the side without reusing them yet
        // and then assign them in a separate pass below:
        final CirClosure[] optimizedClosures = new CirClosure[Snippet.snippets().length()];
        for (int i = 0; i < Snippet.snippets().length(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            final CirClosure cirClosure = cirSnippet.copyClosure();
            optimizedClosures[i] = cirClosure;
            cirGenerator().notifyBeforeTransformation(cirSnippet, cirClosure, Transformation.SNIPPET_OPTIMIZATION);
            CirOptimizer.apply(cirGenerator(), cirSnippet, cirClosure, CirInliningPolicy.STATIC);
            cirGenerator().notifyAfterTransformation(cirSnippet, cirClosure, Transformation.SNIPPET_OPTIMIZATION);
            if (cirSnippet.snippet() instanceof BuiltinsSnippet) {
                CirBuiltinCheck.apply(cirClosure, cirSnippet.snippet());
            }
        }

        cleanupAfterSnippets();

        // Updated each snippet's closure with the respective optimized version:
        for (int i = 0; i < Snippet.snippets().length(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            cirGenerator().notifyBeforeGeneration(cirSnippet);
            cirSnippet.setGenerated(optimizedClosures[i]);
            cirGenerator().notifyAfterGeneration(cirSnippet);
        }
        Trace.end(1, "optimizeSnippets");
    }

    /**
     * After optimizing all Snippets, there are many CirMethods holding unoptimized CIR closures.
     * We need to clear those so that these methods can be compiled properly when needed.
     */
    @PROTOTYPE_ONLY
    private void cleanupAfterSnippets() {
        for (ClassActor classActor : ClassRegistry.vmClassRegistry()) {
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                CompilationScheme.Static.resetMethodState(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                CompilationScheme.Static.resetMethodState(classMethodActor);
            }
        }
    }

    private static boolean _optimizing = true;

    public boolean optimizing() {
        return _optimizing;
    }

    /**
     * This allows us to test the CIR generator only without ever running the CIR optimizer.
     */
    public static void disableOptimizing() {
        _optimizing = false;
    }

    @Override
    public void compileSnippets() {
        final CirVariableFactory cirVariableFactory = new CirVariableFactory();
        translateSnippets(cirVariableFactory);
        if (_optimizing) {
            optimizeSnippets(cirVariableFactory);
        }
        super.compileSnippets();
    }

    @Override
    public void gatherCalls(final TargetMethod targetMethod,
                    final AppendableSequence<MethodActor> directCalls,
                    final AppendableSequence<MethodActor> virtualCalls,
                    final AppendableSequence<MethodActor> interfaceCalls) {
        traceBeforeFindMethodActors(targetMethod);
        final MethodActor[] directCallees = targetMethod.directCallees();
        if (directCallees != null) {
            for (MethodActor methodActor : directCallees) {
                directCalls.append(methodActor);
            }
        }
        final CirMethod cirMethod = cirGenerator().getCirMethod(targetMethod.classMethodActor());
        if (cirMethod == null || !cirMethod.isGenerated()) {
            // do nothing.
            return;
        }
        // collect all virtual and interface calls by visiting the optimized CIR code,
        // relating CIR call nodes back to their bytecode source.
        final CirVisitor collector = new CirVisitor() {
            @Override
            public void visitCall(CirCall call) {
                final BytecodeLocation location = call.bytecodeLocation();
                assert location != null || call.javaFrameDescriptor() == null : "Each CirCall with JavaFrameDescriptor must have bytecodelocation";
                if (location != null) {
                    final ConstantPool pool = location.classMethodActor().codeAttribute().constantPool();
                    final InvokedMethodRecorder invokedMethodRecorder = new InvokedMethodRecorder(pool, directCalls, virtualCalls, interfaceCalls);
                    final BytecodeScanner bytecodeScanner = new BytecodeScanner(invokedMethodRecorder);
                    try {
                        final byte[] bytecode = location.classMethodActor().codeAttribute().code();
                        if (bytecode != null && location.position() < bytecode.length) {
                            bytecodeScanner.scanInstruction(bytecode, location.position());
                        }
                    } catch (Throwable throwable) {
                        ProgramError.unexpected("could not scan byte code in " + targetMethod.classMethodActor().holder().name() + "." + targetMethod.classMethodActor().name(), throwable);
                    }
                }
            }
        };
        CirVisitingTraversal.apply(cirMethod.closure(), collector);
        traceAfterFindMethodActors(targetMethod, directCalls);
    }

    private void traceBeforeFindMethodActors(TargetMethod targetMethod) {
        if (Trace.hasLevel(5)) {
            Trace.begin(5, "methodActorsReferencedByCalls: " + targetMethod.classMethodActor().format("%R %n(%P)"));
        }
    }

    private void traceAfterFindMethodActors(TargetMethod targetMethod, AppendableSequence<MethodActor> result) {
        if (Trace.hasLevel(5)) {
            Trace.end(5, result.length() + " methodActorsReferencedByCalls: " + targetMethod.classMethodActor().format("%R %n(%P)"));
        }
    }
}

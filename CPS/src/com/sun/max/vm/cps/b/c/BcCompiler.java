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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.b.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.snippet.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class BcCompiler extends BCompiler implements CirGeneratorScheme {

    private final BirToCirTranslator birToCirTranslator;

    public BcCompiler(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
        birToCirTranslator = new BirToCirTranslator(this);
    }

    public CirGenerator cirGenerator() {
        return birToCirTranslator;
    }

    @Override
    public IrGenerator irGenerator() {
        return cirGenerator();
    }

    @Override
    public Sequence<IrGenerator> irGenerators() {
        return Sequence.Static.appended(super.irGenerators(), birToCirTranslator);
    }

    @HOSTED_ONLY
    @Override
    public void createBuiltins(PackageLoader packageLoader) {
        super.createBuiltins(packageLoader);
    }

    @HOSTED_ONLY
    @Override
    public void createSnippets(PackageLoader packageLoader) {
        super.createSnippets(packageLoader);
    }

    @HOSTED_ONLY
    private void translateSnippets() {
        Trace.begin(1, "translateSnippets");
        for (int i = 0; i < Snippet.snippets().length(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            try {
                cirGenerator().notifyBeforeGeneration(cirSnippet);
                final ClassMethodActor classMethodActor = cirSnippet.classMethodActor();
                final CirVariableFactory cirVariableFactory = new CirVariableFactory();
                final CirClosure cirClosure = birToCirTranslator.translateMethod(birGenerator().makeIrMethod(classMethodActor), cirSnippet, cirVariableFactory);
                cirSnippet.setGenerated(cirClosure);
                cirGenerator().setCirMethod(classMethodActor, cirSnippet);
                cirGenerator().notifyAfterGeneration(cirSnippet);
            } catch (Throwable throwable) {
                ProgramError.unexpected("error during snippet translation: " + cirSnippet.name(), throwable);
            }
        }
        Trace.end(1, "translateSnippets");
    }

    @HOSTED_ONLY
    private void optimizeSnippets() {
        Trace.begin(1, "optimizeSnippets");
        // Each snippet optimization must proceed without encountering prior folding,
        // so store all results on the side without reusing them yet
        // and then assign them in a separate pass below:
        final CirClosure[] optimizedClosures = new CirClosure[Snippet.snippets().length()];
        for (int i = 0; i < Snippet.snippets().length(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            final CirClosure cirClosure = cirSnippet.copyClosure();
            optimizedClosures[i] = cirClosure;
            cirGenerator().notifyBeforeTransformation(cirSnippet, cirClosure, TransformationType.SNIPPET_OPTIMIZATION);
            CirOptimizer.apply(cirGenerator(), cirSnippet, cirClosure, CirInliningPolicy.STATIC);
            cirGenerator().notifyAfterTransformation(cirSnippet, cirClosure, TransformationType.SNIPPET_OPTIMIZATION);
            if (cirSnippet.snippet instanceof BuiltinsSnippet) {
                CirBuiltinCheck.apply(cirClosure, cirSnippet.snippet);
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
    @HOSTED_ONLY
    private void cleanupAfterSnippets() {
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY) {
            for (ClassMethodActor classMethodActor : classActor.localVirtualMethodActors()) {
                CompilationScheme.Static.resetMethodState(classMethodActor);
            }
            for (ClassMethodActor classMethodActor : classActor.localStaticMethodActors()) {
                CompilationScheme.Static.resetMethodState(classMethodActor);
            }
        }
    }

    private static boolean optimizing = true;

    public boolean optimizing() {
        return optimizing;
    }

    /**
     * This allows us to test the CIR generator only without ever running the CIR optimizer.
     */
    public static void disableOptimizing() {
        optimizing = false;
    }

    @HOSTED_ONLY
    @Override
    public void compileSnippets() {
        translateSnippets();
        if (optimizing) {
            optimizeSnippets();
        }
        super.compileSnippets();
    }

    private void traceBeforeFindMethodActors(TargetMethod targetMethod) {
        if (Trace.hasLevel(5)) {
            Trace.begin(5, "methodActorsReferencedByCalls: " + targetMethod.classMethodActor().format("%R %n(%P)"));
        }
    }

    private void traceAfterFindMethodActors(TargetMethod targetMethod, Set<MethodActor> result) {
        if (Trace.hasLevel(5)) {
            Trace.end(5, result.size() + " methodActorsReferencedByCalls: " + targetMethod.classMethodActor().format("%R %n(%P)"));
        }
    }
}

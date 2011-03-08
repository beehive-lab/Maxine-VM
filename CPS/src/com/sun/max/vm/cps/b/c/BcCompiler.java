/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.b.c;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.b.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.CirTraceObserver.TransformationType;
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

    public BcCompiler() {
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
    public List<IrGenerator> irGenerators() {
        final List<IrGenerator> result = new LinkedList<IrGenerator>(super.irGenerators());
        result.add(birToCirTranslator);
        return result;
    }

    @HOSTED_ONLY
    private void translateSnippets() {
        Trace.begin(1, "translateSnippets");
        for (int i = 0; i < Snippet.snippets().size(); i++) {
            final CirSnippet cirSnippet = CirSnippet.get(Snippet.snippets().get(i));
            try {
                cirGenerator().notifyBeforeGeneration(cirSnippet);
                final ClassMethodActor classMethodActor = cirSnippet.classMethodActor();
                final CirVariableFactory cirVariableFactory = new CirVariableFactory();
                final CirClosure cirClosure = birToCirTranslator.translateMethod(birGenerator().makeIrMethod(classMethodActor, true), cirSnippet, cirVariableFactory);
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
        final CirClosure[] optimizedClosures = new CirClosure[Snippet.snippets().size()];
        for (int i = 0; i < Snippet.snippets().size(); i++) {
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
        for (int i = 0; i < Snippet.snippets().size(); i++) {
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
        for (ClassActor classActor : ClassRegistry.BOOT_CLASS_REGISTRY.copyOfClasses()) {
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

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
package com.sun.max.vm.cps.b.c;

import static com.sun.max.vm.cps.cir.CirTraceObserver.TransformationType.*;

import com.sun.max.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.cps.b.c.HCirToLCirTranslation.Verifier;
import com.sun.max.vm.cps.bir.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.optimize.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;

public class BirToCirTranslator extends CirGenerator {

    public BirToCirTranslator(CirGeneratorScheme cirGeneratorScheme) {
        super(cirGeneratorScheme);
    }

    CirClosure translateMethod(BirMethod birMethod, CirMethod cirMethod, CirVariableFactory variableFactory) {
        final BirToCirMethodTranslation methodTranslation = new BirToCirMethodTranslation(birMethod, variableFactory, this);

        BlockTranslator.run(methodTranslation);
        CirClosure cirClosure = methodTranslation.cirClosure();
        notifyBeforeTransformation(cirMethod, cirClosure, INITIAL_CIR_CREATION);
        notifyAfterTransformation(cirMethod, cirClosure, INITIAL_CIR_CREATION);

        notifyBeforeTransformation(cirMethod, cirClosure, HCIR_FREE_VARIABLE_CAPTURING);
        FreeVariableCapturing freeVariableCapturing = new FreeVariableCapturing(methodTranslation);
        freeVariableCapturing.run();
        notifyAfterTransformation(cirMethod, cirClosure, HCIR_FREE_VARIABLE_CAPTURING);

        notifyBeforeTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);
        CirAlphaConversion.apply(cirClosure);
        notifyAfterTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);

        notifyBeforeTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);
        freeVariableCapturing.pruneJavaLocals();
        notifyAfterTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);

        /* perform copy propagation first before running any env-based analysis
         * passes.  The code that BIR->HCIR produces is horrible for analysis
         * since no two values ever operate on the same variable.  We could've
         * fixed the analysis to cope with copy-propagation itself, but we
         * decided that the better option is to perform copy-propagation explicitly
         * first before running any analysis pass.  It was simpler and got most
         * of the interesting cases.
         */
        notifyBeforeTransformation(cirMethod, cirClosure, COPY_PROPAGATION);
        CirCopyPropagation.apply(cirClosure);
        notifyAfterTransformation(cirMethod, cirClosure, COPY_PROPAGATION);

        /* optionally verify the structure of the code */
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), new Verifier());

        final EnvBasedInitializedAnalysis analysis = new EnvBasedInitializedAnalysis(InitializedDomain.DOMAIN);

        Environment<CirVariable, InitializedDomain.Set> env = null;
        if (!cirMethod.classMethodActor().isStatic()) {
            env = new Environment<CirVariable, InitializedDomain.Set>();
            final CirVariable receiver = cirClosure.parameters()[0];
            env = env.extend(receiver, InitializedDomain.DOMAIN.getInitialized());
        }
        final IdentityHashMapping<CirCall, InitializedDomain.Set[]> result = analysis.analyse(cirClosure, env);
        EnvBasedInitializedAnalysis.InitializedResult.applyResult(cirClosure, result);

        notifyBeforeTransformation(cirMethod, cirClosure, HCIR_TO_LCIR);
        HCirToLCirTranslation.apply(methodTranslation);
        notifyAfterTransformation(cirMethod, cirClosure, HCIR_TO_LCIR);

        notifyBeforeTransformation(cirMethod, cirClosure, LCIR_FREE_VARIABLE_CAPTURING);
        freeVariableCapturing = new FreeVariableCapturing(methodTranslation);
        freeVariableCapturing.run();
        notifyAfterTransformation(cirMethod, cirClosure, LCIR_FREE_VARIABLE_CAPTURING);

        notifyBeforeTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);
        CirAlphaConversion.apply(cirClosure);
        notifyAfterTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);

        notifyBeforeTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);
        freeVariableCapturing.pruneJavaLocals();
        notifyAfterTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);

        return cirClosure;
    }

    /**
     * OPTIMIZATION: This shortcut prevents a lot of reflective invocations and thus improves CIR optimization times significantly.
     */
    private void foldAndMemoize(CirMethod cirMethod) {
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirClosure cirClosure = new CirClosure();
        cirClosure.setParameters(variableFactory.normalContinuationParameter(), variableFactory.exceptionContinuationParameter());
        try {
            cirClosure.setBody(CirFoldable.Static.fold(cirMethod, cirClosure.parameters()));
        } catch (CirFoldingException cirFoldingException) {
            throw ProgramError.unexpected("Error while folding " + cirMethod.classMethodActor(), cirFoldingException);
        }
        cirMethod.setGenerated(cirClosure);
    }

    @Override
    protected void generateIrMethod(CirMethod cirMethod, boolean install) {
        final ClassMethodActor compilee = cirMethod.classMethodActor().compilee();
        if (!compilee.isHiddenToReflection()) {
            if (compilee.isStatic() && compilee.isDeclaredFoldable() && compilee.descriptor().numberOfParameters() == 0) {
                foldAndMemoize(cirMethod);
                return;
            }
        }

        final BirGeneratorScheme birGeneratorScheme = (BirGeneratorScheme) compilerScheme();
        final BirMethod birMethod = birGeneratorScheme.birGenerator().makeIrMethod(cirMethod.classMethodActor(), install);
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirClosure cirClosure = translateMethod(birMethod, cirMethod, variableFactory);

        if (compilerScheme().optimizing()) {
            CirInliningPolicy cirInliningPolicy = CirInliningPolicy.DYNAMIC;
            Class<Class<? extends Accessor>> type = null;
            Class<? extends Accessor> accessor = Utils.cast(type, cirMethod.classMethodActor().accessor());
            if (accessor != null) {
                cirInliningPolicy = new CirInliningPolicy(accessor);
            }
            notifyBeforeTransformation(cirMethod, cirClosure, OPTIMIZATION);
            CirOptimizer.apply(this, cirMethod, cirClosure, cirInliningPolicy);
            notifyAfterTransformation(cirMethod, cirClosure, OPTIMIZATION);
        }
        cirMethod.setGenerated(cirClosure);
        return;
    }
}

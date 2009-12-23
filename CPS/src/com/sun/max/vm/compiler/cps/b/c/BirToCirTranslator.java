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
package com.sun.max.vm.compiler.cps.b.c;

import static com.sun.max.vm.compiler.cps.cir.CirTraceObserver.TransformationType.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.cps.b.c.HCirToLCirTranslation.*;
import com.sun.max.vm.compiler.cps.bir.*;
import com.sun.max.vm.compiler.cps.cir.*;
import com.sun.max.vm.compiler.cps.cir.optimize.*;
import com.sun.max.vm.compiler.cps.cir.transform.*;
import com.sun.max.vm.compiler.cps.cir.variable.*;

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
    protected void generateIrMethod(CirMethod cirMethod) {
        final ClassMethodActor compilee = cirMethod.classMethodActor().compilee();
        if (!compilee.isHiddenToReflection()) {
            if (compilee.isStatic() && MaxineVM.isMaxineClass(compilee.holder().typeDescriptor) && compilee.isDeclaredFoldable() && compilee.descriptor().numberOfParameters() == 0) {
                foldAndMemoize(cirMethod);
                return;
            }
        }

        final BirGeneratorScheme birGeneratorScheme = (BirGeneratorScheme) compilerScheme();
        final BirMethod birMethod = birGeneratorScheme.birGenerator().makeIrMethod(cirMethod.classMethodActor());
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirClosure cirClosure = translateMethod(birMethod, cirMethod, variableFactory);

        if (compilerScheme().optimizing()) {
            CirInliningPolicy cirInliningPolicy = CirInliningPolicy.DYNAMIC;
            if (MaxineVM.isHosted() && !cirMethod.classMethodActor().isHiddenToReflection()) {
                final ACCESSOR accessorAnnotation = cirMethod.classMethodActor().getAnnotation(ACCESSOR.class);
                if (accessorAnnotation != null) {
                    cirInliningPolicy = new CirInliningPolicy(accessorAnnotation.value());
                }
            }
            notifyBeforeTransformation(cirMethod, cirClosure, OPTIMIZATION);
            CirOptimizer.apply(this, cirMethod, cirClosure, cirInliningPolicy);
            notifyAfterTransformation(cirMethod, cirClosure, OPTIMIZATION);
        }
        cirMethod.setGenerated(cirClosure);
        return;
    }
}

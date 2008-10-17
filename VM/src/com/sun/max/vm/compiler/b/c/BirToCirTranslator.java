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

import static com.sun.max.vm.compiler.cir.CirTraceObserver.Transformation.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.HCirToLCirTranslation.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.optimize.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.jni.*;

public class BirToCirTranslator extends CirGenerator {

    public BirToCirTranslator(CirGeneratorScheme cirGeneratorScheme) {
        super(cirGeneratorScheme);
    }

    CirClosure translateMethod(BirMethod birMethod, CirMethod cirMethod, CirVariableFactory variableFactory) {
        final BirToCirMethodTranslation methodTranslation = new BirToCirMethodTranslation(birMethod, variableFactory, this);
        final ClassMethodActor classMethodActor = birMethod.classMethodActor();

        BlockTranslator.run(methodTranslation);
        CirClosure cirClosure = methodTranslation.cirClosure();
        notifyBeforeTransformation(cirMethod, cirClosure, INITIAL_CIR_CREATION);
        notifyAfterTransformation(cirMethod, cirClosure, INITIAL_CIR_CREATION);

        notifyBeforeTransformation(cirMethod, cirClosure, HCIR_FREE_VARIABLE_CAPTURING);
        FreeVariableCapturing freeVariableCapturing = new FreeVariableCapturing(methodTranslation);
        freeVariableCapturing.run();
        notifyAfterTransformation(cirMethod, cirClosure, HCIR_FREE_VARIABLE_CAPTURING);

        notifyBeforeTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);
        CirAlphaConversion.apply(methodTranslation.variableFactory(), cirClosure);
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
        CirCopyPropagation.apply(cirClosure);

        /* optionally verify the structure of the code */
        CirVisitingTraversal.apply(methodTranslation.cirClosure(), new Verifier());

        if (true) {
            final EnvBasedInitializedAnalysis analysis = new EnvBasedInitializedAnalysis(InitializedDomain.DOMAIN);
            final IdentityHashMapping<CirCall, InitializedDomain.Set[]> result = analysis.analyse(cirClosure);
            EnvBasedInitializedAnalysis.InitializedResult.applyResult(cirClosure, result);
        }

        if (false) { /* FIXME: investigate which of these are broken and why. */
            notifyBeforeTransformation(cirMethod, cirClosure, HCIR_SPLITTING);

            SplitTransformation.apply(methodTranslation);
            notifyAfterTransformation(cirMethod, cirClosure, HCIR_SPLITTING);

            final NameAnalysis nameAnalysis = new NameAnalysis();
            nameAnalysis.apply(cirClosure);

            final DefsetAnalysis defset = new DefsetAnalysis(DefsetDomain.singleton);
            defset.apply(cirClosure);

            final ClassTypeAnalysis typeAnalysis = new ClassTypeAnalysis(defset, nameAnalysis, TypesetDomain.singleton);

            typeAnalysis.apply(cirClosure);

            (new TypeStrengthReduction(typeAnalysis)).apply(cirClosure);
        }

        notifyBeforeTransformation(cirMethod, cirClosure, HCIR_TO_LCIR);
        HCirToLCirTranslation.apply(methodTranslation);
        notifyAfterTransformation(cirMethod, cirClosure, HCIR_TO_LCIR);

        if (MaxineVM.isPrototyping()) {
            cirClosure = applyWrapping(classMethodActor, cirClosure);
        }


        notifyBeforeTransformation(cirMethod, cirClosure, LCIR_FREE_VARIABLE_CAPTURING);
        freeVariableCapturing = new FreeVariableCapturing(methodTranslation);
        freeVariableCapturing.run();
        notifyAfterTransformation(cirMethod, cirClosure, LCIR_FREE_VARIABLE_CAPTURING);


        notifyBeforeTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);

        CirAlphaConversion.apply(methodTranslation.variableFactory(), cirClosure);
        notifyAfterTransformation(cirMethod, cirClosure, ALPHA_CONVERSION);

        notifyBeforeTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);
        freeVariableCapturing.pruneJavaLocals();
        notifyAfterTransformation(cirMethod, cirClosure, JAVA_LOCALS_PRUNING);

        return cirClosure;
    }

    /**
     * Performs wrapping of a method annotated by {@link WRAPPED} (explicitly or {@linkplain JNI_FUNCTION implicitly}).
     * Note that this transformation is only performed during {@linkplain MaxineVM#isPrototyping() prototyping}.
     *
     * @param classMethodActor
     *                a method being compiled
     * @param cirClosure
     *                the initial CIR graph for {@code classMethodActor}
     * @return the closure for {@code classMethodActor} after any relevant wrapping transformation has been applied
     */
    @PROTOTYPE_ONLY
    private CirClosure applyWrapping(final ClassMethodActor classMethodActor, final CirClosure cirClosure) {
        if (!classMethodActor.isInitializer()) {
            final Method wrappedMethod = classMethodActor.toJava();
            Class wrapperHolder = null;
            if (classMethodActor.isJniFunction()) {
                wrapperHolder = JniFunctionWrapper.class;
            } else {
                final WRAPPED wrapped = wrappedMethod.getAnnotation(WRAPPED.class);
                if (wrapped != null) {
                    wrapperHolder = wrapped.value();
                }
            }
            if (wrapperHolder != null) {
                final ClassMethodActor wrapperClassMethodActor = (ClassMethodActor) MethodActor.fromJava(WRAPPED.Static.getWrapper(wrappedMethod, wrapperHolder));
                final CirMethod wrapperCirMethod = makeIrMethod(wrapperClassMethodActor);
                notifyBeforeTransformation(wrapperCirMethod, null, WRAPPER_APPLICATION);
                final CirClosure wrappedCirClosure = CirWrapping.apply(wrapperCirMethod, cirClosure);
                notifyAfterTransformation(wrapperCirMethod, wrappedCirClosure, WRAPPER_APPLICATION);
                return wrappedCirClosure;
            }
        }
        return cirClosure;
    }

    /**
     * OPTIMIZATION: This shortcut prevents a lot of reflective invocations and thus improves CIR optimization times significantly.
     */
    private void foldAndMemoize(CirMethod cirMethod) {
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirClosure cirClosure = new CirClosure(null);
        cirClosure.setParameters(variableFactory.normalContinuationParameter(), variableFactory.exceptionContinuationParameter());
        cirClosure.setBody(CirRoutine.Static.fold(cirMethod, cirClosure.parameters()));
        cirMethod.setGenerated(cirClosure);
    }

    @Override
    protected void generateIrMethod(CirMethod cirMethod, CompilationDirective compilationDirective) {
        final ClassMethodActor compilee = cirMethod.classMethodActor().compilee();
        if (!compilee.isHiddenToReflection()) {
            if (compilee.isStatic() && com.sun.max.Package.contains(compilee.holder().toJava()) && compilee.isDeclaredFoldable() && compilee.getNumberOfParameters() == 0) {
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
            if (MaxineVM.isPrototyping() && !cirMethod.classMethodActor().isHiddenToReflection()) {
                final Method javaMethod = cirMethod.classMethodActor().toJava();
                final ACCESSOR accessorAnnotation = javaMethod.getAnnotation(ACCESSOR.class);
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

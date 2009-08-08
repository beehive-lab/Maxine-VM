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
package com.sun.max.vm.template.generate;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.template.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;

@PROTOTYPE_ONLY
public class TemplateGenerator {

    protected TargetGenerator targetGenerator;

    public TemplateGenerator() {
        targetGenerator = ((TargetGeneratorScheme) MaxineVM.target().configuration().compilerScheme()).targetGenerator();

        // TODO: this hack is to avoid an unresolved call in instrumented templates
        ClassActor.fromJava(AlarmCounter.class);

        // Make sure the JavaStackFrame class used for generating the templates are initialized in the target.
        // This will enable all sorts of compiler optimization that we want the templates to benefit from.
        initializeClassInTarget(JitStackFrameOperation.class);

        // Also, make sure the class whose field is being accessed is loaded in the target first (we want a resolved symbol at compiled time).
        // PrototypeClassLoader.PROTOTYPE_CLASS_LOADER.loadClass(ResolvedAtCompileTime.class.getName());
        initializeClassInTarget(ResolvedAtCompileTime.class);
        verifyInvariants();
    }

    protected TargetGenerator targetGenerator() {
        return targetGenerator;
    }

    public ClassMethodActor getClassMethodActor(Method method) {
        return (ClassMethodActor) MethodActor.fromJava(method);
    }


    protected Class initializeClassInTarget(final Class javaClass) {
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                assert !MaxineVM.isPrototypeOnly(javaClass);
                final Class targetClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, javaClass.getName());
                final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(targetClass);
                MakeClassInitialized.makeClassInitialized(tupleClassActor);
                return targetClass;
            }
        });
    }

    protected boolean qualifiesAsTemplate(Method m) {
        final int modifiers = m.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && !(Modifier.isNative(modifiers) || Modifier.isAbstract(modifiers));
    }

    protected void verifyInvariants() throws IllegalStateException {
        // Make sure that the resolved version is loaded in the target
        if (ClassRegistry.get(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, JavaTypeDescriptor.forJavaClass(ResolvedAtCompileTime.class), false) == null) {
            throw new IllegalStateException("Class " + ResolvedAtCompileTime.class + " must be loaded in target when generating templates");
        }
        // Make sure that the unresolved version is not loaded in the target
        if (ClassRegistry.get(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, JavaTypeDescriptor.forJavaClass(UnresolvedAtCompileTime.class), false) != null) {
            throw new IllegalStateException("Class " + UnresolvedAtCompileTime.class + " must not be loaded in target when generating templates");
        }
    }

    public VariableSequence<TargetMethod> generateTargetTemplates(Class templateSourceClass) {
        final VariableSequence<TargetMethod> templates = new ArrayListSequence<TargetMethod>();
        // Before all, make sure the template source class is initialized in the target.
        // This will enable some compiler optimization that we want the templates to benefit from.
        final Class templateHolderClassInTarget = initializeClassInTarget(templateSourceClass);
        final Method[] javaMethods = templateHolderClassInTarget.getDeclaredMethods();
        for (Method javaMethod : javaMethods) {
            if (qualifiesAsTemplate(javaMethod)) {
                final ClassMethodActor classMethodActor = (ClassMethodActor) MethodActor.fromJava(javaMethod);
                templates.append(generateTargetTemplate(classMethodActor));
            }
        }
        return templates;
    }

    public TargetMethod generateTargetTemplate(final ClassMethodActor sourceTemplate) {
        return MaxineVM.usingTarget(new Function<TargetMethod>() {
            public TargetMethod call() {
                ProgramError.check(targetGenerator().hasStackParameters(sourceTemplate), "Template must not have *any* stack parameters: " + sourceTemplate);
                final TargetMethod targetMethod = targetGenerator().makeIrMethod(sourceTemplate);
                ProgramError.check(targetMethod.referenceLiterals() == null, "Template must not have *any* reference literals: " + targetMethod);
                ProgramError.check(targetMethod.scalarLiterals() == null, "Template must not have *any* scalar literals: " + targetMethod);
                return targetMethod;
            }
        });
    }
}

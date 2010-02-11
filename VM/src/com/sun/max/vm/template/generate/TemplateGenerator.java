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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.Snippet.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetLocation.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.template.source.*;

@HOSTED_ONLY
public class TemplateGenerator {

    protected RuntimeCompilerScheme targetCompiler;

    public TemplateGenerator() {
        targetCompiler = MaxineVM.target().configuration.bootCompilerScheme();

        // Make sure the JavaStackFrame class used for generating the templates are initialized in the target.
        // This will enable all sorts of compiler optimization that we want the templates to benefit from.
        initializeClassInTarget(JitStackFrameOperation.class);
    }

    protected RuntimeCompilerScheme targetGenerator() {
        return targetCompiler;
    }

    public static Class initializeClassInTarget(final Class javaClass) {
        return MaxineVM.usingTarget(new Function<Class>() {
            public Class call() {
                assert !MaxineVM.isHostedOnly(javaClass);
                final Class targetClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, javaClass.getName());
                final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(targetClass);
                MakeClassInitialized.makeClassInitialized(tupleClassActor);
                return targetClass;
            }
        });
    }

    public static boolean hasStackParameters(ClassMethodActor classMethodActor) {
        TargetABI abi = VMConfiguration.target().targetABIsScheme().optimizedJavaABI;
        TargetLocation[] locations = abi.getParameterTargetLocations(classMethodActor.getParameterKinds());
        for (TargetLocation location : locations) {
            if (!(location instanceof IntegerRegister) && !(location instanceof FloatingPointRegister)) {
                return true;
            }
        }
        return false;
    }

    public TargetMethod generateTargetTemplate(final ClassMethodActor sourceTemplate) {
        return MaxineVM.usingTarget(new Function<TargetMethod>() {
            public TargetMethod call() {
                ProgramError.check(hasStackParameters(sourceTemplate), "Template must not have *any* stack parameters: " + sourceTemplate);
                final TargetMethod targetMethod = targetGenerator().compile(sourceTemplate);
                ProgramError.check(targetMethod.referenceLiterals() == null, "Template must not have *any* reference literals: " + targetMethod);
                ProgramError.check(targetMethod.scalarLiterals() == null, "Template must not have *any* scalar literals: " + targetMethod);
                return targetMethod;
            }
        });
    }
}

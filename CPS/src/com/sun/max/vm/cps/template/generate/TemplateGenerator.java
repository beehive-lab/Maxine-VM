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
package com.sun.max.vm.cps.template.generate;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;

import com.sun.c1x.util.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.Snippet.MakeClassInitialized;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.template.source.*;
import com.sun.max.vm.hosted.*;

@HOSTED_ONLY
public class TemplateGenerator {

    protected RuntimeCompilerScheme targetCompiler;

    public TemplateGenerator() {
        targetCompiler = CPSCompiler.Static.compiler();

        // Make sure the JavaStackFrame class used for generating the templates are initialized in the target.
        // This will enable all sorts of compiler optimization that we want the templates to benefit from.
        initializeClassInTarget(JitStackFrameOperation.class);
    }

    protected RuntimeCompilerScheme targetGenerator() {
        return targetCompiler;
    }

    public static Class initializeClassInTarget(final Class javaClass) {
        assert !MaxineVM.isHostedOnly(javaClass);
        final Class targetClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, javaClass.getName());
        final TupleClassActor tupleClassActor = (TupleClassActor) ClassActor.fromJava(targetClass);
        MakeClassInitialized.makeClassInitialized(tupleClassActor);
        return targetClass;
    }

    public static boolean hasStackParameters(ClassMethodActor classMethodActor) {
        CiKind receiver = !classMethodActor.isStatic() ? classMethodActor.holder().kind() : null;
        for (CiValue arg : vm().registerConfigs.standard.getCallingConvention(Type.JavaCall, Util.signatureToKinds(classMethodActor.signature(), receiver), target()).locations) {
            if (!arg.isRegister()) {
                return true;
            }
        }
        return false;
    }

    public TargetMethod generateTargetTemplate(final ClassMethodActor sourceTemplate) {
        ProgramError.check(hasStackParameters(sourceTemplate), "Template must not have *any* stack parameters: " + sourceTemplate);
        final TargetMethod targetMethod = targetGenerator().compile(sourceTemplate);
        ProgramError.check(targetMethod.referenceLiterals() == null, "Template must not have *any* reference literals: " + targetMethod);
        ProgramError.check(targetMethod.scalarLiterals() == null, "Template must not have *any* scalar literals: " + targetMethod);
        return targetMethod;
    }
}

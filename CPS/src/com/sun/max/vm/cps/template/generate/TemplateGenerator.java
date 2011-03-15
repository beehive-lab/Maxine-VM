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

    protected RuntimeCompiler targetCompiler;

    public TemplateGenerator() {
        targetCompiler = CPSCompiler.Static.compiler();

        // Make sure the JavaStackFrame class used for generating the templates are initialized in the target.
        // This will enable all sorts of compiler optimization that we want the templates to benefit from.
        initializeClassInTarget(JitStackFrameOperation.class);
    }

    protected RuntimeCompiler targetGenerator() {
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
        final TargetMethod targetMethod = targetGenerator().compile(sourceTemplate, true, null);
        ProgramError.check(targetMethod.referenceLiterals() == null, "Template must not have *any* reference literals: " + targetMethod);
        ProgramError.check(targetMethod.scalarLiterals() == null, "Template must not have *any* scalar literals: " + targetMethod);
        return targetMethod;
    }
}

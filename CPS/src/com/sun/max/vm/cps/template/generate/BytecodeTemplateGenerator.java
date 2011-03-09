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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.template.*;

/**
 * A simple generator of templates. A template is a sequence of code that implements a bytecode. It can be used to
 * generate an interpreter, or by a JIT to compile method bytecode.
 * <p>
 * This generator follows strictly the JVM specification in terms of input and output of each template. Each template
 * can assume the availability of a stack and an activation frame, the latter holding the local variables (including
 * incoming arguments for the current method invocation).
 * <p>
 * The generator itself is not meant to be part of the VM target image, at least currently.
 *
 * @author Laurent Daynes
 */
@HOSTED_ONLY
public class BytecodeTemplateGenerator extends TemplateGenerator {

    public BytecodeTemplateGenerator() {
        super();
    }

    private int maxTemplateFrameSize;

    public int maxTemplateFrameSize() {
        return maxTemplateFrameSize;
    }

    public void generateBytecodeTemplates(Class templateSourceClass, EnumMap<BytecodeTemplate, TargetMethod> templates) {
        // Before all, make sure the template source class is initialized in the target.
        // This will enable some compiler optimization that we want the templates to benefit from.
        final Class templateHolderClassInTarget = initializeClassInTarget(templateSourceClass);
        final Method[] templateMethods = templateHolderClassInTarget.getDeclaredMethods();
        for (Method method : templateMethods) {
            if (Platform.platform().isAcceptedBy(method.getAnnotation(PLATFORM.class))) {
                BYTECODE_TEMPLATE bct = method.getAnnotation(BYTECODE_TEMPLATE.class);
                if (bct != null) {
                    BytecodeTemplate bt = bct.value();
                    templates.put(bt, generateBytecodeTemplate(ClassMethodActor.fromJava(method)));
                }
            }
        }
    }

    private TargetMethod generateBytecodeTemplate(final ClassMethodActor bytecodeSourceTemplate) {
        if (hasStackParameters(bytecodeSourceTemplate)) {
            ProgramError.unexpected("Template must not have *any* stack parameters: " + bytecodeSourceTemplate, null);
        }
        final TargetMethod targetMethod = targetGenerator().compile(bytecodeSourceTemplate);
        if (!(targetMethod.referenceLiterals() == null)) {
            StringBuilder sb = new StringBuilder("Template must not have *any* reference literals: " + targetMethod);
            for (int i = 0; i < targetMethod.referenceLiterals().length; i++) {
                Object literal = targetMethod.referenceLiterals()[i];
                sb.append("\n  " + i + ": " + literal.getClass().getName() + " // \"" + literal + "\"");
            }
            ProgramError.unexpected(sb.toString());
        }
        if (targetMethod.scalarLiterals() != null) {
            ProgramError.unexpected("Template must not have *any* scalar literals: " + targetMethod + "\n\n" + targetMethod.traceToString(), null);
        }
        if (targetMethod.frameSize() > maxTemplateFrameSize) {
            maxTemplateFrameSize = targetMethod.frameSize();
        }
        return targetMethod;
    }

}

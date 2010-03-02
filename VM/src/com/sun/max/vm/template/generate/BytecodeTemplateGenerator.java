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
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.*;

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
            if (Platform.target().isAcceptedBy(method.getAnnotation(PLATFORM.class))) {
                BYTECODE_TEMPLATE bct = method.getAnnotation(BYTECODE_TEMPLATE.class);
                if (bct != null) {
                    BytecodeTemplate bt = bct.value();
                    templates.put(bt, generateBytecodeTemplate(method));
                }
            }
        }
    }

    private TargetMethod generateBytecodeTemplate(final ClassMethodActor bytecodeSourceTemplate) {
        return MaxineVM.usingTarget(new Function<TargetMethod>() {
            public TargetMethod call() {
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
        });
    }

    private TargetMethod generateBytecodeTemplate(Method bytecodeSourceTemplate) {
        final ClassMethodActor classMethodActor = ClassMethodActor.fromJava(bytecodeSourceTemplate);
        final TargetMethod template = generateBytecodeTemplate(classMethodActor);
        return template;
    }

}

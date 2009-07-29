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

import static com.sun.max.annotate.BYTECODE_TEMPLATE.Static.*;

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
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
@PROTOTYPE_ONLY
public class BytecodeTemplateGenerator extends TemplateGenerator {

    public BytecodeTemplateGenerator() {
        super();
    }

    private int maxTemplateFrameSize;

    public int maxTemplateFrameSize() {
        return maxTemplateFrameSize;
    }

    @Override
    protected boolean qualifiesAsTemplate(Method m) {
        return super.qualifiesAsTemplate(m) && bytecodeImplementedBy(m) != null;
    }

    public void generateBytecodeTemplates(Class templateSourceClass, VariableSequence<CompiledBytecodeTemplate> templates) {
        // Before all, make sure the template source class is initialized in the target.
        // This will enable some compiler optimization that we want the templates to benefit from.
        final Class templateHolderClassInTarget = initializeClassInTarget(templateSourceClass);
        final Method[] templateMethods = templateHolderClassInTarget.getDeclaredMethods();
        for (Method m : templateMethods) {
            if (qualifiesAsTemplate(m)) {
                templates.append(generateBytecodeTemplate(m));
            }
        }
    }

    private CompiledBytecodeTemplate generateBytecodeTemplate(final ClassMethodActor bytecodeSourceTemplate) {
        return MaxineVM.usingTarget(new Function<CompiledBytecodeTemplate>() {
            public CompiledBytecodeTemplate call() {
                ProgramError.check(!targetGenerator().hasStackParameters(bytecodeSourceTemplate), "Template must not have *any* stack parameters: " + bytecodeSourceTemplate);
                final TargetMethod targetMethod = targetGenerator().makeIrMethod(bytecodeSourceTemplate);
                if (!(targetMethod.referenceLiterals() == null)) {
                    ProgramError.unexpected("Template must not have *any* reference literals: " + targetMethod + " " + Arrays.toString(targetMethod.referenceLiterals()), null);
                }
                ProgramError.check(targetMethod.scalarLiterals() == null, "Template must not have *any* scalar literals: " + targetMethod);
                if (targetMethod.frameSize() > maxTemplateFrameSize) {
                    maxTemplateFrameSize = targetMethod.frameSize();
                }
                return new CompiledBytecodeTemplate(targetMethod);
            }
        });
    }

    private CompiledBytecodeTemplate generateBytecodeTemplate(Method bytecodeSourceTemplate) {
        final ClassMethodActor classMethodActor = (ClassMethodActor) MethodActor.fromJava(bytecodeSourceTemplate);
        final CompiledBytecodeTemplate template = generateBytecodeTemplate(classMethodActor);
        return template;
    }

}

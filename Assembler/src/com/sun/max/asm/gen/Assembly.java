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
package com.sun.max.asm.gen;

import java.lang.reflect.*;

import com.sun.max.*;
import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * An assembly framework, instantiated once per instruction set.
 *
 * @author Bernd Mathiske
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class Assembly<Template_Type extends Template> {

    private static MaxPackage instructionSetPackage(InstructionSet instructionSet) {
        final MaxPackage thisPackage = MaxPackage.fromClass(Assembly.class);
        return thisPackage.subPackage(instructionSet.category().name().toLowerCase(), instructionSet.name().toLowerCase());
    }

    private final InstructionSet instructionSet;
    private final Class<Template_Type> templateType;

    protected Assembly(InstructionSet instructionSet, Class<Template_Type> templateType) {
        this.instructionSet = instructionSet;
        this.templateType = templateType;
    }

    public InstructionSet instructionSet() {
        return instructionSet;
    }

    public Class<Template_Type> templateType() {
        return templateType;
    }

    public MaxPackage getPackage() {
        return instructionSetPackage(instructionSet);
    }

    protected abstract Sequence<Template_Type> createTemplates();

    private Sequence<Template_Type> templates;
    private AppendableSequence<Template_Type> labelTemplates;

    public final Sequence<Template_Type> templates() {
        if (templates == null) {
            templates = createTemplates();
        }
        return templates;
    }

    public final Sequence<Template_Type> labelTemplates() {
        if (labelTemplates == null) {
            labelTemplates = new LinkSequence<Template_Type>();
            for (Template_Type template : templates()) {
                if (!template.isRedundant() && template.labelParameterIndex() >= 0) {
                    labelTemplates.append(template);
                }
            }
        }
        return labelTemplates;
    }

    public abstract BitRangeOrder bitRangeEndianness();

    private Object getBoxedJavaValue(Argument argument) {
        if (argument instanceof ImmediateArgument) {
            final ImmediateArgument immediateArgument = (ImmediateArgument) argument;
            return immediateArgument.boxedJavaValue();
        }
        return argument;
    }

    public final String createMethodCallString(Template template, IndexedSequence<Argument> argumentList) {
        assert argumentList.length() == template.parameters().length();
        String call = template.assemblerMethodName() + "(";
        for (int i = 0; i < argumentList.length(); i++) {
            call += ((i == 0) ? "" : ", ") + getBoxedJavaValue(argumentList.get(i));
        }
        return call + ")";
    }

    private Method getAssemblerMethod(Assembler assembler, Template template, Class[] parameterTypes) throws NoSuchAssemblerMethodError {
        try {
            return assembler.getClass().getMethod(template.assemblerMethodName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new NoSuchAssemblerMethodError(e.getMessage(), template);
        }
    }

    private Method getAssemblerMethod(Assembler assembler, Template template, IndexedSequence<Argument> arguments) throws NoSuchAssemblerMethodError {
        final Class[] parameterTypes = template.parameterTypes();
        final int index = template.labelParameterIndex();
        if (index >= 0 && arguments.get(index) instanceof Label) {
            parameterTypes[index] = Label.class;
            return getAssemblerMethod(assembler, template, parameterTypes);
        }
        if (template.assemblerMethod == null) {
            template.assemblerMethod = getAssemblerMethod(assembler, template, parameterTypes);
        }
        return template.assemblerMethod;
    }

    public void assemble(Assembler assembler, Template template, IndexedSequence<Argument> arguments) throws AssemblyException, NoSuchAssemblerMethodError {
        assert arguments.length() == template.parameters().length();
        final Method assemblerMethod = getAssemblerMethod(assembler, template, arguments);
        final Object[] objects = new Object[arguments.length()];
        for (int i = 0; i < arguments.length(); i++) {
            objects[i] = getBoxedJavaValue(arguments.get(i));
        }
        try {
            assemblerMethod.invoke(assembler, objects);
        } catch (IllegalArgumentException illegalArgumentException) {
            ProgramError.unexpected("argument type mismatch", illegalArgumentException);
        } catch (IllegalAccessException illegalAccessException) {
            ProgramError.unexpected("illegal access to assembler method unexpected", illegalAccessException);
        } catch (InvocationTargetException invocationTargetException) {
            final Throwable target = invocationTargetException.getTargetException();
            if (target instanceof AssemblyException) {
                throw (AssemblyException) target;
            }
            if (target instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) target;
            }
            ProgramError.unexpected(invocationTargetException);
        }
    }
}

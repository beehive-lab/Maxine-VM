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

import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * An internal representation of an assembler method.
 *
 * @author Bernd Mathiske
 */
public abstract class Template implements Cloneable, Comparable<Template> {

    private int serial = -1;
    private InstructionDescription instructionDescription;
    private int labelParameterIndex = -1;

    protected Template(InstructionDescription instructionDescription) {
        this.instructionDescription = instructionDescription;
    }

    protected Template(InstructionDescription instructionDescription, int serial) {
        this.instructionDescription = instructionDescription;
        this.serial = serial;
    }

    public int serial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public InstructionDescription instructionDescription() {
        return instructionDescription;
    }

    /**
     * Gets the index of this template's parameter that can be represented as a {@linkplain Label label}.
     * A template is guaranteed to at most one such parameter.
     *
     * @return the index of this template's label parameter or -1 if it does not have one
     */
    public int labelParameterIndex() {
        return labelParameterIndex;
    }

    /**
     * Call this right before adding a parameter that may be represented by a label.
     */
    protected void setLabelParameterIndex() {
        if (labelParameterIndex != -1) {
            ProgramError.unexpected("a template can have at most one label parameter");
        }
        labelParameterIndex = parameters().length();
    }

    public abstract String assemblerMethodName();

    protected Method assemblerMethod;

    /**
     * Determines if this template is redundant with respect to another
     * {@linkplain #canonicalRepresentative() canonical} template.
     * Two templates are redundant if they both have the same name and operands.
     * Redundant pairs of instructions are assumed to implement the same machine
     * instruction semantics but may have different encodings.
     *
     * @return whether this template is redundant with respect some other template
     */
    public final boolean isRedundant() {
        return canonicalRepresentative() != null;
    }

    /**
     * @see #isRedundant()
     */
    public abstract Template canonicalRepresentative();

    /**
     * The name of the Java method that will be created from this template.
     */
    private String internalName;

    public String internalName() {
        return internalName;
    }

    protected void setInternalName(String name) {
        this.internalName = name;
    }

    public String externalName() {
        if (instructionDescription.externalName() != null) {
            return instructionDescription.externalName();
        }
        return internalName();
    }

    public boolean isDisassemblable() {
        return instructionDescription.isDisassemblable();
    }

    public boolean isExternallyTestable() {
        return instructionDescription.isExternallyTestable();
    }

    public abstract Sequence<? extends Operand> operands();

    public abstract IndexedSequence<? extends Parameter> parameters();

    /**
     * Gets the argument from a given list of arguments corresponding to a parameter of this template.
     *
     * @return the argument at index {@code i} in {@code arguments} where {@code parameter == parameters().get(i)}
     */
    public Argument bindingFor(Parameter parameter, IndexedSequence<Argument> arguments) {
        final Sequence< ? extends Parameter> parameters = parameters();
        assert arguments.length() == parameters.length();
        final int index = Sequence.Static.indexOfIdentical(parameters, parameter);
        ProgramError.check(index != -1, parameter + " is not a parameter of " + externalName());
        return arguments.get(index);
    }

    public Class[] parameterTypes() {
        final Class[] parameterTypes = new Class[parameters().length()];
        for (int i = 0; i < parameters().length(); i++) {
            parameterTypes[i] = parameters().get(i).type();
        }
        return parameterTypes;
    }

    @Override
    public Template clone() {
        try {
            final Template result = (Template) super.clone();
            result.instructionDescription = instructionDescription.clone();
            return result;
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected(cloneNotSupportedException);
        }
    }

    public int compareTo(Template other) {
        int result = internalName.compareTo(other.internalName);
        if (result != 0) {
            return result;
        }
        final IndexedSequence<? extends Parameter> myParameters = parameters();
        final IndexedSequence<? extends Parameter> otherParameters = other.parameters();
        final int n = Math.min(myParameters.length(), otherParameters.length());
        for (int i = 0; i < n; i++) {
            result = myParameters.get(i).compareTo(otherParameters.get(i));
            if (result != 0) {
                return result;
            }
        }
        return new Integer(myParameters.length()).compareTo(otherParameters.length());
    }

    public final boolean isEquivalentTo(Template other) {
        if (this == other) {
            return true;
        }
        Template a = this;
        if (a.canonicalRepresentative() != null) {
            a = a.canonicalRepresentative();
        }
        Template b = other;
        if (b.canonicalRepresentative() != null) {
            b = b.canonicalRepresentative();
        }
        return a == b;
    }
}

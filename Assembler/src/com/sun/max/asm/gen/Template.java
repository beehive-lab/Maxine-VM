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

    private int _serial = -1;
    private InstructionDescription _instructionDescription;
    private int _labelParameterIndex = -1;

    protected Template(InstructionDescription instructionDescription) {
        _instructionDescription = instructionDescription;
    }

    protected Template(InstructionDescription instructionDescription, int serial) {
        _instructionDescription = instructionDescription;
        _serial = serial;
    }

    public int serial() {
        return _serial;
    }

    public void setSerial(int serial) {
        _serial = serial;
    }

    public InstructionDescription instructionDescription() {
        return _instructionDescription;
    }

    /**
     * Gets the index of this template's parameter that can be represented as a {@linkplain Label label}.
     * A template is guaranteed to at most one such parameter.
     * 
     * @return the index of this template's label parameter or -1 if it does not have one
     */
    public int labelParameterIndex() {
        return _labelParameterIndex;
    }

    /**
     * Call this right before adding a parameter that may be represented by a label.
     */
    protected void setLabelParameterIndex() {
        if (_labelParameterIndex != -1) {
            ProgramError.unexpected("a template can have at most one label parameter");
        }
        _labelParameterIndex = parameters().length();
    }

    public abstract String assemblerMethodName();

    protected Method _assemblerMethod;

    public abstract boolean isRedundant();

    /**
     * The name of the Java method that will be created from this template.
     */
    private String _internalName;

    public String internalName() {
        return _internalName;
    }

    protected void setInternalName(String internalName) {
        _internalName = internalName;
    }

    public String externalName() {
        if (_instructionDescription.externalName() != null) {
            return _instructionDescription.externalName();
        }
        return internalName();
    }

    public boolean isDisassemblable() {
        return _instructionDescription.isDisassemblable();
    }

    public boolean isExternallyTestable() {
        return _instructionDescription.isExternallyTestable();
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
            result._instructionDescription = _instructionDescription.clone();
            return result;
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected(cloneNotSupportedException);
        }
    }

    public int compareTo(Template other) {
        int result = _internalName.compareTo(other._internalName);
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

    public boolean isEquivalentTo(Template other) {
        return this == other;
    }
}

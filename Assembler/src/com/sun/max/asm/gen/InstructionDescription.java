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

import java.util.*;

import com.sun.max.asm.gen.cisc.x86.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A sequence of objects that describe group of closely related instructions. An
 * {@link #Template instruction template} is created for each instruction in the
 * group.
 * <p>
 * The types of objects that an instruction description contains
 * depend on the whether the underlying platform is CISC or RISC.
 * The types for these two instruction categories are enumerated by
 * the {@code visit...} methods in the {@link RiscInstructionDescriptionVisitor}
 * and {@link X86InstructionDescriptionVisitor} classes.
 *
 * @author Bernd Mathiske
 */
public abstract class InstructionDescription implements Iterable<Object>, Cloneable {

    private static int nextSerial;

    private int serial;

    /**
     * The components of the description.
     */
    private final MutableSequence<Object> specifications;

    public InstructionDescription(MutableSequence<Object> specifications) {
        this.specifications = specifications;
        this.serial = nextSerial++;
    }

    public int serial() {
        return serial;
    }

    /**
     * @return the objects from which this description is composed
     */
    public MutableSequence<Object> specifications() {
        return specifications;
    }

    private Sequence<InstructionConstraint> constraints;

    /**
     * @return the {@link InstructionConstraint} instances (if any) within this description
     */
    public Sequence<InstructionConstraint> constraints() {
        if (constraints == null) {
            constraints = Sequence.Static.filter(specifications, InstructionConstraint.class);
        }
        return constraints;
    }

    private String architectureManualSection;

    public InstructionDescription setArchitectureManualSection(String section) {
        architectureManualSection = section;
        return this;
    }

    public String architectureManualSection() {
        return architectureManualSection;
    }

    private String externalName;

    public String externalName() {
        return externalName;
    }

    public InstructionDescription setExternalName(String name) {
        this.externalName = name;
        return this;
    }

    private boolean isDisassemblable = true;

    /**
     * Determines if the templates created from the description can be recovered from an assembled instruction.
     * This is almost always possible. One example where it isn't is an instruction description that
     * has a parameter that is not correlated one-to-one with some bits in the encoded instruction.
     * In RISC architectures, this will be any instruction that has at least one {@link InputOperandField}
     * parameter.
     */
    public boolean isDisassemblable() {
        return isDisassemblable;
    }

    public InstructionDescription beNotDisassemblable() {
        isDisassemblable = false;
        return this;
    }

    public boolean isSynthetic() {
        return false;
    }

    private boolean isExternallyTestable = true;

    public boolean isExternallyTestable() {
        return isExternallyTestable;
    }

    public InstructionDescription beNotExternallyTestable() {
        isExternallyTestable = false;
        return this;
    }

    private WordWidth requiredAddressSize;

    public WordWidth requiredAddressSize() {
        return requiredAddressSize;
    }

    public InstructionDescription requireAddressSize(WordWidth addressSize) {
        this.requiredAddressSize = addressSize;
        return this;
    }

    private WordWidth requiredOperandSize;

    public WordWidth requiredOperandSize() {
        return requiredOperandSize;
    }

    public InstructionDescription requireOperandSize(WordWidth operandSize) {
        this.requiredOperandSize = operandSize;
        return this;
    }

    public Iterator<Object> iterator() {
        return specifications.iterator();
    }

    @Override
    public InstructionDescription clone() {
        try {
            final InstructionDescription clone = (InstructionDescription) super.clone();
            clone.serial = ++nextSerial;
            return clone;
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected(cloneNotSupportedException);
        }
    }

    @Override
    public final int hashCode() {
        return serial;
    }

    @Override
    public final boolean equals(Object object) {
        if (object instanceof InstructionDescription) {
            return serial == ((InstructionDescription) object).serial;
        }
        return false;
    }

}

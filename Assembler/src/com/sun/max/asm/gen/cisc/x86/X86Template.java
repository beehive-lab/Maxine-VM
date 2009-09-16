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
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.*;
import com.sun.max.asm.x86.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public abstract class X86Template extends Template implements X86InstructionDescriptionVisitor {
    private final InstructionAssessment instructionFamily;
    private boolean hasSibByte;
    private final X86TemplateContext context;
    private HexByte instructionSelectionPrefix;
    private HexByte opcode1;
    private HexByte opcode2;
    private ModRMGroup modRMGroup;
    private ModRMGroup.Opcode modRMGroupOpcode;
    private AppendableSequence<X86Operand> operands = new LinkSequence<X86Operand>();
    private AppendableSequence<X86ImplicitOperand> implicitOperands = new LinkSequence<X86ImplicitOperand>();
    private AppendableIndexedSequence<X86Parameter> parameters = new ArrayListSequence<X86Parameter>();
    protected boolean isLabelMethodWritten;

    protected X86Template(X86InstructionDescription instructionDescription, int serial, InstructionAssessment instructionFamily, X86TemplateContext context) {
        super(instructionDescription, serial);
        this.instructionFamily = instructionFamily;
        this.context = context;
    }

    @Override
    public X86InstructionDescription instructionDescription() {
        return (X86InstructionDescription) super.instructionDescription();
    }

    protected X86TemplateContext context() {
        return context;
    }

    public HexByte instructionSelectionPrefix() {
        return instructionSelectionPrefix;
    }

    public HexByte opcode1() {
        return opcode1;
    }

    public HexByte opcode2() {
        return opcode2;
    }

    public boolean hasModRMByte() {
        return instructionFamily.hasModRMByte();
    }

    public X86TemplateContext.ModCase modCase() {
        return context.modCase();
    }

    public ModRMGroup modRMGroup() {
        return modRMGroup;
    }

    public ModRMGroup.Opcode modRMGroupOpcode() {
        return modRMGroupOpcode;
    }

    public X86TemplateContext.RMCase rmCase() {
        return context.rmCase();
    }

    public boolean hasSibByte() {
        return hasSibByte;
    }

    protected void haveSibByte() {
        hasSibByte = true;
    }

    public X86TemplateContext.SibBaseCase sibBaseCase() {
        return context.sibBaseCase();
    }

    public WordWidth addressSizeAttribute() {
        return context.addressSizeAttribute();
    }

    public WordWidth operandSizeAttribute() {
        return context.operandSizeAttribute();
    }

    private WordWidth externalCodeSizeAttribute;

    public WordWidth externalCodeSizeAttribute() {
        return externalCodeSizeAttribute;
    }

    protected void setExternalCodeSizeAttribute(WordWidth codeSizeAttribute) {
        this.externalCodeSizeAttribute = codeSizeAttribute;
    }

    @Override
    public String internalName() {
        String result = super.internalName();
        if (result != null && internalOperandTypeSuffix != null) {
            result += internalOperandTypeSuffix;
        }
        return result;
    }

    @Override
    public String externalName() {
        if (instructionDescription().externalName() != null) {
            return instructionDescription().externalName();
        }
        String result = super.internalName();
        if (externalOperandTypeSuffix != null) {
            result += externalOperandTypeSuffix;
        }
        return result;
    }

    private String format(HexByte parameter) {
        return parameter == null ? "" : parameter.toString() + ", ";
    }

    @Override
    public String toString() {
        return "<X86Template #" + serial() + ": " + internalName() + " " + format(instructionSelectionPrefix) + format(opcode1) + format(opcode2) + operands + ">";
    }

    private String namePrefix = "";

    protected void useNamePrefix(String prefix) {
        if (this.namePrefix.length() == 0) {
            this.namePrefix = prefix;
        }
    }

    /**
     * @see #computeRedundancyWith(X86Template)
     */
    private X86Template canonicalRepresentative;

    @Override
    public Template canonicalRepresentative() {
        return canonicalRepresentative;
    }

    private String canonicalName;

    public String canonicalName() {
        if (canonicalName == null) {
            canonicalName = namePrefix + internalName();
            if (implicitOperands.length() == 1) {
                final X86ImplicitOperand implicitOperand = implicitOperands.first();
                switch (implicitOperand.designation()) {
                    case DESTINATION:
                    case OTHER:
                        break;
                    case SOURCE:
                        canonicalName += "__";
                        break;
                }
                canonicalName += "_" + implicitOperand.name();
            } else {
                for (X86ImplicitOperand implicitOperand : implicitOperands) {
                    canonicalName += "_" + implicitOperand.name();
                }
            }
        }
        return canonicalName;
    }

    @Override
    public String assemblerMethodName() {
        if (isRedundant()) {
            return canonicalName() + "_r" + serial();
        }
        return canonicalName();
    }

    public boolean isExternalOperandOrderingInverted() {
        return instructionDescription().isExternalOperandOrderingInverted();
    }

    public InstructionDescription modRMInstructionDescription() {
        if (modRMGroup == null) {
            return null;
        }
        return modRMGroup.getInstructionDescription(modRMGroupOpcode);
    }

    protected <Parameter_Type extends X86Parameter> Parameter_Type addParameter(Parameter_Type parameter) {
        parameters.append(parameter);
        operands.append(parameter);
        if (parameter instanceof X86AddressParameter) {
            useNamePrefix("m_");
        }
        return parameter;
    }

    protected void addParameter(X86Parameter parameter, ArgumentRange argumentRange) {
        addParameter(parameter);
        parameter.setArgumentRange(argumentRange);
    }

    protected void addParameter(X86Parameter parameter, ArgumentRange argumentRange, TestArgumentExclusion testArgumentExclusion) {
        addParameter(parameter, argumentRange);
        parameter.excludeTestArguments(testArgumentExclusion);
    }

    protected <EnumerableArgument_Type extends Enum<EnumerableArgument_Type> & EnumerableArgument<EnumerableArgument_Type>> X86Parameter addEnumerableParameter(X86Operand.Designation designation, ParameterPlace parameterPlace,
                                            final Enumerator<EnumerableArgument_Type> enumerator) {
        return addParameter(new X86EnumerableParameter<EnumerableArgument_Type>(designation, parameterPlace, enumerator));
    }

    protected void addImplicitOperand(X86ImplicitOperand implicitOperand) {
        implicitOperands.append(implicitOperand);
        operands.append(implicitOperand);
    }

    public Sequence<X86ImplicitOperand> implicitOperands() {
        return implicitOperands;
    }

    @Override
    public Sequence<X86Operand> operands() {
        return operands;
    }

    @Override
    public IndexedSequence<X86Parameter> parameters() {
        return parameters;
    }

    public void visitAddressingMethodCode(AddressingMethodCode addressingMethodCode, X86Operand.Designation designation) throws TemplateNotNeededException {
        switch (addressingMethodCode) {
            case M: {
                visitOperandCode(OperandCode.Mv, designation, ArgumentRange.UNSPECIFIED, TestArgumentExclusion.NONE);
                break;
            }
            default: {
                ProgramError.unexpected("don't know what to do with addressing method code: " + addressingMethodCode);
            }
        }
    }

    private String getOperandTypeSuffix(OperandTypeCode operandTypeCode) throws TemplateNotNeededException {
        switch (operandTypeCode) {
            case b:
                return "b";
            case z:
                if (operandSizeAttribute() != addressSizeAttribute()) {
                    throw TemplateNotNeededException.raise();
                }
            case d_q:
            case v:
                switch (operandSizeAttribute()) {
                    case BITS_16:
                        return "w";
                    case BITS_32:
                        return "l";
                    case BITS_64:
                        return "q";
                    default:
                        throw ProgramError.unexpected();
                }
            default:
                break;
        }
        return operandTypeCode.name();
    }

    private void checkSuffix(String newSuffix, String oldSuffix) {
        if (oldSuffix != null) {
            ProgramError.check(newSuffix.equals(oldSuffix), "conflicting operand type codes specified: " + newSuffix + " vs. " + oldSuffix);
        }
    }

    private String externalOperandTypeSuffix;

    private void setExternalOperandTypeSuffix(String suffix) {
        checkSuffix(suffix, externalOperandTypeSuffix);
        externalOperandTypeSuffix = suffix;
    }

    protected void setExternalOperandTypeSuffix(OperandTypeCode operandTypeCode) throws TemplateNotNeededException {
        setExternalOperandTypeSuffix(getOperandTypeSuffix(operandTypeCode));
    }

    private String internalOperandTypeSuffix;

    protected void setOperandTypeSuffix(String suffix) {
        setExternalOperandTypeSuffix(suffix);
        checkSuffix(suffix, internalOperandTypeSuffix);
        internalOperandTypeSuffix = suffix;
    }

    public void visitOperandTypeCode(OperandTypeCode operandTypeCode) throws TemplateNotNeededException {
        setOperandTypeSuffix(getOperandTypeSuffix(operandTypeCode));
    }

    public void visitGeneralRegister(GeneralRegister generalRegister, X86Operand.Designation designation, ImplicitOperand.ExternalPresence externalPresence) {
        addImplicitOperand(new X86ImplicitOperand(designation, externalPresence, generalRegister));
    }

    public void visitSegmentRegister(SegmentRegister segmentRegister, X86Operand.Designation designation) {
        addImplicitOperand(new X86ImplicitOperand(designation, ImplicitOperand.ExternalPresence.EXPLICIT, segmentRegister));
    }

    public void visitModRMGroup(ModRMGroup modRM) throws TemplateNotNeededException {
        this.modRMGroup = modRM;
        final ModRMDescription instructionDescription = modRM.getInstructionDescription(context.modRMGroupOpcode());
        if (instructionDescription == null) {
            TemplateNotNeededException.raise();
        }
        this.modRMGroupOpcode = instructionDescription.opcode();
        setInternalName(instructionDescription.name().toLowerCase());
    }

    public void visitModCase(X86TemplateContext.ModCase modCase) throws TemplateNotNeededException {
        if (context.modCase() != X86TemplateContext.ModCase.MOD_3) {
            TemplateNotNeededException.raise();
        }
    }

    public void visitInstructionConstraint(InstructionConstraint constraint) {
    }

    protected abstract void organize_M(X86Operand.Designation designation) throws TemplateNotNeededException;

    protected <EnumerableArgument_Type extends Enum<EnumerableArgument_Type> & EnumerableArgument<EnumerableArgument_Type>> void organize_E(X86Operand.Designation designation, ParameterPlace place,
                    final Enumerator<EnumerableArgument_Type> registerEnumerator, TestArgumentExclusion testArgumentExclusion) throws TemplateNotNeededException {
        if (context().modCase() == X86TemplateContext.ModCase.MOD_3) {
            switch (context().rmCase()) {
                case NORMAL:
                    addEnumerableParameter(designation, place, registerEnumerator).excludeTestArguments(testArgumentExclusion);
                    break;
                default:
                    TemplateNotNeededException.raise();
            }
        } else {
            organize_M(designation);
        }
    }

    public void visitFloatingPointOperandCode(FloatingPointOperandCode floatingPointOperandCode, X86Operand.Designation designation,
                                              final TestArgumentExclusion testArgumentExclusion) throws TemplateNotNeededException {
        switch (floatingPointOperandCode) {
            case ST_i:
                addEnumerableParameter(designation, ParameterPlace.OPCODE2, FPStackRegister.ENUMERATOR).excludeTestArguments(testArgumentExclusion);
                break;
            default:
                setOperandTypeSuffix(floatingPointOperandCode.operandTypeSuffix());
                organize_M(designation);
                break;
        }
    }

    public void visitFPStackRegister(FPStackRegister fpStackRegister, X86Operand.Designation designation) {
        addImplicitOperand(new X86ImplicitOperand(designation, ImplicitOperand.ExternalPresence.EXPLICIT, fpStackRegister));
    }

    public void visitString(String string) {
        assert internalName() == null;
        setInternalName(string.toLowerCase());
    }

    public void visitInteger(Integer integer, X86Operand.Designation designation) {
        addImplicitOperand(new X86ImplicitOperand(designation, ImplicitOperand.ExternalPresence.EXPLICIT, new Immediate8Argument((byte) integer.intValue())));
    }

    public void visitHexByte(HexByte hexByte) throws TemplateNotNeededException {
        if (opcode1 == null) {
            opcode1 = hexByte;
        } else if (opcode2 == null) {
            opcode2 = hexByte;
        } else {
            if (hexByte == HexByte._66 && context.operandSizeAttribute() == WordWidth.BITS_16) {
                TemplateNotNeededException.raise();
            }
            assert instructionSelectionPrefix == null;
            instructionSelectionPrefix = opcode1;
            opcode1 = opcode2;
            opcode2 = hexByte;
        }
    }

    /**
     * Determines if this template is redundant with respect to a given template.
     * Two templates are redundant if they both have the same name and operands.
     * Redundant pairs of instructions are assumed to implement the same machine
     * instruction semantics but have different encodings.
     *
     * @param other another template to compare against
     * @return whether this template is redundant with respect to {@code other}
     */
    public boolean computeRedundancyWith(X86Template other) {
        if (canonicalRepresentative != null) {
            assert canonicalRepresentative == other;
            return true;
        }
        if (!canonicalName().equals(other.canonicalName())) {
            return false;
        }
        if (parameters.length() != other.parameters.length()) {
            return false;
        }
        for (int i = 0; i < parameters.length(); i++) {
            if (!parameters.get(i).type().equals(other.parameters.get(i).type())) {
                return false;
            }
        }

        canonicalRepresentative = other;
        return true;
    }

}

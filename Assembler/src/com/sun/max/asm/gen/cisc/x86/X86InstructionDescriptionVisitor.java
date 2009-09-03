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

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.*;
import com.sun.max.asm.x86.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * Almost like the visitor pattern.
 * Specialized dispatch over InstructionDescription objects.
 *
 * @author Bernd Mathiske
 */
public interface X86InstructionDescriptionVisitor {

    void visitOperandCode(OperandCode operandCode, X86Operand.Designation designation, ArgumentRange argumentRange, TestArgumentExclusion testArgumentExclusion) throws TemplateNotNeededException;

    void visitAddressingMethodCode(AddressingMethodCode addressingMethodCode, X86Operand.Designation designation) throws TemplateNotNeededException;

    void visitOperandTypeCode(OperandTypeCode operandTypeCode) throws TemplateNotNeededException;

    void visitRegisterOperandCode(RegisterOperandCode registerOperandCode, X86Operand.Designation designation, ImplicitOperand.ExternalPresence externalPresence);

    void visitGeneralRegister(GeneralRegister generalRegister, X86Operand.Designation designation, ImplicitOperand.ExternalPresence externalPresence);

    void visitSegmentRegister(SegmentRegister segmentRegister, X86Operand.Designation designation);

    void visitModRMGroup(ModRMGroup modRMGroup) throws TemplateNotNeededException;

    void visitModCase(X86TemplateContext.ModCase modCase) throws TemplateNotNeededException;

    void visitFloatingPointOperandCode(FloatingPointOperandCode floatingPointOperandCode, X86Operand.Designation designation, TestArgumentExclusion testArgumentExclusion) throws TemplateNotNeededException;

    void visitFPStackRegister(FPStackRegister fpStackRegister, X86Operand.Designation designation);

    void visitString(String string);

    void visitInteger(Integer integer, X86Operand.Designation designation);

    void visitHexByte(HexByte hexByte) throws TemplateNotNeededException;

    void visitInstructionConstraint(InstructionConstraint constraint);

    public static final class Static {
        private Static() {
        }

        /**
         * @return whether the specification constitutes an operand
         * @throws TemplateNotNeededException
         */
        private static boolean visitSpecification(X86InstructionDescriptionVisitor visitor, Object specification,
                                                  final X86Operand.Designation designation, ArgumentRange argumentRange,
                                                  final TestArgumentExclusion testArgumentExclusion, ImplicitOperand.ExternalPresence externalPresence) throws TemplateNotNeededException {
            if (specification instanceof OperandCode) {
                visitor.visitOperandCode((OperandCode) specification, designation, argumentRange, testArgumentExclusion);
                return true;
            } else if (specification instanceof AddressingMethodCode) {
                visitor.visitAddressingMethodCode((AddressingMethodCode) specification, designation);
                return true;
            } else if (specification instanceof OperandTypeCode) {
                visitor.visitOperandTypeCode((OperandTypeCode) specification);
                return false;
            } else if (specification instanceof RegisterOperandCode) {
                visitor.visitRegisterOperandCode((RegisterOperandCode) specification, designation, externalPresence);
                return true;
            } else if (specification instanceof GeneralRegister) {
                visitor.visitGeneralRegister((GeneralRegister) specification, designation, externalPresence);
                return true;
            } else if (specification instanceof SegmentRegister) {
                visitor.visitSegmentRegister((SegmentRegister) specification, designation);
                return true;
            } else if (specification instanceof ModRMGroup) {
                visitor.visitModRMGroup((ModRMGroup) specification);
                return false;
            } else if (specification instanceof X86TemplateContext.ModCase) {
                visitor.visitModCase((X86TemplateContext.ModCase) specification);
                return false;
            } else if (specification instanceof FloatingPointOperandCode) {
                visitor.visitFloatingPointOperandCode((FloatingPointOperandCode) specification, designation, testArgumentExclusion);
                return true;
            } else if (specification instanceof FPStackRegister) {
                visitor.visitFPStackRegister((FPStackRegister) specification, designation);
                return true;
            } else if (specification instanceof String) {
                visitor.visitString((String) specification);
                return false;
            } else if (specification instanceof Integer) {
                visitor.visitInteger((Integer) specification, designation);
                return true;
            } else if (specification instanceof ArgumentRange) {
                final ArgumentRange newArgumentRange = (ArgumentRange) specification;
                return visitSpecification(visitor, newArgumentRange.wrappedSpecification(), designation, newArgumentRange, testArgumentExclusion, externalPresence);
            } else if (specification instanceof HexByte) {
                visitor.visitHexByte((HexByte) specification);
                return false;
            } else if (specification instanceof TestArgumentExclusion) {
                final TestArgumentExclusion exclusion = (TestArgumentExclusion) specification;
                return visitSpecification(visitor, exclusion.wrappedSpecification(), designation, argumentRange, exclusion, externalPresence);
            } else if (specification instanceof ExternalOmission) {
                final ExternalOmission omission = (ExternalOmission) specification;
                return visitSpecification(visitor, omission.wrappedSpecification(), designation, argumentRange, testArgumentExclusion, ImplicitOperand.ExternalPresence.OMITTED);
            } else {
                throw ProgramError.unexpected("unknown instruction description specification: " + specification);
            }
        }

        /**
         * @return whether this instruction description is to be used to create a template in the given context
         */
        public static boolean visitInstructionDescription(X86InstructionDescriptionVisitor visitor, InstructionDescription instructionDescription) {
            try {
                int designationIndex = 0;
                for (Object specification : instructionDescription) {
                    final X86Operand.Designation designation = X86Operand.Designation.VALUES.get(designationIndex);
                    if (visitSpecification(visitor, specification, designation, ArgumentRange.UNSPECIFIED, TestArgumentExclusion.NONE, ImplicitOperand.ExternalPresence.EXPLICIT)) {
                        designationIndex++;
                    }
                }
                return true;
            } catch (TemplateNotNeededException templateNotNeededException) {
                return false;
            }
        }
    }
}

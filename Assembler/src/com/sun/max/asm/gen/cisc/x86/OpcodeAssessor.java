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
/*VCSID=3ebb6291-1727-41dd-9b94-fbf6528aa07d*/
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.cisc.*;

/**
 * Used when pre-scanning instruction descriptions to assess variants within the respective instruction family.
 *
 * @see InstructionAssessment
 * 
 * @author Bernd Mathiske
 */
public class OpcodeAssessor extends X86InstructionDescriptionAdapter {

    private final InstructionAssessment _instructionFamily;

    public OpcodeAssessor(InstructionAssessment instructionFamily) {
        super();
        _instructionFamily = instructionFamily;
    }

    @Override
    public void visitOperandCode(OperandCode operandCode, X86Operand.Designation designation, ArgumentRange argumentRange, TestArgumentExclusion testArgumentExclusion) {
        switch (operandCode.operandTypeCode()) {
            case a:
            case d_q:
            case p:
            case s:
            case v:
            case z:
                _instructionFamily.haveOperandSizeVariants();
                break;
            default:
                break;
        }
        switch (operandCode.addressingMethodCode()) {
            case A:
            case E:
            case M:
            case O:
            case Q:
            case W:
                _instructionFamily.haveAddressSizeVariants();
                break;
            default:
                break;
        }
        switch (operandCode.addressingMethodCode()) {
            case C:
            case D:
            case E:
            case G:
            case M:
            case P:
            case PR:
            case Q:
            case R:
            case S:
            case V:
            case VR:
            case T:
            case W:
                _instructionFamily.haveModRMByte();
                break;
            default:
                break;
        }
    }

    @Override
    public void visitRegisterOperandCode(RegisterOperandCode registerOperandCode, X86Operand.Designation position, ImplicitOperand.ExternalPresence externalPresence) {
        _instructionFamily.haveOperandSizeVariants();
    }

    @Override
    public void visitModRMGroup(ModRMGroup modRMGroup) {
        _instructionFamily.setModRMGroup(modRMGroup);
    }

    @Override
    public void visitModCase(X86TemplateContext.ModCase modCase) throws TemplateNotNeededException {
        _instructionFamily.haveModRMByte();
    }

    @Override
    public void visitString(String s) {
        if (s.startsWith("J") || s.startsWith("j")) {
            _instructionFamily.beJump();
        }
    }
}

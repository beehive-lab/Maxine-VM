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


/**
 * Some information about a family of instructions that have the same basic opcode.
 * 
 * @see OpcodeAssessor
 * 
 * @author Bernd Mathiske
 */
public class InstructionAssessment {

    private boolean _hasAddressSizeVariants;
    private boolean _hasOperandSizeVariants;
    private boolean _hasModRMByte;
    private ModRMGroup _modRMGroup;
    private boolean _isJump;

    public InstructionAssessment() {
    }

    public void haveAddressSizeVariants() {
        _hasAddressSizeVariants = true;
    }

    public boolean hasAddressSizeVariants() {
        return _hasAddressSizeVariants;
    }

    public void haveOperandSizeVariants() {
        _hasOperandSizeVariants = true;
    }

    public boolean hasOperandSizeVariants() {
        return _hasOperandSizeVariants;
    }

    public void haveModRMByte() {
        _hasModRMByte = true;
    }

    public boolean hasModRMByte() {
        return _hasModRMByte;
    }

    public void setModRMGroup(ModRMGroup modRMGroup) {
        _modRMGroup = modRMGroup;
        _hasModRMByte = modRMGroup != null;
    }

    public ModRMGroup modRMGroup() {
        return _modRMGroup;
    }

    public void beJump() {
        _isJump = true;
    }

    public boolean isJump() {
        return _isJump;
    }
}

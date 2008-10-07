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
/*VCSID=244d432e-90ce-461a-8624-7ca5452caeb7*/
package com.sun.max.asm.gen.cisc.x86;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * A bundle of choices one can make when creating a template (addressing modes and operand sizes).
 *
 * @author Bernd Mathiske
 */
public class X86TemplateContext implements Cloneable {

    /** 
     * ModRM  mod Field. See mod field in "A.3.1 ModRM Operand References"
     */
    public enum ModCase {
        MOD_0,
        MOD_1,
        MOD_2,
        MOD_3;

        public static final IndexedSequence<ModCase> VALUES = new ArraySequence<ModCase>(values());

        public int value() {
            return ordinal();
        }
    }

    /**
     * Addressing mode variants. See r/m field in "A.3.1 ModRM Operand References"
     */
    public enum RMCase {
        NORMAL(0), // all other addressing modes, e.g. registers
        SIB(4),  // Scale-Index-Base addressing mode, e.g. [SIB]; see "Table A-15. ModRM Memory References, 32-Bit and 64-Bit Addressing"
        SWORD(6), // indirect signed 16-bit displacement, e.g. [disp16]; see "Table A-13. ModRM Memory References, 16-Bit Addressing"
        SDWORD(5); // indirect signed 32-bit displacement, e.g. [disp32] or [rIP+disp32]; see "Table A-15. ModRM Memory References, 32-Bit and 64-Bit Addressing"

        public static final IndexedSequence<RMCase> VALUES = new ArraySequence<RMCase>(values());

        private final int _rmFieldValue;

        private RMCase(int rmFieldValue) {
            _rmFieldValue = rmFieldValue;
        }

        public int value() {
            return _rmFieldValue;
        }
    }

    /**
     * Classes of "index" fields for SIB. See "Table A-17. SIB Memory References".
     */
    public enum SibIndexCase {
        GENERAL_REGISTER, // index register specified
        NONE; // SIB index = 100b and REX.X = 0 - no index register specified

        public static final IndexedSequence<SibIndexCase> VALUES = new ArraySequence<SibIndexCase>(values());
    }

    /**
     * Classes of "base" fields for SIB. See "Table A-16. SIB base Field References".
     */
    public enum SibBaseCase {
        GENERAL_REGISTER, // general purpose register base
        SPECIAL; // /5 - immediate displacement base / rBP / r13)

        public static final IndexedSequence<SibBaseCase> VALUES = new ArraySequence<SibBaseCase>(values());
    }

    public X86TemplateContext() {
    }

    private WordWidth _addressSizeAttribute;

    public WordWidth addressSizeAttribute() {
        return _addressSizeAttribute;
    }

    public void setAddressSizeAttribute(WordWidth addressSizeAttribute) {
        _addressSizeAttribute = addressSizeAttribute;
    }

    private WordWidth _operandSizeAttribute;

    public WordWidth operandSizeAttribute() {
        return _operandSizeAttribute;
    }

    public void setOperandSizeAttribute(WordWidth operandSizeAttribute) {
        _operandSizeAttribute = operandSizeAttribute;
    }

    private ModRMGroup.Opcode _modRMGroupOpcode;

    public ModRMGroup.Opcode modRMGroupOpcode() {
        return _modRMGroupOpcode;
    }

    public void setModRMGroupOpcode(ModRMGroup.Opcode modRMGroupOpcode) {
        _modRMGroupOpcode = modRMGroupOpcode;
    }

    private ModCase _modCase;

    public ModCase modCase() {
        return _modCase;
    }

    public void setModCase(ModCase modCase) {
        _modCase = modCase;
    }

    private RMCase _rmCase;

    public RMCase rmCase() {
        return _rmCase;
    }

    public void setRMCase(RMCase rmCase) {
        _rmCase = rmCase;
    }

    private SibIndexCase _sibIndexCase;

    public SibIndexCase sibIndexCase() {
        return _sibIndexCase;
    }

    public void setSibIndexCase(SibIndexCase sibIndexCase) {
        _sibIndexCase = sibIndexCase;
    }

    protected SibBaseCase _sibBaseCase;

    public SibBaseCase sibBaseCase() {
        return _sibBaseCase;
    }

    public void setSibBaseCase(SibBaseCase sibBaseCase) {
        _sibBaseCase = sibBaseCase;
    }

    @Override
    public X86TemplateContext clone() {
        try {
            return (X86TemplateContext) super.clone();
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            throw ProgramError.unexpected("clone() failed", cloneNotSupportedException);
        }
    }

    @Override
    public String toString() {
        return "<Context: " + _addressSizeAttribute + ", " + _operandSizeAttribute + ", " + _modRMGroupOpcode + ", " + _modCase + ", " + _rmCase + ", " + _sibIndexCase + ", " + _sibBaseCase + ">";
    }
}

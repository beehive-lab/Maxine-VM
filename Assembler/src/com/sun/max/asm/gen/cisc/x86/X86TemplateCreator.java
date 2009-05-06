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


import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 */
public abstract class X86TemplateCreator<Template_Type extends X86Template> {

    private final Assembly _assembly;
    private final WordWidth _addressWidth;
    private X86InstructionDescription _instructionDescription;
    private InstructionAssessment _instructionAssessment;
    private X86TemplateContext _context;
    private int _serial = 1;

    protected X86TemplateCreator(Assembly assembly, WordWidth addressWidth) {
        _assembly = assembly;
        _addressWidth = addressWidth;
    }

    private final AppendableSequence<Template_Type> _templates = new ArrayListSequence<Template_Type>();

    public Sequence<Template_Type> templates() {
        return _templates;
    }

    private final Map<String, AppendableSequence<Template_Type>> _internalNameToTemplates = new HashMap<String, AppendableSequence<Template_Type>>();

    private void addTemplate(Template_Type template) {
        _templates.append(template);
        AppendableSequence<Template_Type> t = _internalNameToTemplates.get(template.internalName());
        if (t == null) {
            t = new LinkSequence<Template_Type>();
            _internalNameToTemplates.put(template.internalName(), t);
        }
        t.append(template);
    }

    private boolean isRedundant(X86Template template) {
        final Sequence<Template_Type> t = _internalNameToTemplates.get(template.internalName());
        if (t != null) {
            for (X86Template other : t) {
                if (template.isRedundant(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract Template_Type createTemplate(X86InstructionDescription instructionDescription, int serial, InstructionAssessment instructionFamily, X86TemplateContext context);

    private void createTemplate() {
        final Template_Type template = createTemplate(_instructionDescription, _serial, _instructionAssessment, _context);
        if (X86InstructionDescriptionVisitor.Static.visitInstructionDescription(template, _instructionDescription)) {
            final InstructionDescription modRMInstructionDescription = template.modRMInstructionDescription();
            if (modRMInstructionDescription != null && !X86InstructionDescriptionVisitor.Static.visitInstructionDescription(template, modRMInstructionDescription)) {
                return;
            }
            if (isRedundant(template)) {
                if (_assembly.usingRedundantTemplates()) {
                    template.beRedundant();
                } else {
                    return;
                }
            }
            addTemplate(template);
            _serial++;
        }
    }

    private void createTemplatesForSibBaseCases() {
        for (X86TemplateContext.SibBaseCase sibBaseCase : X86TemplateContext.SibBaseCase.VALUES) {
            if (sibBaseCase == X86TemplateContext.SibBaseCase.GENERAL_REGISTER || _context.modCase() == X86TemplateContext.ModCase.MOD_0) {
                _context = _context.clone();
                _context._sibBaseCase = sibBaseCase;
                createTemplate();
            }
        }
    }

    private void createTemplatesForSibIndexCases() {
        for (X86TemplateContext.SibIndexCase sibIndexCase : X86TemplateContext.SibIndexCase.VALUES) {
            _context = _context.clone();
            _context.setSibIndexCase(sibIndexCase);
            createTemplatesForSibBaseCases();
        }
    }

    private void createTemplatesForRMCases() {
        for (X86TemplateContext.RMCase rmCase : X86TemplateContext.RMCase.VALUES) {
            _context = _context.clone();
            _context.setRMCase(rmCase);
            switch (_context.modCase()) {
                case MOD_3: {
                    if (rmCase == X86TemplateContext.RMCase.NORMAL) {
                        createTemplate();
                    }
                    break;
                }
                default: {
                    switch (rmCase) {
                        case SIB:
                            createTemplatesForSibIndexCases();
                            break;
                        default:
                            createTemplate();
                            break;
                    }
                }
            }
        }
    }

    private void createTemplatesForModRMGroups() {
        if (_instructionAssessment.modRMGroup() != null) {
            for (ModRMGroup.Opcode modRMGroupOpcode : ModRMGroup.Opcode.VALUES) {
                _context = _context.clone();
                _context.setModRMGroupOpcode(modRMGroupOpcode);
                createTemplatesForRMCases();
            }
        } else {
            createTemplatesForRMCases();
        }
    }

    private void createTemplatesForModCases(WordWidth operandSizeAttribute) {
        _context = _context.clone();
        _context.setOperandSizeAttribute(operandSizeAttribute);

        if (_instructionAssessment.hasModRMByte()) {
            for (X86TemplateContext.ModCase modCase : X86TemplateContext.ModCase.VALUES) {
                _context = _context.clone();
                _context.setModCase(modCase);
                createTemplatesForModRMGroups();
            }
        } else {
            createTemplate();
        }
    }

    private void createTemplatesForOperandSizeAttribute(WordWidth addressSizeAttribute) {
        _context = _context.clone();
        _context.setAddressSizeAttribute(addressSizeAttribute);

        if (_instructionDescription.requiredOperandSize() != null) {
            createTemplatesForModCases(_instructionDescription.requiredOperandSize());
        } else {
            if (_instructionDescription.defaultOperandSize() != WordWidth.BITS_64) {
                createTemplatesForModCases(WordWidth.BITS_32);
            }
            if (_addressWidth == WordWidth.BITS_64) {
                createTemplatesForModCases(WordWidth.BITS_64);
            }
            if (X86Assembly.are16BitOffsetsSupported() || !_instructionAssessment.isJump()) {
                createTemplatesForModCases(WordWidth.BITS_16);
            }
        }
    }

    private void createTemplatesForAddressSizeAttribute() {
        if (_instructionDescription.requiredAddressSize() != null) {
            if (X86Assembly.are16BitAddressesSupported() || _instructionDescription.requiredAddressSize() == _addressWidth) {
                createTemplatesForOperandSizeAttribute(_instructionDescription.requiredAddressSize());
            }
        } else {
            createTemplatesForOperandSizeAttribute(_addressWidth);
            if (X86Assembly.are16BitAddressesSupported() && _instructionAssessment.hasAddressSizeVariants()) {
                createTemplatesForOperandSizeAttribute(WordWidth.fromInt(_addressWidth.numberOfBits() / 2));
            }
        }
    }

    public void createTemplates(InstructionDescriptionCreator<X86InstructionDescription> instructionDescriptionCreator) {
        for (X86InstructionDescription instructionDescription : instructionDescriptionCreator.instructionDescriptions()) {
            _instructionDescription = instructionDescription;
            _instructionAssessment = new InstructionAssessment();
            final OpcodeAssessor assessor = new OpcodeAssessor(_instructionAssessment);
            X86InstructionDescriptionVisitor.Static.visitInstructionDescription(assessor, _instructionDescription);
            _context = new X86TemplateContext();
            createTemplatesForAddressSizeAttribute();
        }
    }
}

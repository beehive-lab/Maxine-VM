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
package com.sun.max.asm.gen.risc;

import static com.sun.max.collect.SequenceBag.MapType.*;

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 *
 *
 * @author Bernd Mathiske
 */
public class RiscTemplateCreator {

    public RiscTemplateCreator() {
    }

    private AppendableSequence<RiscTemplate> templates = new LinkSequence<RiscTemplate>();

    public Sequence<RiscTemplate> templates() {
        return templates;
    }

    protected RiscTemplate createTemplate(InstructionDescription instructionDescription) {
        return new RiscTemplate(instructionDescription);
    }

    public Sequence<RiscTemplate> createOptionTemplates(Sequence<RiscTemplate> templateList, OptionField optionField) {
        final AppendableSequence<RiscTemplate> newTemplates = new LinkSequence<RiscTemplate>();
        for (RiscTemplate template : templateList) {
            RiscTemplate canonicalRepresentative = null;
            if (optionField.defaultOption() != null) {
                canonicalRepresentative = (RiscTemplate) template.clone();
                canonicalRepresentative.organizeOption(optionField.defaultOption(), null);
            }
            for (Option option : optionField.options()) {
                if (option.equals(optionField.defaultOption())) {
                    newTemplates.append(canonicalRepresentative);
                } else {
                    final RiscTemplate templateWithOption = (RiscTemplate) template.clone();
                    templateWithOption.organizeOption(option, canonicalRepresentative);
                    newTemplates.append(templateWithOption);
                }
            }
        }
        return newTemplates;
    }

    private int serial;
    private Bag<String, RiscTemplate, Sequence<RiscTemplate>> nameToTemplates = new SequenceBag<String, RiscTemplate>(HASHED);

    public Sequence<RiscTemplate> nameToTemplates(String name) {
        return nameToTemplates.get(name);
    }

    public void createTemplates(RiscInstructionDescriptionCreator instructionDescriptionCreator) {
        final AppendableSequence<RiscTemplate> initialTemplates = new LinkSequence<RiscTemplate>();
        for (InstructionDescription instructionDescription : instructionDescriptionCreator.instructionDescriptions()) {
            final RiscTemplate template = createTemplate(instructionDescription);
            initialTemplates.append(template);
            RiscInstructionDescriptionVisitor.Static.visitInstructionDescription(template, instructionDescription);
        }
        for (RiscTemplate initialTemplate : initialTemplates) {
            Sequence<RiscTemplate> newTemplates = new LinkSequence<RiscTemplate>(initialTemplate);
            for (OptionField optionField : initialTemplate.optionFields()) {
                newTemplates = createOptionTemplates(newTemplates, optionField);
            }
            for (RiscTemplate template : newTemplates) {
                serial++;
                template.setSerial(serial);
                templates.append(template);
                nameToTemplates.add(template.internalName(), template);

                // Create the link to the non-synthetic instruction from which a synthetic instruction is derived.
                if (template.instructionDescription().isSynthetic()) {
                    boolean found = false;
                    final Iterator<RiscTemplate> iterator = nameToTemplates.iterator();
                    while (iterator.hasNext() && !found) {
                        final RiscTemplate rawTemplate = iterator.next();
                        if (!rawTemplate.instructionDescription().isSynthetic() && (template.opcodeMask() & rawTemplate.opcodeMask()) == rawTemplate.opcodeMask() &&
                                        (template.opcode() & rawTemplate.opcodeMask()) == rawTemplate.opcode()) {
                            template.setSynthesizedFrom(rawTemplate);
                            found = true;
                        }
                    }
                    ProgramError.check(found);
                }
            }
        }
    }

}

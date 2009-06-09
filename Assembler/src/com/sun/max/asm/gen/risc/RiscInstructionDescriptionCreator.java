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

import java.util.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.asm.gen.risc.field.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class RiscInstructionDescriptionCreator extends InstructionDescriptionCreator<RiscInstructionDescription> {

    protected final RiscTemplateCreator<? extends RiscTemplate> _templateCreator;

    protected RiscInstructionDescriptionCreator(Assembly assembly, RiscTemplateCreator<? extends RiscTemplate> templateCreator) {
        super(assembly);
        _templateCreator = templateCreator;
    }

    @Override
    protected RiscInstructionDescription createInstructionDescription(MutableSequence<Object> specifications) {
        return new RiscInstructionDescription(specifications);
    }

    private int firstStringIndex(List<Object> specifications) {
        for (int i = 0; i < specifications.size(); i++) {
            if (specifications.get(i) instanceof String) {
                return i;
            }
        }
        throw ProgramError.unexpected("template instruction description without name");
    }

    private void setFirstString(List<Object> specifications, String value) {
        specifications.set(firstStringIndex(specifications), value);
    }

    private void eliminateConstraintFor(Parameter parameter, List<Object> specifications) {
        for (final Iterator iterator = specifications.iterator(); iterator.hasNext();) {
            final Object s = iterator.next();
            if (s instanceof InstructionConstraint) {
                final InstructionConstraint constraint = (InstructionConstraint) s;
                if (constraint.referencesParameter(parameter)) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean updateSpecifications(List<Object> specifications, Object pattern) {
        for (int i = 0; i < specifications.size(); i++) {
            final Object specification = specifications.get(i);
            if (specification.equals(pattern)) {
                specifications.set(i, pattern);
                return true;
            } else if (pattern instanceof RiscConstant && (specification instanceof OperandField || specification instanceof OptionField)) {
                final RiscConstant constant = (RiscConstant) pattern;
                final RiscField constantField = constant.field();
                final RiscField variableField = (RiscField) specification;
                if (variableField.equals(constantField)) {
                    specifications.set(i, pattern);
                    if (specification instanceof Parameter) {
                        eliminateConstraintFor((Parameter) specification, specifications);
                    }
                    return true;
                }
            } else if (pattern instanceof InstructionConstraint && !(pattern instanceof Parameter)) {
                specifications.add(pattern);
                return true;
            } else if (pattern instanceof RiscField) {
                if (((RiscField) pattern).bitRange() instanceof OmittedBitRange) {
                    specifications.add(pattern);
                    return true;
                }
            }
        }
        return false;
    }

    private RiscInstructionDescription createSyntheticInstructionDescription(String name, RiscTemplate template, Object[] patterns) {
        final List<Object> specifications = new ArrayListSequence<Object>(template.instructionDescription().specifications());
        for (Object pattern : patterns) {
            if (!updateSpecifications(specifications, pattern)) {
                // InstructionDescription with the same name, but different specifications, skip it:
                Trace.line(3, name + " not updated with " + pattern + " in " + specifications);
                return null;
            }
        }
        setFirstString(specifications, name);
        final Class<MutableSequence<Object>> type = null;
        return (RiscInstructionDescription) defineInstructionDescription(StaticLoophole.cast(type, specifications)).beSynthetic();
    }

    /**
     * Creates a synthetic instruction from a previously defined (raw or synthetic) instruction
     * by replacing one or more parameters of the instruction with a constant or alternative parameter.
     * 
     * @param name          the internal (base) name of the new synthetic instruction
     * @param templateName  the internal name of the original instruction on which the synthetic instruction is based
     * @param patterns      the replacements for one or more parameters of the original instruction
     * @return the newly created instruction descriptions resulting from the substitution wrapped in a RiscInstructionDescriptionModifier
     */
    protected RiscInstructionDescriptionModifier synthesize(String name, String templateName, Object... patterns) {
        final AppendableSequence<RiscInstructionDescription> instructionDescriptions = new ArrayListSequence<RiscInstructionDescription>();
        // Creating a new VariableSequence here prevents iterator comodification below:
        final Sequence<? extends RiscTemplate> nameTemplates = _templateCreator.nameToTemplates(templateName);
        if (!nameTemplates.isEmpty()) {
            final Sequence<RiscTemplate> templates = new ArrayListSequence<RiscTemplate>(nameTemplates);
            assert !templates.isEmpty();
            for (RiscTemplate template : templates) {
                final RiscInstructionDescription instructionDescription = createSyntheticInstructionDescription(name, template, patterns);
                if (instructionDescription != null) {
                    instructionDescriptions.append(instructionDescription);
                }
            }
        }
        ProgramError.check(!instructionDescriptions.isEmpty());
        return new RiscInstructionDescriptionModifier(instructionDescriptions);
    }
}

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
package com.sun.max.vm.template;

import static com.sun.max.vm.stack.JavaStackFrameLayout.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.template.generate.*;
import com.sun.max.vm.type.*;

/**
 * Template table for JIT Compiler and Interpreter generator.
 * JIT and Interpreter generator initialize the template table with a set of bytecode
 * template sources.
 * A bytecode template source is a class annotated with the TEMPLATE annotation, and
 * with parameter-less methods that each provide an implementation of a bytecode template.
 * Each such method may provide additional criteria via a BYTECODE_TEMPLATE annotation.
 *
 * Bytecode template sources must be annotated with the TEMPLATE annotation.
 *
 * Users of the template table can select a template with a selector, a kind, and a
 * boolean instrumented flag.
 * The values defaults to Selector=NO_ASSUMPTION, Kind=VOID, Instrumented=NO, Traced=NO.
 *
 * @see com.sun.max.vm.bytecode
 * @see  com.sun.max.vm.type.Kind
 * @see com.sun.max.vm.template.TemplateChooser
 * @see com.sun.max.annotate.TEMPLATE
 * @see com.sun.max.annotate.BYTECODE_TEMPLATE
 *
 * @author Laurent Daynes
 * @author Aziz Ghuloum
 */
public class TemplateTable {
    private final int _maxFrameSlots;

    public int maxFrameSlots() {
        return _maxFrameSlots;
    }

    private final TemplateChooser[] _templateChoosers;

    public TemplateTable(Class... templateSources) {
        _templateChoosers = new TemplateChooser[Bytecode.BREAKPOINT.ordinal() + 1];

        final BytecodeTemplateGenerator templateGenerator = new BytecodeTemplateGenerator();

        final VariableSequence<CompiledBytecodeTemplate> templateSequence = new ArrayListSequence<CompiledBytecodeTemplate>();
        for (Class <?> templateSource : templateSources) {
            assert templateSource.isAnnotationPresent(TEMPLATE.class);
            templateGenerator.generateBytecodeTemplates(templateSource, templateSequence);
        }

        _maxFrameSlots = Ints.roundUp(templateGenerator.maxTemplateFrameSize(), STACK_SLOT_SIZE) / STACK_SLOT_SIZE;

        for (CompiledBytecodeTemplate template : templateSequence) {
            final Bytecode bytecode = template.bytecode();
            final int index = bytecode.ordinal();
            final TemplateChooser tc = _templateChoosers[index];
            if (tc == null) {
                _templateChoosers[index] = new SingleChoiceTemplateChooser(template);
            } else {
                _templateChoosers[index] = tc.extended(template);
            }
        }
    }

    public CompiledBytecodeTemplate get(Bytecode bytecode) {
        return get(bytecode, Kind.VOID, TemplateChooser.Selector.NO_ASSUMPTION);
    }

    public CompiledBytecodeTemplate get(Bytecode bytecode, Kind kind) {
        return get(bytecode, kind, TemplateChooser.Selector.NO_ASSUMPTION);
    }

    public CompiledBytecodeTemplate get(Bytecode bytecode, TemplateChooser.Selector selector) {
        return get(bytecode, Kind.VOID, selector);
    }

    public CompiledBytecodeTemplate get(Bytecode bytecode, Kind kind, TemplateChooser.Selector selector) {
        final TemplateChooser templateChooser = _templateChoosers[bytecode.ordinal()];
        assert templateChooser != null : "no template chooser for " + bytecode;
        final CompiledBytecodeTemplate template = templateChooser.select(kind, selector);
        assert template != null : "attempt to access non existent template for bytecode " + bytecode + " with selector " + selector.toString();
        return template;
    }

    static class SingleChoiceTemplateChooser extends TemplateChooser {
        final CompiledBytecodeTemplate _template;
        SingleChoiceTemplateChooser(CompiledBytecodeTemplate template) {
            _template = template;
        }

        @Override
        public CompiledBytecodeTemplate select(Kind kind, Selector selector) {
            if (selector.initialized() == _template.initialized() &&
                selector.instrumented() == _template.instrumented() &&
                selector.resolved() == _template.resolved() &&
                selector.traced() == _template.traced() &&
                kind == _template.kind()) {
                return _template;
            }
            return null;
        }

        @Override
        public TemplateChooser extended(CompiledBytecodeTemplate template) {
            final MultipleChoiceTemplateChooser tc = new MultipleChoiceTemplateChooser();
            return tc.extended(template).extended(_template);
        }
    }

    static class MultipleChoiceTemplateChooser extends TemplateChooser {
        final CompiledBytecodeTemplate[][][][][] _templates;

        public MultipleChoiceTemplateChooser() {
            _templates = new CompiledBytecodeTemplate
              [KindEnum.VALUES.length()]
               [TemplateChooser.Instrumented.DEFAULT.ordinal()]
                [TemplateChooser.Resolved.DEFAULT.ordinal()]
                 [TemplateChooser.Initialized.DEFAULT.ordinal()]
                  [TemplateChooser.Traced.DEFAULT.ordinal()];
        }

        @Override
        public TemplateChooser extended(CompiledBytecodeTemplate template) {
            final int kindIndex = template.kind().asEnum().ordinal();
            final int instrumentIndex = template.instrumented().ordinal();
            final int resolvedIndex = template.resolved().ordinal();
            final int initializedIndex = template.initialized().ordinal();
            final int tracedIndex = template.traced().ordinal();

            // We need to override templates with tracing versions, therefore this assertion is bogus.
            // assert _templates[kindIndex][instrumentIndex][resolvedIndex][initializedIndex][tracedIndex] == null : "attempt to override template";
            _templates[kindIndex][instrumentIndex][resolvedIndex][initializedIndex][tracedIndex] = template;
            return this;
        }

        @Override
        public CompiledBytecodeTemplate select(Kind kind, Selector selector) {
            final int kindIndex = kind.asEnum().ordinal();
            final int instrumentIndex = selector.instrumented().ordinal();
            final int resolvedIndex = selector.resolved().ordinal();
            final int initializedIndex = selector.initialized().ordinal();
            final int tracedIndex = selector.traced().ordinal();
            final CompiledBytecodeTemplate template = _templates[kindIndex][instrumentIndex][resolvedIndex][initializedIndex][tracedIndex];
            return template;
        }
    }

}

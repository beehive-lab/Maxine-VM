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
package com.sun.max.asm.gen.risc.field;

import java.lang.reflect.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;

/**
 * A field that contains an immediate value.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public class ImmediateOperandField extends OperandField<ImmediateArgument> implements ImmediateParameter, WrappableSpecification, InstructionConstraint {

    public String asJavaExpression() {
        final String value = valueString();
        return minArgumentValue() + " <= " + value + " && " + value + " <= " + maxArgumentValue();
    }

    public boolean check(Template template, IndexedSequence<Argument> arguments) {
        final long value = evaluate(template, arguments);
        return minArgumentValue() <= value && value <= maxArgumentValue();
    }

    public Method predicateMethod() {
        return null;
    }

    public boolean referencesParameter(Parameter parameter) {
        return parameter == this;
    }

    public ImmediateOperandField(BitRange bitRange) {
        super(bitRange);
    }

    public static ImmediateOperandField create(BitRangeOrder order, int... bits) {
        final BitRange bitRange = BitRange.create(bits, order);
        return new ImmediateOperandField(bitRange);
    }

    public static ImmediateOperandField createDescending(int firstBitIndex, int lastBitIndex) {
        return new ImmediateOperandField(new DescendingBitRange(firstBitIndex, lastBitIndex));
    }

    public static ImmediateOperandField createAscending(int firstBitIndex, int lastBitIndex) {
        return new ImmediateOperandField(new AscendingBitRange(firstBitIndex, lastBitIndex));
    }

    public static ImmediateOperandField createAscending(int... bits) {
        return create(BitRangeOrder.ASCENDING, bits);
    }

    @Override
    public Class type() {
        return int.class;
    }

    public String valueString() {
        if (boundTo() != null) {
            return boundTo().valueString();
        }
        return variableName();
    }

    @Override
    public ImmediateOperandField beSigned() {
        return (ImmediateOperandField) super.beSigned();
    }

    @Override
    public ImmediateOperandField beSignedOrUnsigned() {
        return (ImmediateOperandField) super.beSignedOrUnsigned();
    }

    @Override
    public Immediate32Argument disassemble(int instruction) {
        return new Immediate32Argument(extract(instruction));
    }

    @Override
    public ImmediateOperandField setVariableName(String name) {
        super.setVariableName(name);
        return this;
    }

    private ArgumentRange argumentRange;

    public ArgumentRange argumentRange() {
        if (argumentRange == null) {
            argumentRange = new ArgumentRange(this, minArgumentValue(), maxArgumentValue());
        }
        return argumentRange;
    }

    private Iterable<? extends Argument> testArguments;
    private Iterable<? extends Argument> illegalTestArguments;

    private static final MapFunction<Integer, Immediate32Argument> ARGUMENT_WRAPPER = new MapFunction<Integer, Immediate32Argument>() {
        public Immediate32Argument map(Integer integer) {
            return new Immediate32Argument(integer);
        }
    };

    public Iterable<? extends Argument> getLegalTestArguments() {
        if (testArguments == null) {
            final Sequence<Integer> integers = signDependentOperations().legalTestArgumentValues(minArgumentValue(), maxArgumentValue(), grain());
            testArguments = LinkSequence.map(integers, Immediate32Argument.class, ARGUMENT_WRAPPER);
        }
        return testArguments;
    }

    public Iterable<? extends Argument> getIllegalTestArguments() {
        if (this.illegalTestArguments == null) {
            final AppendableSequence<Immediate32Argument> illegalArguments = new LinkSequence<Immediate32Argument>();
            final int min = minArgumentValue();
            if (min != Integer.MIN_VALUE) {
                illegalArguments.append(new Immediate32Argument(min - 1));
                illegalArguments.append(new Immediate32Argument(Integer.MIN_VALUE));
            }
            final int max = maxArgumentValue();
            if (max != Integer.MAX_VALUE) {
                illegalArguments.append(new Immediate32Argument(max + 1));
                illegalArguments.append(new Immediate32Argument(Integer.MAX_VALUE));
            }
            this.illegalTestArguments = illegalArguments;
        }
        return illegalTestArguments;
    }

    public TestArgumentExclusion excludeExternalTestArguments(Argument... arguments) {
        return new TestArgumentExclusion(AssemblyTestComponent.EXTERNAL_ASSEMBLER, this, Sets.from(arguments));
    }

    @Override
    public ImmediateOperandField bindTo(Expression expression) {
        return (ImmediateOperandField) super.bindTo(expression);
    }
}

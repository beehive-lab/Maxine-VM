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

import java.util.*;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;

/**
 * An operand field defines an instruction field whose value is given as a parameter in the generated
 * assembler method. The field is also a parameter in the external assembler syntax unless
 * it's {@link #_type value type} implements {@link ExternalMnemonicSuffixArgument} in which
 * case, the field's value is represented as a suffix of the mnemonic in the external assembler syntax.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Dave Ungar
 * @author Adam Spitz
 */
public abstract class OperandField<Argument_Type extends Argument> extends RiscField implements Parameter, Expression  {

    private SignDependentOperations _signDependentOperations;

    protected OperandField(BitRange bitRange) {
        super(bitRange);
        _signDependentOperations = SignDependentOperations.UNSIGNED;
    }

    public RiscConstant constant(int value) {
        return new RiscConstant(new ConstantField(name(), bitRange()), value);
    }

    protected SignDependentOperations signDependentOperations() {
        return _signDependentOperations;
    }

    protected void setSignDependentOperations(SignDependentOperations signDependentOperations) {
        _signDependentOperations = signDependentOperations;
    }

    public int maxArgumentValue() {
        return _signDependentOperations.maxArgumentValue(bitRange());
    }

    public int minArgumentValue() {
        return _signDependentOperations.minArgumentValue(bitRange());
    }

    public int assemble(int value) throws AssemblyException {
        return _signDependentOperations.assemble(value, bitRange());
    }

    public int extract(int instruction) {
        return _signDependentOperations.extract(instruction, bitRange());
    }

    public abstract Argument_Type disassemble(int instruction);

    /**
     * @return the minimal difference between any two potential operands
     */
    public int grain() {
        return 1 << zeroes();
    }

    /**
     * @return implied zeroes to be "appended" to respective operands
     */
    public int zeroes() {
        return 0;
    }

    @Override
    public OperandField<Argument_Type> clone() {
        final Class<OperandField<Argument_Type>> type = null;
        return StaticLoophole.cast(type, super.clone());
    }

    public OperandField<Argument_Type> beSigned() {
        final OperandField<Argument_Type> result = clone();
        result.setSignDependentOperations(SignDependentOperations.SIGNED);
        return result;
    }

    public OperandField<Argument_Type> beSignedOrUnsigned() {
        final OperandField<Argument_Type> result = clone();
        result.setSignDependentOperations(SignDependentOperations.SIGNED_OR_UNSIGNED);
        return result;
    }

    public boolean isSigned() {
        return _signDependentOperations == SignDependentOperations.SIGNED;
    }

    public abstract Class type();

    private String _variableName;

    public String variableName() {
        if (_variableName != null) {
            return _variableName;
        }
        return name();
    }

    public Argument getExampleArgument() {
        final Iterator<? extends Argument> it = getLegalTestArguments().iterator();
        if (it.hasNext()) {
            return it.next();
        }
        return null;
    }

    public OperandField<Argument_Type> setVariableName(String name) {
        _variableName = name;
        return this;
    }

    public String externalName() {
        return variableName();
    }

    private Set<Argument> _excludedDisassemblerTestArguments = Sets.empty(Argument.class);

    public OperandField<Argument_Type> withExcludedDisassemblerTestArguments(Set<Argument> arguments) {
        final OperandField<Argument_Type> result = clone();
        result._excludedDisassemblerTestArguments = arguments;
        return result;
    }

    public OperandField<Argument_Type> withExcludedDisassemblerTestArguments(Argument... arguments) {
        return withExcludedDisassemblerTestArguments(Sets.from(arguments));
    }

    public Set<Argument> excludedDisassemblerTestArguments() {
        return _excludedDisassemblerTestArguments;
    }

    private Set<Argument> _excludedExternalTestArguments = Sets.empty(Argument.class);

    public OperandField<Argument_Type> withExcludedExternalTestArguments(Set<Argument> arguments) {
        final OperandField<Argument_Type> result = clone();
        result._excludedExternalTestArguments = arguments;
        return result;
    }

    public OperandField<Argument_Type> withExcludedExternalTestArguments(Argument... arguments) {
        return withExcludedExternalTestArguments(Sets.from(arguments));
    }

    public Set<Argument> excludedExternalTestArguments() {
        return _excludedExternalTestArguments;
    }

    public int compareTo(Parameter other) {
        return type().getName().compareTo(other.type().getName());
    }

    public long evaluate(Template template, IndexedSequence<Argument> arguments) {
        if (boundTo() != null) {
            return boundTo().evaluate(template, arguments);
        }
        return template.bindingFor(this, arguments).asLong();
    }

    private Expression _expression;

    public OperandField<Argument_Type> bindTo(Expression expression) {
        final OperandField<Argument_Type> result = clone();
        result._expression = expression;
        return result;
    }

    public Expression boundTo() {
        return _expression;
    }
}

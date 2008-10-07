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
/*VCSID=847ce505-176e-4498-95f9-ce2bd65621e4*/
package com.sun.max.asm.gen.risc.field;

import com.sun.max.asm.*;
import com.sun.max.asm.gen.risc.*;
import com.sun.max.asm.gen.risc.bitRange.*;
import com.sun.max.collect.*;
import com.sun.max.program.*;

/**
 * An OptionField is a field whose value is specified as an optional part of the assembler
 * mnemonic or assembler method name. The field has a default value if it is not specified.
 * An example of an optional field is the {@link com.sun.max.asm.gen.risc.sparc.SPARCFields#_p_option
 * predict bit} for the SPARC Branch on Equal with Prediction instruction:
 * 
 *     bge        // predict that branch will be taken (default)
 *     bge,pt     // predict that branch will be taken
 *     bge,pn     // predict that branch will not be taken
 * 
 * The definition of this field therefore has three {@link Option options}.
 * 
 * @author Dave Ungar
 * @author Bernd Mathiske
 * @author Adam Spitz
 * @author Doug Simon
 * @author Sumeet Panchal
 */
public class OptionField extends RiscField {

    public OptionField(BitRange bitRange) {
        super(bitRange);
    }

    public static OptionField createAscending(int... bits) {
        final BitRange bitRange = BitRange.create(bits, BitRangeOrder.ASCENDING);
        return new OptionField(bitRange);
    }

    public static OptionField createDescending(int... bits) {
        final BitRange bitRange = BitRange.create(bits, BitRangeOrder.DESCENDING);
        return new OptionField(bitRange);
    }

    public RiscConstant constant(int value) {
        return new RiscConstant(this, value);
    }

    protected Option _defaultOption;

    public Option defaultOption() {
        return _defaultOption;
    }

    protected AppendableSequence<Option> _options = new LinkSequence<Option>();

    public Iterable<Option> options() {
        return _options;
    }

    @Override
    public OptionField clone() {
        final OptionField result = (OptionField) super.clone();
        result._options = new LinkSequence<Option>(_options);
        return result;
    }

    /**
     * Creates a copy of this field that can take an additional value.
     * 
     * @param name   addition to the assembler method's name used to specify the option value
     * @param value  the option value
     * @param externalName addition to the external assembler syntax used to specify the option value
     * @return the extended field
     */
    public OptionField withOption(String name, int value, String externalName) {
        final OptionField result = clone();
        final Option newOption = new Option(name, value, externalName, result);
        for (Option option : _options) {
            if (option.equals(newOption)) {
                ProgramError.unexpected("duplicate option: " + option);
            }
        }
        result._options.append(newOption);

        if (name.equals("")) {
            result._defaultOption = newOption;
        }
        return result;
    }

    /**
     * Creates a copy of this field that can take an additional value.
     * 
     * @param name   addition to the assembler method's name used to specify the option value
     * @param value  the option value
     * @return the extended field
     */
    public OptionField withOption(String name, int value) {
        return withOption(name, value, name);
    }

    /**
     * Creates a copy of this field that can take an additional value.
     * 
     * @param value  the option value
     * @return the extended field
     */
    public OptionField withOption(int value) {
        return withOption("", value);
    }

    /**
     * Creates a copy of this field that can take an additional value.
     * 
     * @param name       addition to the assembler method's name used to specify the option value
     * @param argument   the option value represented as a symbol
     * @return the extended field
     */
    public OptionField withOption(String name, SymbolicArgument argument) {
        if (argument instanceof ExternalMnemonicSuffixArgument) {
            return withOption(name, argument.value(), argument.externalValue());
        }
        return withOption(name, argument.value());
    }
}

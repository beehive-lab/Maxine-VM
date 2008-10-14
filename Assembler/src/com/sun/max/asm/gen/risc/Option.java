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

import com.sun.max.asm.gen.risc.field.*;

/**
 * An Option is one of the values that an {@link OptionField optional field} can take.
 * An example is the value of the <i>predict bit</i> for the SPARC Branch on Equal with
 * Prediction instruction that specifies the bit is set. The format of this instruction is:
 * 
 *    bge_pt(...)  // assembler method
 *    bge,pt ...   // external assembler syntax
 * 
 * @author Dave Ungar
 * @author Adam Spitz
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class Option {

    /**
     * The addition to the assembler method's name used to specify this option value.
     */
    protected final String _name;

    /**
     * The addition to the external assembler syntax used to specify this option value.
     */
    protected final String _externalName;

    /**
     * The value of the option.
     */
    protected final int _value;

    /**
     * The field to which this option applies.
     */
    private final OptionField _field;

    public String name() {
        return _name;
    }

    public String externalName() {
        return _externalName;
    }

    public int value() {
        return _value;
    }

    public Option() {
        this("");
    }

    public Option(String name) {
        this(name, 0);
    }

    public Option(String name, int value) {
        this(name, value, "");
    }

    public Option(String name, int value, String externalName) {
        this(name, value, externalName, null);
    }

    public Option(String name, int value, String externalName, OptionField field) {
        _name = name;
        _value = value;
        _externalName = externalName;
        _field = field;
    }

    public OptionField field() {
        return _field;
    }

    public boolean isRedundant() {
        return (_field.defaultOption() != null) && (_value == _field.defaultOption()._value) && (!(equals(_field.defaultOption())));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Option && _name.equals(((Option) other)._name);
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }
}

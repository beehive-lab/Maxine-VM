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
package com.sun.max.program.option;

import com.sun.max.lang.*;

/**
 * The {@code Option} class represents a command-line or other configuration
 * option with a particular name, type, and description.
 *
 * @author Ben L. Titzer
 */
public class Option<Value_Type> implements Cloneable {

    /**
     * The {@code Option.Type} class represents a type for an option. This class
     * implements method for parsing and unparsing values from strings.
     */
    public abstract static class Type<Value_Type> {
        protected final String _typeName;
        public final Class<Value_Type> _type;

        protected Type(Class<Value_Type> type, String typeName) {
            _typeName = typeName;
            _type = type;
        }

        public String getTypeName() {
            return _typeName;
        }

        public String unparseValue(Value_Type value) {
            return String.valueOf(value);
        }

        public abstract Value_Type parseValue(String string) throws Option.Error;

        public abstract String getValueFormat();

        public Option<Value_Type> cast(Option option) {
            return StaticLoophole.cast(option);
        }
    }

    public static class Error extends java.lang.Error {

        public Error(String message) {
            super(message);
        }
    }

    protected final String _name;
    protected Value_Type _defaultValue;
    protected final Type<Value_Type> _type;
    protected final String _help;
    protected Value_Type _value;

    /**
     * The constructor for the {@code Option} class creates constructs a new
     * option with the specified parameters.
     *
     * @param name     the name of the option as a string
     * @param defaultValue the default value of the option
     * @param type     the type of the option, which is used for parsing and unparsing values
     * @param help     a help description which is usually used to generate a formatted
     *                 help output
     */
    public Option(String name, Value_Type defaultValue, Type<Value_Type> type, String help) {
        _defaultValue = defaultValue;
        _name = name;
        _type = type;
        _help = help;
        _value = null;
    }

    /**
     * The {@code getName()} method returns the name of this option as a string.
     *
     * @return the name of this option
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the default value for this option,
     * which is the value that the option retains if no assignment is made.
     *
     * @param value the default value of the option
     */
    public void setDefaultValue(Value_Type value) {
        _defaultValue = value;
    }

    /**
     * The {@code getDefaultValue()} method returns the default value for this option,
     * which is the value that the option retains if no assignment is made.
     *
     * @return the default value of the option
     */
    public Value_Type getDefaultValue() {
        return _defaultValue;
    }

    /**
     * The {@code getValue()} method retrieves the current value of this option.
     *
     * @return the current value of this option
     */
    public Value_Type getValue() {
        return _value == null ? _defaultValue : _value;
    }

    /**
     * The {@code setValue()) method sets the value of this option.
     *
     * @param value the new value to this option
     */
    public void setValue(Value_Type value) {
        _value = value;
    }

    /**
     * The {@code setValue()} method sets the value of this option, given a string value.
     * The type of this option is used to determine how to parse the string into a value
     * of the appropriate type. Thus this method may potentially throw runtime exceptions
     * if parsing fails.
     *
     * @param string the new value of this option as a string
     */
    public void setString(String string) {
        setValue(_type.parseValue(string));
    }

    /**
     * The {@code getType()} method returns the type of this option.
     * @return the type of this option.
     */
    public Type<Value_Type> getType() {
        return _type;
    }

    /**
     * The {@code getString()} method retrieves the value of this option as a string.
     * The type of this option is used to determine how to unparse the value into a string.
     *
     * @return the value of this option as a string
     */
    public String getString() {
        return _type.unparseValue(getValue());
    }

    public String getHelp() {
        return _help;
    }

    @Override
    public String toString() {
        return getName();
    }
}

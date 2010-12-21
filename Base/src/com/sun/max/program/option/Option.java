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

import com.sun.max.*;

/**
 * The {@code Option} class represents a command-line or other configuration
 * option with a particular name, type, and description.
 *
 * @author Ben L. Titzer
 */
public class Option<T> implements Cloneable {

    /**
     * The {@code Option.Type} class represents a type for an option. This class
     * implements method for parsing and unparsing values from strings.
     */
    public abstract static class Type<T> {
        protected final String typeName;
        public final Class<T> type;

        protected Type(Class<T> type, String typeName) {
            this.typeName = typeName;
            this.type = type;
        }

        public String getTypeName() {
            return typeName;
        }

        public String unparseValue(T value) {
            return String.valueOf(value);
        }

        public abstract T parseValue(String string) throws Option.Error;

        public abstract String getValueFormat();

        public Option<T> cast(Option option) {
            return Utils.cast(option);
        }
    }

    public static class Error extends java.lang.Error {

        public Error(String message) {
            super(message);
        }
    }

    protected final String name;
    protected T defaultValue;
    protected final Type<T> type;
    protected final String help;
    protected T value;

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
    public Option(String name, T defaultValue, Type<T> type, String help) {
        this.defaultValue = defaultValue;
        this.name = name;
        this.type = type;
        this.help = help;
        value = null;
    }

    /**
     * The {@code getName()} method returns the name of this option as a string.
     *
     * @return the name of this option
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the default value for this option,
     * which is the value that the option retains if no assignment is made.
     *
     * @param val the default value of the option
     */
    public void setDefaultValue(T val) {
        defaultValue = val;
    }

    /**
     * The {@code getDefaultValue()} method returns the default value for this option,
     * which is the value that the option retains if no assignment is made.
     *
     * @return the default value of the option
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * The {@code getValue()} method retrieves the current value of this option.
     *
     * @return the current value of this option
     */
    public T getValue() {
        return !assigned ? defaultValue : value;
    }

    private boolean assigned;

    /**
     * The {@code setValue()) method sets the value of this option.
     *
     * @param value the new value to this option
     */
    public void setValue(T value) {
        assigned = true;
        this.value = value;
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
        setValue(type.parseValue(string));
    }

    /**
     * The {@code getType()} method returns the type of this option.
     * @return the type of this option.
     */
    public Type<T> getType() {
        return type;
    }

    /**
     * The {@code getString()} method retrieves the value of this option as a string.
     * The type of this option is used to determine how to unparse the value into a string.
     *
     * @return the value of this option as a string
     */
    public String getString() {
        return type.unparseValue(getValue());
    }

    public String getHelp() {
        return help;
    }

    @Override
    public String toString() {
        return getName();
    }
}

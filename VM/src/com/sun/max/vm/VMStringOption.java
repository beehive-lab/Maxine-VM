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
package com.sun.max.vm;

import com.sun.max.unsafe.*;

/**
 * A VM option that supports string values passed to parameters, including options
 * that consume the next argument. Note that options that consume the next argument
 * should be parsed in the {@code PRISTINE} phase.
 *
 * @author Ben L. Titzer
 */
public class VMStringOption extends VMOption {
    protected String _value;
    protected Pointer _cstring = Pointer.zero();
    protected final boolean _space;
    protected boolean _allocated;

    /**
     * Creates a new string option with the specified parameters and adds it to the appropriate VM option parsing phase.
     *
     * @param prefix the name of the option including the leading '-' character
     * @param space a boolean indicating whether this option consumes the next parameter ({@code true}) or not ({@code
     *            false})
     * @param defaultValue the default string value of this option
     * @param help the help text for this option
     * @param phase the option phase in which to parse this option
     */
    public VMStringOption(String prefix, boolean space, String defaultValue, String help, MaxineVM.Phase phase) {
        super(prefix, help, phase);
        _value = defaultValue;
        _space = space;
        if (space) {
            assert phase == MaxineVM.Phase.PRISTINE;
        }
    }

    /**
     * Parses the value of the string parameter from the command line. Note that this method will be called directly
     * with the next argument when the option expects to consume the next argument; otherwise it will be called
     * indirectly through {@code parse()} as normal.
     *
     * @param optionValue a pointer to a C-style string that gives the option's value
     */
    @Override
    public boolean parseValue(Pointer optionValue) {
        _cstring = optionValue;
        return true;
    }

    /**
     * Gets the value of this option as a string. Note that parsing a command line option typically does not build the
     * string; the string is built upon the first call to this method. This is necessary for certain bootstrap-sensitive
     * situations.
     *
     * @return the string value of this option
     */
    public String getValue() {
        if (!_cstring.isZero() && !_allocated) {
            _allocated = true;
            _value = new String(CString.toByteArray(_cstring, CString.length(_cstring).toInt()));
        }
        return _value;
    }

    /**
     * Print the help text for this option.
     */
    @Override
    public void printHelp() {
        if (_space) {
            VMOptions.printHelpForOption(_prefix, " <value>", _help);
        } else {
            VMOptions.printHelpForOption(_prefix, "<value>", _help);
        }
    }

    /**
     * Returns {@code true} if this option expects to consume the next argument.
     */
    @Override
    public boolean consumesNext() {
        return _space;
    }

    /**
     * Returns {@code true} if this option was specified on the command line.
     */
    @Override
    public boolean isPresent() {
        return !_cstring.isZero();
    }
}

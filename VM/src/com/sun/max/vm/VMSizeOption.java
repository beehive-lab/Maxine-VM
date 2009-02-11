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

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A VM command line option that can accept a size (i.e. a number that has an associated kilobyte,
 * megabyte, or gigabyte specifier).
 *
 * @author Ben L. Titzer
 */
public class VMSizeOption extends VMOption {
    protected Size _value;

    /**
     * Creates a new size option with the specified values and adds this option to the command line
     * arguments.
     *
     * @param prefix the name of the option, including the leading '-' character
     * @param defaultValue the default size for this option when it is not present on the command line
     * @param help the help text to report for this option
     * @param phase the phase in which this option should be parsed
     */
    @PROTOTYPE_ONLY
    public VMSizeOption(String prefix, Size defaultValue, String help, MaxineVM.Phase phase) {
        super(prefix, help, phase);
        _value = defaultValue;
    }

    /**
     * Parse the value from a C-style string. This method accepts positive integers that may be suffixed with zero or
     * one of the 'K', 'k', 'M', 'm', 'G', or 'g' characters, which denote kilobytes, megabytes, and gigabytes,
     * respectively.
     *
     * @param optionValue a pointer to the beginning of the C-style string representing the value
     * @return {@code true} if the value was parsed successfully; {@code false} otherwise
     */
    @Override
    public boolean parseValue(Pointer optionValue) {
        _value = Size.fromLong(VMOptions.parseScaledValue(optionValue, CString.length(optionValue), 0));
        if (_value.lessThan(0)) {
            return false;
        }
        return true;
    }

    /**
     * Print the help text on the console.
     */
    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(_prefix, "<size>", _help);
    }

    /**
     * Gets the size value of this option--either the default, or the value specified on the command line.
     * @return the size of this option
     */
    public Size getValue() {
        return _value;
    }
}

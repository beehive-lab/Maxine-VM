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

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.*;

/**
 * A VM option that represents an integer, such as a tuning parameter or other
 * internal VM configuration.
 *
 * @author Ben L. Titzer
 */
public class VMIntOption extends VMOption {
    protected int value;

    /**
     * Creates a new integer option.
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option
     * in the global registry or VM options.</b>
     *
     * @param prefix the name of the option, including the leading '-' character
     * @param defaultValue the default value of the option when it is not specified
     * @param help the help text for the option
     */
    public VMIntOption(String prefix, int defaultValue, String help) {
        super(prefix, appendDefaultValue(help, defaultValue < 0 ? String.valueOf(defaultValue) : Ints.toUnitsString(defaultValue, true)));
        value = defaultValue;
    }

    /**
     * Parses a C-style string to produce an integer value for this option.
     * @param optionValue a pointer to the C-style string which contains the value
     * @return {@code true} if the value was parsed successfully; {@code false} otherwise
     */
    @Override
    public boolean parseValue(Pointer optionValue) {
        value = CString.parseUnsignedInt(optionValue);
        // TODO: deal with negative numbers.
        return value >= 0;
    }

    /**
     * Print the command-line help for this option.
     */
    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, "<n>", help);
    }

    /**
     * Gets the value of this option as an {@code int}.
     * @return the value of this option
     */
    public int getValue() {
        return value;
    }
}

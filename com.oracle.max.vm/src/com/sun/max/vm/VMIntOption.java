/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.Phase;

/**
 * A VM option that represents an integer, such as a tuning parameter or other
 * internal VM configuration.
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
        value = CString.parseInt(optionValue);
        return !CString.parseError;
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

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

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.*;

/**
 * A VM command line option that can accept a size (i.e. a number that has an associated kilobyte,
 * megabyte, or gigabyte specifier).
 */
public class VMSizeOption extends VMOption {
    protected Size value;

    /**
     * Creates a new size option with the specified values and adds this option to the command line
     * arguments.
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option
     * in the global registry or VM options.</b>
     *
     * @param prefix the name of the option, including the leading '-' character
     * @param defaultValue the default size for this option when it is not present on the command line
     * @param help the help text to report for this option
     */
    @HOSTED_ONLY
    public VMSizeOption(String prefix, Size defaultValue, String help) {
        super(prefix, appendDefaultValue(help, Longs.toUnitsString(defaultValue.toLong(), true)));
        value = defaultValue;
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
        long value = CString.parseScaledValue(optionValue, CString.length(optionValue), 0);
        if (value < 0) {
            return false;
        }
        this.value = Size.fromLong(value);
        return true;
    }

    /**
     * Print the help text on the console.
     */
    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, "<size>", help);
    }

    /**
     * Gets the size value of this option--either the default, or the value specified on the command line.
     * @return the size of this option
     */
    public Size getValue() {
        return value;
    }
}

/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.unsafe.*;

/**
 * A VM option that represents a float.
 */
public class VMFloatOption extends VMOption {
    protected float value;

    /**
     * Creates a new float option.
     *
     * <b>The caller is responsible for registering this option in the global registry or VM options.</b>
     *
     * @param prefix the name of the option, including the leading '-' character
     * @param defaultValue the default value of the option when it is not specified
     * @param help the help text for the option
     */
    @HOSTED_ONLY
    public VMFloatOption(String prefix, float defaultValue, String help) {
        super(prefix, appendDefaultValue(help, String.valueOf(defaultValue)));
        value = defaultValue;
    }

    /**
     * Parses a C-style string to produce a float value for this option.
     *
     * @param optionValue a pointer to the C-style string which contains the value
     * @return {@code true} if the value was parsed successfully; {@code false} otherwise
     */
    @Override
    public boolean parseValue(Pointer optionValue) {
        value = CString.parseFloat(optionValue);
        if (Float.isNaN(value)) {
            return false;
        }
        return true;
    }

    /**
     * Print the command-line help for this option.
     */
    @Override
    public void printHelp() {
        VMOptions.printHelpForOption(category(), prefix, "<n>", help);
    }

    /**
     * Gets the value of this option as an {@code float}.
     * @return the value of this option
     */
    public float getValue() {
        return value;
    }
}

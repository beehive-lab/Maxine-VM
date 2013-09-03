/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.util.*;

/**
 * A VM option that supports string values passed to parameters, including options
 * that consume the next argument. Note that options that consume the next argument
 * should be parsed in the {@code PRISTINE} phase.
 */
public class VMStringOption extends VMOption {
    protected String value;
    protected final boolean space;
    @RESET
    protected Pointer cstring = Pointer.zero();
    @RESET
    protected boolean allocated;

    /**
     * Creates a new string option with the specified parameters.
     *
     * <b>The caller is responsible for registering this option in the global registry or VM options.</b>
     *
     * @param prefix the name of the option including the leading '-' character
     * @param space a boolean indicating whether this option consumes the next parameter ({@code true}) or not ({@code
     *            false})
     * @param defaultValue the default string value of this option
     * @param help the help text for this option
     */
    @HOSTED_ONLY
    public VMStringOption(String prefix, boolean space, String defaultValue, String help) {
        super(prefix, appendDefaultValue(help, defaultValue));
        this.value = defaultValue;
        this.space = space;
    }

    /**
     * Package private constructor for use at runtime when implementing repeated string (e.g. agent) options.
     */
    VMStringOption(String prefix) {
        super(prefix, "");
        space = false;
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
        cstring = optionValue;
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
        if (!cstring.isZero() && !allocated) {
            allocated = true;
            try {
                value = CString.utf8ToJava(cstring);
            } catch (Utf8Exception e) {
                Log.println("Error parsing value of " + this + " option");
            }
        }
        return value;
    }

    /**
     * Print the help text for this option.
     */
    @Override
    public void printHelp() {
        if (space) {
            VMOptions.printHelpForOption(category(), prefix, " <value>", help);
        } else {
            VMOptions.printHelpForOption(category(), prefix, "<value>", help);
        }
    }

    /**
     * Returns {@code true} if this option expects to consume the next argument.
     */
    @Override
    public boolean consumesNext() {
        return space;
    }

    /**
     * Returns {@code true} if this option was specified on the command line.
     */
    @Override
    public boolean isPresent() {
        return !cstring.isZero();
    }
}

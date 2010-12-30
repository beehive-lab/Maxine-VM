/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.*;

/**
 * Support for HotSpot style boolean options that start with "-XX:+" or "-XX:-".
 * Boolean options are turned on with -XX:+<option> and turned off with -XX:-<option>.
 *
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public class VMBooleanXXOption extends VMOption {

    protected final String inversePrefix;
    protected final int plusOrMinusIndex;

    /**
     * Creates a new boolean option whose prefix starts with "-XX:+" or "-XX:-".
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option in the
     * global registry or VM options.</b>
     *
     * @param name the name of the option, including the leading '-' character. The default {@linkplain #getValue()
     *            value} of the option is true or false depending on whether the prefix starts with "-XX:+" or "-XX:-"
     *            respectively.
     * @param help the help text for the option
     */
    @HOSTED_ONLY
    public VMBooleanXXOption(String name, String help) {
        this(getXXPrefix(name), name.substring(5), help);
    }

    /**
     * Creates a new boolean option.
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option in the
     * global registry or VM options.</b>
     *
     * @param a prefix that must end with {@code '+'} or {@code '-'} to indicate te default value of the option
     * @param name the name of the option, including the leading '-' character. The default {@linkplain #getValue()
     *            value} of the option is true or false depending on whether the prefix starts with "-XX:+" or "-XX:-"
     *            respectively.
     * @param help the help text for the option
     */
    public VMBooleanXXOption(String prefix, String name, String help) {
        super(prefix + name + " ", help);
        plusOrMinusIndex = prefix.length() - 1;
        if (prefix.charAt(plusOrMinusIndex) == '+') {
            inversePrefix = prefix.substring(0, plusOrMinusIndex) + "-" + name;
        } else if (prefix.charAt(plusOrMinusIndex) == '-') {
            inversePrefix = prefix.substring(0, plusOrMinusIndex) + "+" + name;
        } else {
            throw ProgramError.unexpected("Malformed VMBooleanXXOption syntax: " + prefix);
        }
    }

    @Override
    public boolean consumesNext() {
        return false;
    }

    /**
     * Gets the value of this boolean option.
     */
    public boolean getValue() {
        if (!optionStart.isZero()) {
            return ((char) optionStart.readByte(plusOrMinusIndex)) == '+';
        }
        return prefix.charAt(plusOrMinusIndex) == '+';
    }

    @Override
    public boolean matches(Pointer arg) {
        return CString.equals(arg, prefix) || CString.equals(arg, inversePrefix);
    }

    private static String getXXPrefix(String prefix) {
        assert prefix.startsWith("-XX:+") || prefix.startsWith("-XX:-");
        return prefix.substring(0, 5);
    }
}

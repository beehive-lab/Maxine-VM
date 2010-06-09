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

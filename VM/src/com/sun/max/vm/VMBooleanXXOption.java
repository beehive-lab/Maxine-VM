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
 */
public class VMBooleanXXOption extends VMOption {

    protected final String inversePrefix;

    /**
     * Creates a new boolean option whose prefix starts with "-XX:+" or "-XX:-".
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option in the
     * global registry or VM options.</b>
     *
     * @param prefix the name of the option, including the leading '-' character. The default {@linkplain #getValue()
     *            value} of the option is true or false depending on whether the prefix starts with "-XX:+" or "-XX:-"
     *            respectively.
     * @param defaultValue the default value of the option when it is not specified
     * @param help the help text for the option
     */
    @PROTOTYPE_ONLY
    public VMBooleanXXOption(String prefix, String help) {
        super(prefix, help);
        if (prefix.startsWith("-XX:+")) {
            inversePrefix = "-XX:-" + prefix.substring(5);
        } else if (prefix.startsWith("-XX:-")) {
            inversePrefix = "-XX:+" + prefix.substring(5);
        } else {
            throw ProgramError.unexpected("Instances of " + getClass() + " must have a prefix starting with '-XX:+' or '-XX:-'");
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
            return ((char) optionStart.readByte(4)) == '+';
        }
        return prefix.charAt(4) == '+';
    }

    @Override
    public boolean matches(Pointer arg) {
        return CString.equals(arg, prefix) || CString.equals(arg, inversePrefix);
    }
}

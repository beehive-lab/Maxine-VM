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
/*VCSID=0d9b917b-e6a1-4a28-8271-c1d4c5871537*/
package com.sun.max.vm;

import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.debug.*;

/**
 * A class that represents an option to the virtual machine, including both the low-level,
 * internal options (e.g. -Xmixed), and the standard options (e.g. -classpath). Several
 * subclasses of this class provide integer values, strings, and sizes. Some options
 * are parsed at different times than others; thus a VM option can have an associated
 * phase in which it is parsed.
 *
 * @author Ben L. Titzer
 */
public class VMOption {

    /**
     * Constants denoting the categories of VM options.
     */
    public enum Category {
        /**
         * Constant denoting options that do not start with "-X".
         */
        STANDARD,

        /**
         * Constant denoting options that start with "-X" but not "-XX".
         */
        NON_STANDARD,

        /**
         * Constant denoting options that start with "-XX".
         */
        IMPLEMENTATION_SPECIFIC;

        public static final Category from(String prefix) {
            if (prefix.startsWith("-XX")) {
                return Category.IMPLEMENTATION_SPECIFIC;
            }
            if (prefix.startsWith("-X")) {
                return Category.NON_STANDARD;
            }
            return Category.STANDARD;

        }
    }

    protected final String _prefix;
    protected final String _help;
    protected Pointer _optionStart = Pointer.zero();

    /**
     * Creates a new VM option with the specified string prefix (which includes the '-'),
     * the specified help text. The VM option will be parsed during the specified VM
     * startup phase.
     *
     * @param prefix the name of the option, including the leading '-' character
     * @param help the help text to be printed for this option on the command line
     * @param phase the phase in which to parse this option
     */
    public VMOption(String prefix, String help, Phase phase) {
        _prefix = prefix;
        _help = help;
        assert phase != null;
        VMOptions.addOption(this, phase);
    }

    /**
     * Called when the prefix of this option is matched on the command line,
     * this method should parse the option's value and return success or failure. The default
     * behavior is to simply remove the prefix (i.e. the name of this option) and pass
     * the result on to the {@code parseValue()} method, which is typically overridden
     * in subclasses.
     * @param optionStart a pointer to a C-style string representing the entire option
     * @return {@code true} if the option's value was parsed successfully, {@code false} otherwise
     */
    public boolean parse(Pointer optionStart) {
        _optionStart = optionStart;
        return parseValue(optionStart.plus(_prefix.length()));
    }

    /**
     * Parses the value of this option, without the leading prefix.
     *
     * @param optionValue a pointer to the beginning of a C-style string containing the options value to be parsed
     * @return {@code true} if the option was parsed successfully; {@code false} otherwise
     */
    public boolean parseValue(Pointer optionValue) {
        return true;
    }

    /**
     * Prints out help onto the console.
     */
    public void printHelp() {
        VMOptions.printHelpForOption(_prefix, "", _help);
    }

    /**
     * Checks whether this option was present on the command line.
     *
     * @return {@code true} if this option was present on the command line; {@code false} otherwise
     */
    public boolean isPresent() {
        return !_optionStart.isZero();
    }

    /**
     * Returns whether this option expects to consume the next argument on the command line. Most options do not (i.e.
     * their values are part of the same string as their prefix).
     *
     * @return {@code true} if this option consumes the next command line argument; {@code false} otherwise
     */
    public boolean consumesNext() {
        return false;
    }

    /**
     * Returns whether this option should terminate the list of options. Most options can be put in any order, but some
     * (currently only -jar) are required to be the last option, after which the arguments to the program begin.
     *
     * @return {@code true} if this option should terminate the list of options and begin the arguments to the program
     */
    public boolean isLastOption() {
        return false;
    }

    /**
     * Determines if this option has an invalid value or a value that somehow conflicts with another option.
     * This method must not performed any allocation.
     *
     * @return true if this option's value is valid, false otherwise
     */
    public boolean check() {
        return true;
    }

    /**
     * Prints an error message with the {@link Debug} facility describing why the call to {@link #check()} returned {@code false}.
     * This method will not be called if {@link #check()} returned {@code true}.
     */
    public void printErrorMessage() {
    }

    @Override
    public String toString() {
        return _prefix;
    }

    /**
     * Gets the category to which this option belongs.
     *
     * @return the category to which this option belongs
     */
    public Category category() {
        return Category.from(_prefix);
    }
}

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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.Arrays;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.*;

/**
 * A class that represents an option to the virtual machine, including both the low-level,
 * internal options (e.g. -Xmixed), and the standard options (e.g. -classpath). Several
 * subclasses of this class provide integer values, strings, and sizes. Some options
 * are parsed at different times than others; thus a VM option can have an associated
 * phase in which it is parsed.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
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
    protected final boolean _exactPrefix;
    protected final String _help;

    @RESET
    protected Pointer _optionStart = Pointer.zero();

    /**
     * Creates a new VM option with the specified string prefix (which includes the '-') and the specified help text.
     * The VM option will be parsed during the specified VM startup phase.
     *
     * @param prefix the name of the option, including the leading '-' character. If the name ends with a space, then it
     *            must be matched exactly against a VM argument, otherwise it matches if it is a prefix of a VM
     *            argument.
     * @param help the help text to be printed for this option on the command line
     * @param phase the phase in which to parse this option. This must be {@code null} if and only if this is
     *            {@linkplain #isSuboption() sub-option}
     */
    @PROTOTYPE_ONLY
    public VMOption(String prefix, String help, Phase phase) {
        _exactPrefix = prefix.endsWith(" ");
        if (_exactPrefix) {
            _prefix = prefix.substring(0, prefix.length() - 1);
        } else {
            _prefix = prefix;
        }
        _help = help;
        assert phase != null;
        parseOption();
        VMOptions.addOption(this, phase);
    }

    /**
     *
     * @return
     */
    VMOption[] suboptions() {
        return null;
    }

    private VMOption _parentOption;

    private VMOption[] _suboptions = {};

    @PROTOTYPE_ONLY
    void addSuboption(VMOption suboption) {
        assert suboption._prefix.startsWith(_prefix) && !suboption._prefix.equals(_prefix);
        assert suboption._parentOption == null : "Cannot re-parent suboption from " + suboption._parentOption + " to " + this;
        _suboptions = java.util.Arrays.copyOf(_suboptions, _suboptions.length + 1);
        _suboptions[_suboptions.length - 1] = suboption;
        suboption._parentOption = this;
    }

    /**
     * Determines if this option matches a given command line argument.
     *
     * @param arg a command line argument
     * @return {@code true} if this option matches {@code arg}; otherwise {@code false}
     */
    public boolean matches(Pointer arg) {
        return _exactPrefix ? CString.equals(arg, _prefix) : CString.startsWith(arg, _prefix);
    }

    /**
     * Called when the prefix of this option is {@linkplain #matches(Pointer) matched} on the command line,
     * this method should parse the option's value and return success or failure. The default
     * behavior is to simply remove the prefix (i.e. the name of this option) and pass
     * the result on to the {@code parseValue()} method, which is typically overridden
     * in subclasses.
     * @param optionStart a pointer to a C-style string representing the entire option
     * @return {@code true} if the option's value was parsed successfully, {@code false} otherwise
     */
    public boolean parse(Pointer optionStart) {
        _optionStart = optionStart;
        if (_suboptions.length == 0) {
            return parseValue(optionStart.plus(_prefix.length()));
        }
        for (VMOption suboption : _suboptions) {
            if (!suboption.parse(optionStart)) {
                return false;
            }
        }
        return true;
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
        return !_optionStart.isZero() || (_parentOption != null && _parentOption.isPresent());
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
     * Prints an error message with the {@link Log} facility describing why the call to {@link #check()} returned {@code false}.
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


    // Prototype-time support for setting VM options

    @PROTOTYPE_ONLY
    static String[] _vmArguments = null;
    @PROTOTYPE_ONLY
    static String[] _matchedVmArguments = null;

    /**
     * Sets the VM command line arguments that will be parsed for each {@link VMOption} created.
     * This allows some functionality/state controlled by VM options to be exercised and/or pre-set while
     * building the boot image.
     *
     * Note: Any option values set at prototype time while building the boot image are persisted in the boot image.
     *
     * @param vmArguments a set of command line arguments used to enable VM options at prototype time
     */
    @PROTOTYPE_ONLY
    public static void setVMArguments(String[] vmArguments) {
        _vmArguments = vmArguments;
        _matchedVmArguments = new String[vmArguments.length];
    }

    /**
     * Gets all of the VM arguments provided to {@link #setVMArguments(String[])} that have not been matched
     * against a VM option.
     */
    @PROTOTYPE_ONLY
    public static List<String> unmatchedVMArguments() {
        if (_vmArguments != null) {
            final List<String> unmatched = new ArrayList<String>(_vmArguments.length);
            for (int i = 0; i < _vmArguments.length; ++i) {
                if (_matchedVmArguments[i] == null) {
                    unmatched.add(_vmArguments[i]);
                }
            }
            return unmatched;
        }
        return Collections.emptyList();
    }

    @PROTOTYPE_ONLY
    private static ProgramError parseError(String message, int index) {
        final StringBuilder sb = new StringBuilder(String.format("Error parsing VM option: %s:%n%s%n", message, Arrays.toString(_vmArguments, " ")));
        for (int i = 0; i < index; ++i) {
            sb.append(' ');
        }
        sb.append('^');
        throw ProgramError.unexpected(sb.toString());
    }

    @PROTOTYPE_ONLY
    public void parseOption() {
        if (_vmArguments != null) {
            for (int i = 0; i < _vmArguments.length; ++i) {
                final String argument = _vmArguments[i];
                if (argument != null && (_exactPrefix ? argument.equals(_prefix) : argument.startsWith(_prefix))) {
                    _matchedVmArguments[i] = argument;
                    if (consumesNext()) {
                        // this option expects a space and then its value (e.g. -classpath)
                        if (i + 1 >= _vmArguments.length) {
                            parseError("Could not find argument for " + this, i);
                        }
                        final Pointer optionValue = CString.utf8FromJava(_vmArguments[i + 1]);
                        _vmArguments[i + 1] = null;
                        final boolean ok = parseValue(optionValue);
                        Memory.deallocate(optionValue);
                        if (!ok) {
                            parseError("Error parsing " + this, i);
                        }
                    } else {
                        final Pointer optionValue = CString.utf8FromJava(argument);
                        final boolean ok = parse(optionValue);
                        Memory.deallocate(optionValue);
                        if (!ok) {
                            parseError("Error parsing " + this, i);
                        }
                    }
                }
            }
        }
    }

    /**
     * Called once before the VM exits. This method exists primarily for VMOption subclasses to
     * print out final statistics just before the VM exits. As such, all output it generates
     * should be sent to {@link Log#out} and object allocation should be avoided.
     */
    protected void beforeExit() {
    }
}

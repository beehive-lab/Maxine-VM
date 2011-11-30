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

import java.util.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.MaxineVM.Phase;

/**
 * A class that represents an option to the virtual machine, including both the low-level,
 * internal options (e.g. -Xmixed), and the standard options (e.g. -classpath). Several
 * subclasses of this class provide integer values, strings, and sizes. Some options
 * are parsed at different times than others; thus a VM option can have an associated
 * phase in which it is parsed.
 *
 * A VM option must be {@linkplain VMOptions#register(VMOption, Phase) registered} to be
 * enabled at runtime.
 */
public class VMOption {

    /**
     * Constants denoting the categories of VM options.
     */
    public enum Category {
        /**
         * Constant denoting options that do not start with "-X".
         */
        STANDARD(18, 72, "-"),

        /**
         * Constant denoting options that start with "-X" but not "-XX".
         */
        NON_STANDARD(22, 92, "-X"),

        /**
         * Constant denoting options that start with "-XX".
         */
        IMPLEMENTATION_SPECIFIC(42, 122, "-XX");

        public final int helpIndent;
        public final int helpLineMaxWidth;
        public final String prefix;

        Category(int helpIndent, int helpLineMaxWidth, String prefix) {
            this.helpIndent = helpIndent;
            this.helpLineMaxWidth = helpLineMaxWidth;
            this.prefix = prefix;
        }

        public String optionName(VMOption option) {
            String name = option.prefix.substring(prefix.length());
            while (!Character.isJavaIdentifierStart(name.charAt(0))) {
                name = name.substring(1);
            }
            for (int i = 0; i < name.length(); i++) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                    return name.substring(0, i);
                }
            }
            return name;
        }

        static boolean isImplementationSpecificPrefixChar(char c) {
            if (c >= 'A' && c <= 'Z') {
                return true;
            }
            if (c >= '0' && c <= '9') {
                return true;
            }
            return false;
        }

        public static Category from(String prefix) {
            int colon = prefix.indexOf(':');
            if (colon != -1) {
                int i = 1;
                while (i < colon) {
                    char c = prefix.charAt(i);
                    if (!isImplementationSpecificPrefixChar(c)) {
                        break;
                    }
                    i++;
                }
                if (i == colon) {
                    return Category.IMPLEMENTATION_SPECIFIC;
                }
            }


            if (prefix.startsWith("-XX")) {
                return Category.IMPLEMENTATION_SPECIFIC;
            }
            if (prefix.startsWith("-X")) {
                return Category.NON_STANDARD;
            }
            return Category.STANDARD;
        }
    }

    /**
     * The name of the option. This is only unique within this option's {@linkplain #category() category}.
     */
    protected final String name;

    protected final String prefix;
    protected final boolean exactPrefix;
    protected final String help;

    @RESET
    protected Pointer optionStart = Pointer.zero();

    /**
     * Extends a given VM option help message with a suffix describing a given default value.
     *
     * @param help the original help message
     * @param defaultValue the string version of a default value (ignored if {@code null})
     * @return the help message extended to include {@code defaultValue} if the latter is not {@code null}
     */
    @HOSTED_ONLY
    protected static String appendDefaultValue(String help, String defaultValue) {
        if (defaultValue == null) {
            return help;
        }
        if (help == null || help.length() == 0) {
            return "(default: " + defaultValue + ")";
        }
        return help + " (default: " + defaultValue + ")";
    }

    /**
     * Creates a new VM option with the specified string prefix (which includes the '-') and the specified help text.
     *
     * <b>The caller is responsible for {@linkplain VMOptions#register(VMOption, Phase) registering} this option
     * in the global registry or VM options.</b>
     *
     * @param prefix the name of the option, including the leading '-' character. If the name ends with a space, then it
     *            must be matched exactly against a VM argument, otherwise it matches if it is a prefix of a VM
     *            argument.
     * @param help the help text to be printed for this option on the command line
     */
    public VMOption(String prefix, String help) {
        exactPrefix = prefix.endsWith(" ");
        if (exactPrefix) {
            this.prefix = prefix.substring(0, prefix.length() - 1);
        } else {
            this.prefix = prefix;
        }
        this.help = help;
        this.name = category().optionName(this);
    }

    /**
     * Determines if this option matches a given command line argument.
     *
     * @param arg a command line argument
     * @return {@code true} if this option matches {@code arg}; otherwise {@code false}
     */
    public boolean matches(Pointer arg) {
        return exactPrefix ? CString.equals(arg, prefix) : CString.startsWith(arg, prefix);
    }

    /**
     * Called when the prefix of this option is {@linkplain #matches(Pointer) matched} on the command line,
     * this method should parse the option's value and return success or failure. The default
     * behavior is to simply remove the prefix (i.e. the name of this option) and pass
     * the result on to the {@code parseValue()} method, which is typically overridden
     * in subclasses.
     * @param start a pointer to a C-style string representing the entire option
     * @return {@code true} if the option's value was parsed successfully, {@code false} otherwise
     */
    public boolean parse(Pointer start) {
        this.optionStart = start;
        return parseValue(start.plus(prefix.length()));
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
        VMOptions.printHelpForOption(category(), prefix, "", help);
    }

    /**
     * Checks whether this option was present on the command line.
     *
     * @return {@code true} if this option was present on the command line; {@code false} otherwise
     */
    public boolean isPresent() {
        return !optionStart.isZero();
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
        return prefix;
    }

    /**
     * Gets the category to which this option belongs.
     *
     * @return the category to which this option belongs
     */
    public Category category() {
        return Category.from(prefix);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof VMOption) {
            VMOption option = (VMOption) obj;
            return option.category() == category() && option.name.equals(name);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }

    /**
     * Determines if this option halts the VM when it is parsed on the command line.
     */
    protected boolean haltsVM() {
        return false;
    }

    // Prototype-time support for setting VM options

    @HOSTED_ONLY
    static String[] vmArguments = null;
    @HOSTED_ONLY
    static Pointer vmArgumentPointers = null;
    @HOSTED_ONLY
    static String[] matchedVmArguments = null;

    static {
        // Simple way to set VM options via a system property when running in hosted mode
        String value = System.getProperty("max.vmargs");
        if (value != null) {
            setVMArguments(value.split("\\s+"));
        }
    }

    /**
     * Sets the VM command line arguments that will be parsed for each {@link VMOption} created.
     * This allows some functionality/state controlled by VM options to be exercised and/or pre-set while
     * building the boot image.
     *
     * Note: Any option values set while bootstrapping are persisted in the boot image.
     *
     * @param vmArgs a set of command line arguments used to enable VM options while bootstrapping
     */
    @HOSTED_ONLY
    public static void setVMArguments(String[] vmArgs) {
        vmArguments = vmArgs;
        if (vmArgumentPointers != null) {
            Memory.deallocate(vmArgumentPointers);
            vmArgumentPointers = null;
        }
        matchedVmArguments = new String[vmArguments.length];
    }

    @HOSTED_ONLY
    public static String[] extractVMArgs(String[] args) {
        ArrayList<String> vmArgs = new ArrayList<String>();
        ArrayList<String> otherArgs = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                vmArgs.add(arg.substring(1));
            } else {
                otherArgs.add(arg);
            }
        }
        if (!vmArgs.isEmpty()) {
            setVMArguments(vmArgs.toArray(new String[vmArgs.size()]));
            args = otherArgs.toArray(new String[otherArgs.size()]);
        }
        return args;
    }

    /**
     * Gets all of the VM arguments provided to {@link #setVMArguments(String[])} that have not been matched
     * against a VM option.
     * @return all of the VM arguments that have not been matched
     */
    @HOSTED_ONLY
    public static List<String> unmatchedVMArguments() {
        if (vmArguments != null) {
            final List<String> unmatched = new ArrayList<String>(vmArguments.length);
            for (int i = 0; i < vmArguments.length; ++i) {
                if (matchedVmArguments[i] == null) {
                    unmatched.add(vmArguments[i]);
                }
            }
            return unmatched;
        }
        return Collections.emptyList();
    }

    @HOSTED_ONLY
    private static ProgramError parseError(String message, int index) {
        final StringBuilder sb = new StringBuilder(String.format("Error parsing VM option: %s:%n%s%n", message, Utils.toString(vmArguments, " ")));
        for (int i = 0; i < index; ++i) {
            sb.append(' ');
        }
        sb.append('^');
        throw ProgramError.unexpected(sb.toString());
    }

    /**
     * Searches for a {@linkplain #setVMArguments(String[]) registered} VM argument that this option matches and,
     * if found, calls {@link #parse(Pointer)} or {@link #parseValue(Pointer)} on the argument.
     */
    @HOSTED_ONLY
    public void findMatchingArgumentAndParse() {
        if (vmArguments != null) {
            if (vmArgumentPointers == null) {
                vmArgumentPointers = Pointer.fromLong(CString.utf8ArrayFromStringArray(vmArguments, false, false));
            }
            for (int i = 0; i < vmArguments.length; ++i) {
                final String argument = vmArguments[i];
                final Pointer argumentPointer = vmArgumentPointers.getWord(i).asPointer();
                if (argument != null && (matches(argumentPointer))) {
                    matchedVmArguments[i] = argument;
                    if (consumesNext()) {
                        // this option expects a space and then its value (e.g. -classpath)
                        if (i + 1 >= vmArguments.length) {
                            parseError("Could not find argument for " + this, i);
                        }
                        final Pointer optionValue = vmArgumentPointers.getWord(i + 1).asPointer();
                        vmArguments[i + 1] = null;
                        final boolean ok = parseValue(optionValue);
                        if (!ok) {
                            parseError("Error parsing " + this, i);
                        }
                    } else {
                        final boolean ok = parse(argumentPointer);
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

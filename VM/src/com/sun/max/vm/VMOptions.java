/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.lang.reflect.*;
import java.util.*;

import sun.misc.*;
import sun.reflect.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.VMOption.Category;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;

/**
 * VM options handling.
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class VMOptions {

    @HOSTED_ONLY
    private static final Comparator<VMOption> OPTION_COMPARATOR = new Comparator<VMOption>() {
        @Override
        public int compare(VMOption o1, VMOption o2) {
            int categoryOrder = o1.category().ordinal() - o2.category().ordinal();
            if (categoryOrder != 0) {
                return categoryOrder;
            }
            int nameOrder = o1.name.compareTo(o2.name);
            if (nameOrder != 0) {
                return nameOrder;
            }
            return o1.prefix.compareTo(o2.prefix);
        }
    };

    /**
     * A VM option of the form {@code "-XX:name=value"} that updates a {@code String} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class StringFieldOption extends VMStringOption {

        public final Field field;

        StringFieldOption(String prefix, boolean space, String defaultValue, String help, Field field) {
            super(prefix, space, defaultValue, help);
            this.field = field;
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    setFieldValue(field, getValue());
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeReference(fieldActor.offset(), Reference.fromJava(getValue()));
                }
                return true;
            }
            return result;
        }
    }

    /**
     * A VM option of the form {@code "-XX:name=value"} that updates a {@code float} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class FloatFieldOption extends VMFloatOption {

        public final Field field;

        FloatFieldOption(String prefix, float defaultValue, String help, Field field) {
            super(prefix, defaultValue, help);
            this.field = field;
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    setFieldValue(field, getValue());
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeFloat(fieldActor.offset(), getValue());
                }
                return true;
            }
            return result;
        }
    }

    /**
     * A VM option of the form {@code "-XX:name=value"} that updates an {@code int} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class IntFieldOption extends VMIntOption {

        public final Field field;

        IntFieldOption(String prefix, int defaultValue, String help, Field field) {
            super(prefix, defaultValue, help);
            this.field = field;
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    setFieldValue(field, getValue());
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeInt(fieldActor.offset(), getValue());
                }
                return true;
            }
            return result;
        }
    }

    /**
     * A VM option of the form {@code "-XX:name=value"} that updates a {@code Size} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class SizeFieldOption extends VMSizeOption {

        public final Field field;

        SizeFieldOption(String prefix, Size defaultValue, String help, Field field) {
            super(prefix, defaultValue, help);
            this.field = field;
        }



        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    hostedSetFieldValue();
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeWord(fieldActor.offset(), getValue());
                }
                return true;
            }
            return result;
        }

        /**
         * Factored out as a {@link HOSTED_ONLY} method because it won't verify - it
         * mixes word and reference types.
         */
        @HOSTED_ONLY
        void hostedSetFieldValue() {
            setFieldValue(field, getValue());
        }
    }

    /**
     * A VM option of the form {@code "-XX:[+|-]name=value"} that updates a {@code boolean} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class BooleanFieldOption extends VMBooleanXXOption {

        public final Field field;

        BooleanFieldOption(String prefix, String name, String help, Field field) {
            super(prefix, name, help);
            this.field = field;
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    setFieldValue(field, getValue());
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeBoolean(fieldActor.offset(), getValue());
                }
                return true;
            }
            return false;
        }
    }

    /**
     * A VM option of the form {@code "-name"} or {@code "-Xname"} that updates a {@code boolean} field when
     * the option is {@linkplain VMOption#parseValue(Pointer) parsed}.
     */
    public static final class SimpleBooleanFieldOption extends VMOption {

        public final Field field;

        SimpleBooleanFieldOption(String prefix, String help, Field field) {
            super(prefix + " ", help);
            this.field = field;
        }

        @Override
        public boolean parseValue(Pointer optionValue) {
            boolean result = super.parseValue(optionValue);
            if (result) {
                if (MaxineVM.isHosted()) {
                    setFieldValue(field, isPresent());
                } else {
                    FieldActor fieldActor = FieldActor.fromJava(field);
                    Reference.fromJava(fieldActor.holder().staticTuple()).writeBoolean(fieldActor.offset(), isPresent());
                }
                return true;
            }
            return false;
        }
    }

    /**
     * The set of {@linkplain Phase#PRISTINE pristine-phase} VM options built into the boot image.
     */
    private static VMOption[] pristinePhaseOptions = {};

    /**
     * The set of {@linkplain Phase#STARTING starting-phase} VM options built into the boot image.
     */
    private static VMOption[] startingPhaseOptions = {};

    /**
     * All the options sorted by {@linkplain VMOption#category() category} and then by {@linkplain VMOption#prefix prefix}.
     */
    private static VMOption[] sortedOptions;

    private static Pointer savedArgv;
    private static Pointer argv;
    private static int argc;
    private static int argumentStart;

    private static boolean earlyVMExitRequested;

    private static String[] mainClassArguments;
    private static String mainClassName;

    /**
     * This is a reference to the initial value of {@link System#props} when the VM starts up.
     * The "magic" in {@link JavaPrototype#hostToTarget(Object)} will ensure that this map
     * only has the properties from the host specified by {@link JDKInterceptor#REMEMBERED_PROPERTY_NAMES}.
     * The system properties parsed on the command line are stored in this map.
     * This is required so that they are available before the System class is initialized.
     */
    private static final Properties initialSystemProperties = System.getProperties();

    /**
     * The {@code -jar} option.
     */
    private static final VMStringOption jarOption = register(new VMStringOption("-jar", true, null, "Executes main class from jar file.") {
        @Override
        public boolean isLastOption() {
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    /**
     * This option is parsed in the native code (see maxine.c). It's declared here simply so that it
     * shows up in the {@linkplain #printUsage() usage} message.
     */
    private static final VMStringOption logFileOption = register(new VMStringOption("-XX:LogFile=", false, null,
        "Redirect VM log output to the specified file. By default, VM log output goes to the standard output stream."), MaxineVM.Phase.STARTING);

    /**
     * The '-verbose' option and all its variants (e.g. '-verbose:gc', '-verbose:class' etc).
     */
    public static final VerboseVMOption verboseOption = register(new VerboseVMOption(), MaxineVM.Phase.PRISTINE);

    static {
        register(new VMIntOption("-XX:TraceLevel=", 0, "Enable tracing output at the specified level.") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                super.parseValue(optionValue);
                Trace.on(getValue());
                return true;
            }
        }, MaxineVM.Phase.PRISTINE);

        register(new VMOption("-X ", "Print help on non-standard options") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                printUsage(Category.NON_STANDARD);
                return true;
            }
            @Override
            protected boolean haltsVM() {
                return true;
            }
            @Override
            public Category category() {
                return Category.STANDARD;
            }
        }, MaxineVM.Phase.PRISTINE);

        register(new VMOption("-XX ", "Print help on Maxine options") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                printUsage(Category.IMPLEMENTATION_SPECIFIC);
                return true;
            }
            @Override
            protected boolean haltsVM() {
                return true;
            }
            @Override
            public Category category() {
                return Category.NON_STANDARD;
            }
        }, MaxineVM.Phase.PRISTINE);

        register(new VMOption("-C1X ", "Print help on C1X options") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                printUsage(Category.C1X_SPECIFIC);
                return true;
            }
            @Override
            protected boolean haltsVM() {
                return true;
            }
            @Override
            public Category category() {
                return Category.NON_STANDARD;
            }
        }, MaxineVM.Phase.PRISTINE);

        register(new VMBooleanXXOption("-XX:-PrintConfiguration", "Show VM configuration details and exit") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                Log.println("  Platform: " + platform());
                vmConfig().print(Log.out, "  ");
                return true;
            }
            @Override
            protected boolean haltsVM() {
                return true;
            }
        }, MaxineVM.Phase.STARTING);

        register(new VMBooleanXXOption("-XX:-ShowConfiguration", "Show VM configuration details and continue") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                Log.println("  Platform: " + platform());
                vmConfig().print(Log.out, "  ");
                return true;
            }
        }, MaxineVM.Phase.STARTING);

        register(new VMOption("-client", "ignored (present for compatibility)"), MaxineVM.Phase.STARTING);
        register(new VMOption("-server", "ignored (present for compatibility)"), MaxineVM.Phase.STARTING);
    }

    private VMOptions() {
    }

    /**
     * Gets all the registered VM options as a set sorted by {@linkplain VMOption#category() category} and then by {@linkplain VMOption#prefix prefix}.
     */
    @HOSTED_ONLY
    public static SortedSet<VMOption> allOptions() {
        TreeSet<VMOption> result = new TreeSet<VMOption>(OPTION_COMPARATOR);
        result.addAll(Arrays.asList(pristinePhaseOptions));
        result.addAll(Arrays.asList(startingPhaseOptions));
        return result;
    }

    @HOSTED_ONLY
    private static VMOption[] addOption(VMOption[] options, VMOption option, Set<VMOption> allOptions) {
        if (option.category() == VMOption.Category.IMPLEMENTATION_SPECIFIC) {
            final int prefixLength = option instanceof VMBooleanXXOption ? "-XX:+".length() : "-XX:".length();
            final String name = option.prefix.substring(prefixLength);
            ProgramError.check(Character.isUpperCase(name.charAt(0)), "Option with \"-XX:\" prefix must start with an upper-case letter: " + option);
        }
        for (VMOption existingOption : allOptions) {
            ProgramError.check(!existingOption.prefix.equals(option.prefix), "VM option prefix is not unique: " + option.prefix);
            ProgramError.check(OPTION_COMPARATOR.compare(existingOption, option) != 0, "VM option has non-unique sort key: " + option + " [clashes with " + existingOption + "]");
        }
        return Utils.concat(options, option);
    }

    /**
     * Registers a given VM option in the global option registry that is used to match command
     * line arguments passed to the VM at runtime.
     *
     * @param option a VM option
     * @param phase the VM phase during which the option should be parsed
     * @return the {@code option} object
     */
    @HOSTED_ONLY
    public static <T extends VMOption> T register(VMOption option, MaxineVM.Phase phase) {
        assert phase != null;
        final SortedSet<VMOption> allOptions = allOptions();
        if (phase == MaxineVM.Phase.PRISTINE) {
            pristinePhaseOptions = addOption(pristinePhaseOptions, option, allOptions);
        } else if (phase == MaxineVM.Phase.STARTING) {
            assert !option.consumesNext();
            startingPhaseOptions = addOption(startingPhaseOptions, option, allOptions);
        } else {
            ProgramError.unexpected("VM options for the " + phase + " phase not (yet) supported");
        }

        allOptions.add(option);
        sortedOptions = allOptions.toArray(new VMOption[allOptions.size()]);

        option.findMatchingArgumentAndParse();
        final Class<T> type = null;
        return Utils.cast(type, option);
    }

    /**
     * Creates and registers "-XX" VM options for each non-{@code final} {@code static} field
     * in a given class.
     *
     * @param prefix
     * @param javaClass the java class containing the fields for which VM options are to be created
     * @param helpMap map from option names to the help message for the option (may be {@code null})
     */
    @HOSTED_ONLY
    public static void addFieldOptions(String prefix, Class<?> javaClass, Map<String, String> helpMap) {
        for (final Field field : javaClass.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && !Modifier.isFinal(modifiers)) {
                final OptionSettings settings = field.getAnnotation(OptionSettings.class);
                String help;
                String name;
                if (settings != null) {
                    help = settings.help();
                    name = settings.name().isEmpty() ? field.getName().replace('_', '-') : settings.name();
                } else {
                    assert !field.getName().contains("_");
                    name = field.getName().replace('_', '-');
                    help = helpMap != null ? helpMap.get(name) : null;
                }
                try {
                    addFieldOption(prefix, name, field, help, MaxineVM.Phase.STARTING);
                } catch (Exception e) {
                    throw ProgramError.unexpected("Error creating VM option for " + field, e);
                }
            }
        }
    }

    @HOSTED_ONLY
    static void setFieldValue(Field field, Object value) {
        try {
            field.set(null, value);
        } catch (Exception e) {
            throw ProgramError.unexpected("Error setting value of " + field + " to " + value, e);
        }
    }

    /**
     * Creates and registers a VM option whose value is stored in a given non-final {@code static} field.
     *
     * @param prefix the prefix to use for the option (e.g. {@code "-XX:"} or {@code "-C1X:"})
     * @param name the name of the option
     * @param declaringClass the class in which a field named {@code name} backing the option
     * @param help the help text for the option
     */
    @HOSTED_ONLY
    public static VMOption addFieldOption(String prefix, String name, String help) {
        Class declaringClass = Reflection.getCallerClass(2);
        return addFieldOption(prefix, name, Classes.getDeclaredField(declaringClass, name), help, MaxineVM.Phase.STARTING);
    }

    /**
     * Creates and registers a VM option whose value is stored in a given non-final {@code static} field.
     *
     * @param prefix the prefix to use for the option (e.g. {@code "-XX:"} or {@code "-C1X:"})
     * @param name the name of the option
     * @param declaringClass the class in which a field named {@code name} backing the option
     * @param help the help text for the option
     */
    @HOSTED_ONLY
    public static VMOption addFieldOption(String prefix, String name, Class declaringClass, String help) {
        return addFieldOption(prefix, name, Classes.getDeclaredField(declaringClass, name), help, MaxineVM.Phase.STARTING);
    }

    /**
     * Creates and registers a VM option whose value is stored in a given non-final {@code static} field.
     *
     * @param prefix the prefix to use for the option (e.g. {@code "-XX:"} or {@code "-C1X:"})
     * @param name the name of the option
     * @param declaringClass the class in which a field named {@code name} backing the option
     * @param help the help text for the option
     * @param phase the VM phase during which the option should be parsed
     */
    @HOSTED_ONLY
    public static VMOption addFieldOption(String prefix, String name, Class declaringClass, String help, MaxineVM.Phase phase) {
        return addFieldOption(prefix, name, Classes.getDeclaredField(declaringClass, name), help, phase);
    }

    /**
     * Creates and registers a VM option whose value is stored in a given non-final {@code static} field.
     *
     * @param prefix the prefix to use for the option (e.g. {@code "-XX:"} or {@code "-C1X:"})
     * @param name the name of the option
     * @param field the field backing the option
     * @param help the help text for the option
     * @param phase the VM phase during which the option should be parsed
     */
    @HOSTED_ONLY
    public static VMOption addFieldOption(String prefix, String name, Field field, String help, Phase phase) {
        try {
            assert Modifier.isStatic(field.getModifiers());
            assert !Modifier.isFinal(field.getModifiers());
            final Class<?> fieldType = field.getType();
            if (MaxineVM.isHosted()) {
                field.setAccessible(true);
            }
            VMOption option;
            if (fieldType == boolean.class) {
                boolean defaultValue = field.getBoolean(null);
                Category c = Category.from(prefix);
                switch (c) {
                    case STANDARD:
                    case NON_STANDARD:
                        option = new SimpleBooleanFieldOption(prefix + name, help, field);
                        break;
                    case C1X_SPECIFIC:
                    case IMPLEMENTATION_SPECIFIC:
                        option = new BooleanFieldOption(prefix + (defaultValue ? '+' : '-'), name, help, field);
                        break;
                    default:
                        throw FatalError.unexpected(c.toString());
                }
                register(option, phase);
            } else if (fieldType == int.class) {
                int defaultValue = field.getInt(null);
                option = new IntFieldOption(prefix + name + "=", defaultValue, help, field);
                register(option, phase);
            } else if (fieldType == float.class) {
                float defaultValue = field.getFloat(null);
                option = new FloatFieldOption(prefix + name + "=", defaultValue, help, field);
                register(option, phase);
            } else if (fieldType == Size.class) {
                Size defaultValue = (Size) field.get(null);
                option = new SizeFieldOption(prefix + name + "=", defaultValue, help, field);
                register(option, phase);
            } else if (fieldType == String.class) {
                String defaultValue = (String) field.get(null);
                option = new StringFieldOption(prefix + name + "=", false, defaultValue, help, field);
                register(option, phase);
            } else {
                throw new RuntimeException("Field type unsupported by VM options");
            }
            return option;
        } catch (Exception e) {
            throw ProgramError.unexpected("Error creating VM option for " + field, e);
        }
    }

    public static void printHelpForOption(Category category, String prefix, String value, String help) {
        Log.print("    ");
        Log.print(prefix);
        Log.print(value);
        if (help != null) {
            Log.print(" ");
            int column = 5 + prefix.length() + value.length();
            if (column >= category.helpIndent) {
                Log.println();
                column = 0;
            }
            for (; column < category.helpIndent; column++) {
                Log.print(' ');
            }
            // reformat the help text by wrapping the lines after column 72.
            // Strings.formatParagraphs() can't be used because allocation may not work here
            for (int j = 0; j < help.length(); j++) {
                final char ch = help.charAt(j);
                if (column > category.helpLineMaxWidth && (ch == ' ' || ch == '\t')) {
                    Log.println();
                    for (int k = 0; k < category.helpIndent; k++) {
                        Log.print(' ');
                    }
                    column = category.helpIndent;
                } else {
                    Log.print(ch);
                    column++;
                }
            }
        }
        Log.println();
    }

    private static void printOptions(Category category) {
        for (VMOption option : sortedOptions) {
            if (option.category() == category) {
                option.printHelp();
            }
        }
    }

    /**
     * Prints the usage message for a given category or options.
     *
     * @param category the category of options to print the usage message for
     */
    public static void printUsage(Category category) {
        if (category == Category.STANDARD) {
            Log.println("Usage: maxvm [-options] [class | -jar jarfile]  [args...]");
            Log.println("where options include:");
            printOptions(category);
        }
        if (category == Category.NON_STANDARD) {
            Log.println("Non-standard options:");
            printOptions(category);
        }
        if (category == Category.C1X_SPECIFIC) {
            Log.println("C1X options:");
            printOptions(category);
        }
        if (category == Category.IMPLEMENTATION_SPECIFIC) {
            Log.println("Maxine options:");
            printOptions(category);
        }
    }

    /**
     * Determines if the VM should terminate. This will be true if there was an error while parsing the VM options.
     * It may also be true if the semantics of some VM option is to print some diagnostic info and then
     * exit the VM.
     */
    public static boolean earlyVMExitRequested() {
        return earlyVMExitRequested;
    }

    protected static void error(String errorMessage) {
        earlyVMExitRequested = true;
        Log.print("VM program argument parsing error: ");
        Log.println(errorMessage);
        printUsage(Category.STANDARD);
        MaxineVM.setExitCode(1);
    }

    protected static void error(VMOption option) {
        earlyVMExitRequested = true;
        Log.print("Error while parsing ");
        Log.print(option.toString());
        Log.print(": ");
        option.printErrorMessage();
        Log.println();
        printUsage(Category.STANDARD);
        MaxineVM.setExitCode(1);
    }

    public static String jarFile() {
        return jarOption.getValue();
    }

    /**
     * Gets the index of the next non-empty {@linkplain #argv command line argument} starting at a given index.
     *
     * @param start the index of the first argument to consider
     * @return the index of the first word in {@link #argv} that points to a non-empty C string or -1 if there is no
     *         such command line argument at whose index is greater than or equal to {@code index} and less than
     *         {@link #argc}
     */
    private static int findArgument(int start) {
        if (start == -1) {
            return -1;
        }
        for (int i = start; i < argc; i++) {
            final Pointer argument = argv.getWord(0, i).asPointer();
            if (!argument.isZero() && !CString.length(argument).isZero()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parses a given option whose {@linkplain VMOption#prefix prefix} is at a given index in the command line
     * arguments.
     *
     * @param index the index of {@code option}'s prefix in the command line arguments
     * @param argument the command line argument at {@code index}
     * @param option an option whose prefix matches the beginning of {@code argument}
     * @return the index of the next command line argument not consumed by {@code option} or -1 if {@code option}
     *         {@linkplain VMOption#isLastOption() terminates} the list of options
     */
    private static int parseOption(int index, final Pointer argument, final VMOption option) {
        final int nextIndex;
        if (option.consumesNext()) {
            // this option expects a space and then its value (e.g. -classpath)
            if (findArgument(index + 1) != index + 1) {
                error(option.toString());
            }
            // parse the next argument as this option's value
            if (!option.parseValue(argv.getWord(index + 1).asPointer())) {
                error(option.toString());
            } else if (option.haltsVM()) {
                earlyVMExitRequested = true;
            }
            argv.setWord(index, Word.zero());
            argv.setWord(index + 1, Word.zero());
            nextIndex = index + 2;
        } else {
            // otherwise ask the option to parse itself
            if (!option.parse(argument)) {
                error(option.toString());
            } else if (option.haltsVM()) {
                earlyVMExitRequested = true;
            }
            argv.setWord(index, Word.zero());
            nextIndex = index + 1;
        }
        if (option.isLastOption()) {
            argumentStart = nextIndex;
            return -1;
        }
        return nextIndex;
    }

    private static VMOption findVMOption(Pointer arg, VMOption[] options) {
        for (VMOption option : options) {
            if (option.matches(arg)) {
                return option;
            }
        }
        return null;
    }

    /**
     * Copy the initial argv array.
     * Currently, argument parsing is destructive and we may access to the original VM arguments
     * for the runtime management interface (@see getVmArguments)
     * @param initialArgc
     * @param initialArgv
     * @return
     */
    private static Pointer copy(int initialArgc, Pointer initialArgv) {
        final Size copySize = Size.fromInt(Pointer.size() * initialArgc);
        Pointer p = Memory.allocate(copySize);
        Memory.copyBytes(initialArgv, p, copySize);
        return p;
    }

    public static boolean parsePristine(int initialArgc, Pointer initialArgv) {
        savedArgv = initialArgv;
        argv = copy(initialArgc, initialArgv);
        argc = initialArgc;
        argumentStart = initialArgc;

        int index = findArgument(1); // skip the first argument (the name of the executable)
        while (index >= 0) {
            final Pointer argument = argv.getWord(index).asPointer();
            final VMOption option = findVMOption(argument, pristinePhaseOptions);
            if (option != null) {
                // some option prefix matched. attempt to parse it.
                index = parseOption(index, argument, option);
                if (index < 0) {
                    break;
                }
            } else if (argument.getByte() == '-') {
                index++;
                // an option to be handled later
            } else {
                // the first non-option argument must be the main class, unless -jar
                argumentStart = index;
                break;
            }
            index = findArgument(index);
        }
        return checkOptionsForErrors(pristinePhaseOptions);
    }

    protected static String getArgumentString(int index) throws Utf8Exception {
        final Pointer cArgument = argv.getWord(index).asPointer();
        if (cArgument.isZero()) {
            return null;
        }
        final String result = CString.utf8ToJava(cArgument);
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static boolean checkOptionsForErrors(VMOption[] options) {
        if (earlyVMExitRequested) {
            return false;
        }
        for (VMOption option : options) {
            if (!option.check()) {
                error(option);
                return false;
            }
        }
        return true;
    }

    /**
     * Support for java.lang.management.RuntimeMXBean.
     *
     * @return space-separated string of VM arguments, not including class name, -jar, or user arguments
     */
    public static String getVmArguments() {
        final StringBuilder sb = new StringBuilder();
        int index = 1;
        boolean needSpace = false;
        final Pointer p = argv;
        argv = savedArgv;
        while (index < argumentStart) {
            String argument = null;
            try {
                argument = getArgumentString(index);
            } catch (Utf8Exception ex) {
            }
            if (argument != null) {
                if (argument.equals("-jar")) {
                    // skip this and jarfile
                    index++;
                } else {
                    if (needSpace) {
                        sb.append(' ');

                    }
                    sb.append(argument);
                    needSpace = true;
                }
            }
            index++;
        }
        argv = p;
        return sb.toString();
    }

    /**
     * Support for {@link VMSupport#initAgentProperties}.
     * @return space separated string of main class and arguments.
     */
    public static String mainClassAndArguments() {
        final StringBuilder sb = new StringBuilder(jarFile() == null ? mainClassName : jarFile());
        for (int i = 0; i < mainClassArguments.length; i++) {
            sb.append(' ');
            sb.append(mainClassArguments[i]);
        }
        return sb.toString();
    }

    public static boolean parseStarting() {
        try {
            int index = 1;
            while (index < argumentStart) {
                final String argument = getArgumentString(index);
                if (argument == null) {
                    index++;
                } else {
                    if (argument.startsWith("-D")) {
                        parseSystemProperty(initialSystemProperties, argument);
                        argv.setWord(index, Word.zero());
                    } else {
                        final Pointer nextArg = argv.getWord(index).asPointer();
                        final VMOption option = findVMOption(nextArg, startingPhaseOptions);
                        if (option == null) {
                            error("unknown VM argument \"" + CString.utf8ToJava(nextArg) + "\"");
                        } else if (!option.parse(nextArg)) {
                            error("parsing of " + argument + " failed");
                        } else if (option.haltsVM()) {
                            earlyVMExitRequested = true;
                        }
                        argv.setWord(index, Word.zero());
                        index++;
                    }
                }
            }
        } catch (Utf8Exception utf8Exception) {
            error("UTF8 problem");
        }
        return checkOptionsForErrors(startingPhaseOptions);
    }

    /**
     * Adds any system properties specified on the command line to a given properties object.
     * The command line properties override any properties in {@code properties} that have
     * the same name.
     *
     * @param properties the object to which the command line properties are added
     */
    public static void addParsedSystemProperties(Properties properties) {
        for (Map.Entry<Object, Object> entry : initialSystemProperties.entrySet()) {
            properties.setProperty((String) entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * Parses a system property from command line argument that starts with "-D" and adds it
     * to a given properties object.
     *
     * @param properties the object to which the command line property extracted from {@code argument} is added
     * @param argument a command line argument that starts with "-D"
     */
    private static void parseSystemProperty(Properties properties, final String argument) {
        final int index = argument.indexOf('=');
        String name;
        String value = "";
        if (index < 0) {
            name = argument.substring(2); // chop off -D
        } else {
            name = argument.substring(2, index); // get the name of the option
            value = argument.substring(index + 1);
        }
        properties.setProperty(name, value);
    }

    public static String mainClassName() {
        return mainClassName;
    }

    public static String[] mainClassArguments() {
        return mainClassArguments;
    }

    private static void parseMainClassArguments(int argStart) throws Utf8Exception {
        int argumentCount = 0;

        for (int i = argStart; i < argc; i++) {
            if (findArgument(i) >= 0) {
                argumentCount++;
            }
        }
        mainClassArguments = new String[argumentCount];
        int mainClassArgumentsIndex = 0;
        for (int i = argStart; i < argc; i++) {
            if (findArgument(i) >= 0) {
                mainClassArguments[mainClassArgumentsIndex++] = getArgumentString(i);
            }
        }
    }

    /**
     * Tries to parse the next available command line argument which specifies the name of the class containing the main
     * method to be run.
     *
     * @param errorIfNotPresent specifies whether the omission of a main class argument is to be considered an
     *            {@linkplain #error(String) error}
     * @return true if the main class name argument was successfully parsed. If so, then it's now available by calling
     *         {@link #mainClassName()}.
     */
    public static boolean parseMain(boolean errorIfNotPresent) {
        try {
            if (jarOption.isPresent()) {
                // the first argument is the first argument to the program
                parseMainClassArguments(argumentStart);
                return true;
            }
            if (argumentStart < argc) {
                // the first argument is the name of the main class
                mainClassName = getArgumentString(argumentStart);
                parseMainClassArguments(argumentStart + 1);
                return mainClassName != null;
            }
            if (errorIfNotPresent) {
                error("no main class specified");
            }
        } catch (Utf8Exception utf8Exception) {
            error("UTF8 problem");
        }
        return false;
    }

    /**
     * Calls the {@link VMOption#beforeExit()} method of each registered VM option.
     */
    public static void beforeExit() {
        for (VMOption option : pristinePhaseOptions) {
            option.beforeExit();
        }
        for (VMOption option : startingPhaseOptions) {
            option.beforeExit();
        }
        if (MaxineVM.isHosted()) {
            for (String argument : VMOption.unmatchedVMArguments()) {
                if (argument != null) {
                    ProgramWarning.message("VM argument not matched by any VM option: " + argument);
                }
            }
        }
    }
}

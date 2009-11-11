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

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.MaxineVM.*;
import com.sun.max.vm.VMOption.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.reference.*;

/**
 * VM options handling.
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public final class VMOptions {

    /**
     * The set of {@linkplain Phase#PRISTINE pristine-phase} VM options built into the boot image.
     */
    private static VMOption[] pristinePhaseOptions = {};

    /**
     * The set of {@linkplain Phase#STARTING starting-phase} VM options built into the boot image.
     */
    private static VMOption[] startingPhaseOptions = {};

    private static Pointer argv;
    private static int argc;
    private static int argumentStart;

    private static boolean earlyVMExitRequested;

    private static String[] mainClassArguments;
    private static String mainClassName;

    /**
     * This is a reference to the initial value of {@link System#props} when the VM starts up.
     * The "magic" in {@link HostObjectAccess#hostToTarget(Object)} will ensure that this map
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
     * An option to {@linkplain GlobalMetrics#report(java.io.PrintStream) report} on all global metrics gathered during execution.
     * TODO: If this option is not enabled, then global metrics should not be gathered.
     */
    private static final VMBooleanXXOption printRuntimeMetrics = register(new VMBooleanXXOption("-XX:-PrintRuntimeMetrics", "Report random metrics gathered during execution.") {
        @Override
        public boolean parseValue(Pointer optionValue) {
            if (getValue()) {
                GlobalMetrics.reset();
            }
            return true;
        }

        @Override
        protected void beforeExit() {
            if (getValue()) {
                GlobalMetrics.report(Log.out);
            }
        }
    }, MaxineVM.Phase.STARTING);

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

        register(new VMBooleanXXOption("-XX:-PrintConfiguration", "Show VM configuration details and exit") {
            @Override
            public boolean parseValue(Pointer optionValue) {
                VMConfiguration.target().print(Log.out, "  ");
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
                VMConfiguration.target().print(Log.out, "  ");
                return true;
            }
        }, MaxineVM.Phase.STARTING);
    }

    private VMOptions() {
    }

    @HOSTED_ONLY
    private static VMOption[] addOption(VMOption[] options, VMOption option, Iterable<VMOption> allOptions) {
        if (option.category() == VMOption.Category.IMPLEMENTATION_SPECIFIC) {
            final int prefixLength = option instanceof VMBooleanXXOption ? "-XX:+".length() : "-XX:".length();
            final String name = option.prefix.substring(prefixLength);
            ProgramError.check(Character.isUpperCase(name.charAt(0)), "Option with \"-XX:\" prefix must start with an upper-case letter: " + option);
        }
        for (VMOption existingOption : allOptions) {
            ProgramError.check(!existingOption.prefix.equals(option.prefix), "VM option prefix is not unique: " + option.prefix);
        }
        return Arrays.append(options, option);
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
        final Iterable<VMOption> allOptions =  Iterables.from(Arrays.append(pristinePhaseOptions, startingPhaseOptions));
        if (phase == MaxineVM.Phase.PRISTINE) {
            pristinePhaseOptions = addOption(pristinePhaseOptions, option, allOptions);
        } else if (phase == MaxineVM.Phase.STARTING) {
            assert !option.consumesNext();
            startingPhaseOptions = addOption(startingPhaseOptions, option, allOptions);
        } else {
            ProgramError.unexpected("VM options for the " + phase + " phase not (yet) supported");
        }
        option.findMatchingArgumentAndParse();
        final Class<T> type = null;
        return StaticLoophole.cast(type, option);
    }

    /**
     * Creates and registers "-XX" VM options for each non-{@code final} {@code static} field
     * in a given class.
     *
     * @param javaClass the java class containing the fields for which VM options are to be created
     */
    @HOSTED_ONLY
    public static void addFieldOptions(Class<?> javaClass) {
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
                    help = null;
                    name = field.getName().replace('_', '-');
                }
                try {
                    addFieldOption(name, field, help);
                } catch (Exception e) {
                    throw ProgramError.unexpected("Error creating VM option for " + field, e);
                }
            }
        }
    }

    @HOSTED_ONLY
    static void setFieldValue(FieldActor fieldActor, Object value) {
        try {
            fieldActor.toJava().set(null, value);
        } catch (Exception e) {
            throw ProgramError.unexpected("Error setting value of " + fieldActor.toJava() + " to " + value, e);
        }
    }

    /**
     * Creates and registers a "-XX" VM option whose value is stored in a given non-{@code final} {@code static} field.
     *
     * @param name the name of the option
     * @param field the field backing the option
     * @param help the help text for the option
     */
    @HOSTED_ONLY
    public static void addFieldOption(String name, Field field, String help) throws IllegalArgumentException, IllegalAccessException {
        MaxineVM.Phase phase = MaxineVM.Phase.STARTING;
        assert Modifier.isStatic(field.getModifiers());
        assert !Modifier.isFinal(field.getModifiers());
        final Class<?> fieldType = field.getType();
        final ClassActor holder = ClassActor.fromJava(field.getDeclaringClass());
        final FieldActor fieldActor = FieldActor.fromJava(field);
        if (MaxineVM.isHosted()) {
            field.setAccessible(true);
        }
        if (fieldType == boolean.class) {
            boolean defaultValue = field.getBoolean(null);
            VMBooleanXXOption option = new VMBooleanXXOption("-XX:" + (defaultValue ? '+' : '-') + name, help) {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    boolean result = super.parseValue(optionValue);
                    if (result) {
                        if (MaxineVM.isHosted()) {
                            setFieldValue(fieldActor, getValue());
                        } else {
                            Reference.fromJava(holder.staticTuple()).writeBoolean(fieldActor.offset(), getValue());
                        }
                        return true;
                    }
                    return false;
                }
            };
            register(option, phase);
        } else if (fieldType == int.class) {
            int defaultValue = field.getInt(null);
            VMIntOption option = new VMIntOption("-XX:" + name + "=", defaultValue, help) {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    boolean result = super.parseValue(optionValue);
                    if (result) {
                        if (MaxineVM.isHosted()) {
                            setFieldValue(fieldActor, getValue());
                        } else {
                            Reference.fromJava(holder.staticTuple()).writeInt(fieldActor.offset(), getValue());
                        }
                        return true;
                    }
                    return result;
                }
            };
            register(option, phase);
        } else if (fieldType == float.class) {
            float defaultValue = field.getFloat(null);
            VMFloatOption option = new VMFloatOption("-XX:" + name + "=", defaultValue, help) {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    boolean result = super.parseValue(optionValue);
                    if (result) {
                        if (MaxineVM.isHosted()) {
                            setFieldValue(fieldActor, getValue());
                        } else {
                            Reference.fromJava(holder.staticTuple()).writeFloat(fieldActor.offset(), getValue());
                        }
                        return true;
                    }
                    return result;
                }
            };
            register(option, phase);
        } else if (fieldType == String.class) {
            String defaultValue = (String) field.get(null);
            VMStringOption option = new VMStringOption("-XX:" + name + "=", false, defaultValue, help) {
                @Override
                public boolean parseValue(Pointer optionValue) {
                    boolean result = super.parseValue(optionValue);
                    if (result) {
                        if (MaxineVM.isHosted()) {
                            setFieldValue(fieldActor, getValue());
                        } else {
                            Reference.fromJava(holder.staticTuple()).writeReference(fieldActor.offset(), Reference.fromJava(getValue()));
                        }
                        return true;
                    }
                    return result;
                }
            };
            register(option, phase);
        } else {
            throw new RuntimeException("Field type unsupported by VM options");
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

    private static void printOptions(VMOption[] options, Category category) {
        for (VMOption option : options) {
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
            printOptions(pristinePhaseOptions, category);
            printOptions(startingPhaseOptions, category);
        }
        if (category == Category.NON_STANDARD) {
            Log.println("Non-standard options:");
            printOptions(pristinePhaseOptions, category);
            printOptions(startingPhaseOptions, category);
        }
        if (category == Category.IMPLEMENTATION_SPECIFIC) {
            Log.println("Maxine options:");
            printOptions(pristinePhaseOptions, category);
            printOptions(startingPhaseOptions, category);
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
     * Gets all the registered VM options as an {@code IterableWithLength} object.
     *
     * @return all the registered VM options as an {@code IterableWithLength} object
     */
    public static IterableWithLength<VMOption> allOptions() {
        return Iterables.join(Arrays.iterable(pristinePhaseOptions), Arrays.iterable(startingPhaseOptions));
    }

    public static boolean parsePristine(int initialArgc, Pointer initialArgv) {
        argv = initialArgv;
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

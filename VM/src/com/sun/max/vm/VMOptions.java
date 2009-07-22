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
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.VMOption.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;

/**
 * Basic VM argument handling.
 * We have to do some argument processing here, e.g. -Xms, for anything that affects
 * the bootstrap process. Since we have to work at the CString level, we only handle the
 * essential arguments and postpone everything else until the main thread body.
 * We null out the arguments that we process here so that they can easily be ignored
 * in subsequent passes.
 *
 * @author Mick Jordan
 * @author Bernd Mathiske
 * @author Ben L. Titzer
 */
public final class VMOptions {

    private static final int HELP_INDENT = 32;

    /**
     * A comparator for sorted a set of {@link VMOption}s in reverse lexicographic order of their
     * {@linkplain VMOption#prefix prefixes}. This means that suboptions precede their parent option
     * where a suboption is an option whose prefix starts with but is not equal to the parent's prefix.
     */
    @PROTOTYPE_ONLY
    private static final Comparator<VMOption> VMOPTION_SORTER = new Comparator<VMOption>() {
        public int compare(VMOption o1, VMOption o2) {
            return o2.prefix.compareTo(o1.prefix);
        }
    };

    /**
     * Used to collect and sort VM options as they are declared.
     */
    @PROTOTYPE_ONLY
    private static final SortedSet<VMOption> pristinePhaseOptionsSet = new TreeSet<VMOption>(VMOPTION_SORTER);

    /**
     * Used to collect and sort VM options as they are declared.
     */
    @PROTOTYPE_ONLY
    private static final SortedSet<VMOption> startingPhaseOptionsSet = new TreeSet<VMOption>(VMOPTION_SORTER);

    private static VMOption[] pristinePhaseOptions;
    private static VMOption[] startingPhaseOptions;

    private static final VMStringOption jarOption = register(new VMStringOption("-jar", true, null, "Executes main class from jar file.") {
        @Override
        public boolean isLastOption() {
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    /**
     * An option to {@linkplain GlobalMetrics#report(java.io.PrintStream) report} on all global metrics gathered during execution.
     * TODO: If this option is not enabled, then the global metrics should not be gathered.
     */
    private static final VMBooleanXXOption printMetrics = register(new VMBooleanXXOption("-XX:-PrintMetrics", "Report random metrics gathered during execution.") {
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

    private static final VMOption verboseOption = register(new VMOption("-verbose ", "Enables all verbose options."), MaxineVM.Phase.PRISTINE);

    private static final VMIntOption traceLevelOption = register(new VMIntOption("-XX:TraceLevel=", 0, "Enables tracing output at the specified level.") {
        @Override
        public boolean parseValue(Pointer optionValue) {
            Trace.on(getValue());
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    private static final VMBooleanXXOption printConfiguration = register(new VMBooleanXXOption("-XX:-PrintConfiguration", "Shows VM configuration details and exits."), MaxineVM.Phase.STARTING);
    private static final VMBooleanXXOption showConfiguration = register(new VMBooleanXXOption("-XX:-ShowConfiguration", "Shows VM configuration details and continues."), MaxineVM.Phase.STARTING);

    private static Pointer argv;
    private static int argc;
    private static int argumentStart;

    private static boolean earlyVMExitRequested;

    private static String[] mainClassArguments;
    private static String mainClassName;

    private VMOptions() {
    }

    public static void printHelpForOption(String prefix, String value, String help) {
        Log.print("    ");
        Log.print(prefix);
        Log.print(value);
        Log.print(" ");
        int column = 5 + prefix.length() + value.length();
        for (; column < HELP_INDENT; column++) {
            Log.print(' ');
        }
        if (help != null) {
            // reformat the help text by wrapping the lines after column 72.
            // Strings.formatParagraphs() can't be used because allocation may not work here
            for (int j = 0; j < help.length(); j++) {
                final char ch = help.charAt(j);
                if (column > 72 && (ch == ' ' || ch == '\t')) {
                    Log.println();
                    for (int k = 0; k < HELP_INDENT; k++) {
                        Log.print(' ');
                    }
                    column = HELP_INDENT;
                } else {
                    Log.print(ch);
                    column++;
                }
            }
        }
        Log.println();
    }

    @PROTOTYPE_ONLY
    private static VMOption[] addOption(SortedSet<VMOption> options, VMOption option, Iterable<VMOption> allOptions) {
        if (option.category() == VMOption.Category.IMPLEMENTATION_SPECIFIC) {
            final int prefixLength = option instanceof VMBooleanXXOption ? "-XX:+".length() : "-XX:".length();
            final String name = option.prefix.substring(prefixLength);
            ProgramError.check(Character.isUpperCase(name.charAt(0)), "Option with \"-XX:\" prefix must start with an upper-case letter: " + option);
        }
        for (VMOption existingOption : allOptions) {
            ProgramError.check(!existingOption.prefix.equals(option.prefix), "VM option prefix is not unique: " + option.prefix);
            if (option.prefix.startsWith(existingOption.prefix)) {
                existingOption.addSuboption(option);
            } else if (existingOption.prefix.startsWith(option.prefix)) {
                option.addSuboption(existingOption);
            }
        }
        options.add(option);
        return options.toArray(new VMOption[options.size()]);
    }

    /**
     * Registers a given VM option in the global option registry that is used to match command
     * line arguments passed to the VM at runtime.
     *
     * @param option a VM option
     * @param phase the VM phase during which the option should be parsed
     * @return the {@code option} object
     */
    @PROTOTYPE_ONLY
    public static <T extends VMOption> T register(VMOption option, MaxineVM.Phase phase) {
        assert phase != null;
        final Iterable<VMOption> allOptions = Iterables.join(pristinePhaseOptionsSet, startingPhaseOptionsSet);
        if (phase == MaxineVM.Phase.PRISTINE) {
            pristinePhaseOptions = addOption(pristinePhaseOptionsSet, option, allOptions);
        } else if (phase == MaxineVM.Phase.STARTING) {
            assert !option.consumesNext();
            startingPhaseOptions = addOption(startingPhaseOptionsSet, option, allOptions);
        } else {
            ProgramError.unexpected("VM options for the " + phase + " phase not (yet) supported");
        }
        option.findMatchingArgumentAndParse();
        final Class<T> type = null;
        return StaticLoophole.cast(type, option);
    }

    private static void printOptions(VMOption[] options, String label, Category category) {
        for (VMOption option : options) {
            if (option.category() == category) {
                option.printHelp();
            }
        }
    }

    private static void printOptions(String label, Category category) {
        if (label != null) {
            Log.println();
            Log.println(label);
        }
        printOptions(pristinePhaseOptions, label, category);
        printOptions(startingPhaseOptions, label, category);
    }

    public static void printUsage() {
        Log.println("Usage: maxvm [-options] [class | -jar jarfile]  [args...]");
        Log.println("where options include:");

        printOptions(null, Category.STANDARD);
        printOptions("Non-standard options:", Category.NON_STANDARD);
        printOptions("Maxine options:", Category.IMPLEMENTATION_SPECIFIC);
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
        printUsage();
        MaxineVM.setExitCode(1);
    }

    protected static void error(VMOption option) {
        earlyVMExitRequested = true;
        Log.print("Error while parsing ");
        Log.print(option.toString());
        Log.print(": ");
        option.printErrorMessage();
        Log.println();
        printUsage();
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
            }
            argv.setWord(index, Word.zero());
            argv.setWord(index + 1, Word.zero());
            nextIndex = index + 2;
        } else {
            // otherwise ask the option to parse itself
            if (!option.parse(argument)) {
                error(option.toString());
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

    /**
     * This is a reference to the initial value of {@link System#props} when the VM starts up.
     * The "magic" in {@link HostObjectAccess#hostToTarget(Object)} will ensure that this map
     * only has the properties from the host specified by {@link JDKInterceptor#REMEMBERED_PROPERTY_NAMES}.
     * The system properties parsed on the command line are stored in this map.
     * This is required so that they are available before the System class is initialized.
     */
    public static final Properties initialSystemProperties = System.getProperties();

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
                        }
                        argv.setWord(index, Word.zero());
                        index++;
                    }
                }
            }
        } catch (Utf8Exception utf8Exception) {
            error("UTF8 problem");
        }
        final boolean noErrorFound = checkOptionsForErrors(startingPhaseOptions);
        if (noErrorFound) {
            if (printConfiguration.getValue() || showConfiguration.getValue()) {
                final VMConfiguration vm = VMConfiguration.target();
                Log.println("VM Configuration:");
                Log.println("  Build level: " + vm.buildLevel());
                Log.println("  Platform: " + vm.platform());
                for (VMScheme vmScheme : vm.vmSchemes()) {
                    final String specification = vmScheme.specification().getSimpleName();
                    Log.println("  " + specification.replace("Scheme", " scheme") + ": " + vmScheme.getClass().getName());
                }
                if (printConfiguration.getValue()) {
                    earlyVMExitRequested = true;
                }
            }
        }
        return noErrorFound;
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
     * Parse a size specification nX, where X := {K, M, G, T, P, k, m, g, t, p}.
     *
     * For backwards compatibility with HotSpot,
     * lower case letters shall have the same respective meaning as the upper case ones,
     * even though their non-colloquialized definitions would suggest otherwise.
     *
     * @param p a pointer to the C string
     * @param length the maximum length of the C string
     * @param startIndex the starting index into the C string pointed to by the first argument
     * @return the scaled value or -1 if error
     */
    protected static long parseScaledValue(Pointer p, Size length, int startIndex) {
        long result = 0L;
        boolean done = false;
        int index = startIndex;
        while (index < length.toInt()) {
            if (done) {
                // having any additional characters is an error
                return -1L;
            }
            final int character = CString.getByte(p, length, Offset.fromInt(index));
            index++;
            if ('0' <= character && character <= '9') {
                result *= 10;
                result += character - '0';
            } else {
                done = true;
                switch (character) {
                    case 'K':
                    case 'k': {
                        result *= Longs.K;
                        break;
                    }
                    case 'M':
                    case 'm': {
                        result *= Longs.M;
                        break;
                    }
                    case 'G':
                    case 'g': {
                        result *= Longs.G;
                        break;
                    }
                    case 'T':
                    case 't': {
                        result *= Longs.T;
                        break;
                    }
                    case 'P':
                    case 'p': {
                        result *= Longs.P;
                        break;
                    }
                    default: {
                        // illegal character
                        return -1L;
                    }
                }
            }
        }
        return result;
    }

    protected static int parseUnsignedInt(String string) {
        int result = 0;
        for (int i = 0; i < string.length(); i++) {
            final char ch = string.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result *= 10;
                result += string.charAt(i) - '0';
            } else {
                return -1;
            }
        }
        return result;
    }

    protected static long parseUnsignedLong(String string) {
        long result = 0L;
        for (int i = 0; i < string.length(); i++) {
            final char ch = string.charAt(i);
            if (ch >= '0' && ch <= '9') {
                result *= 10L;
                result += string.charAt(i) - '0';
            } else {
                return -1L;
            }
        }
        return result;
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
        if (MaxineVM.isPrototyping()) {
            for (String argument : VMOption.unmatchedVMArguments()) {
                if (argument != null) {
                    ProgramWarning.message("VM argument not matched by any VM option: " + argument);
                }
            }
        }
    }
}

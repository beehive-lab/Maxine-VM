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
package test.com.sun.max.vm.compiler.c1x;

import static test.com.sun.max.vm.compiler.c1x.C1XTest.PatternType.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.xir.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

/**
 * A simple harness to run the C1X compiler and test it in various modes, without
 * needing JUnit.
 *
 * @author Ben L. Titzer
 */
public class C1XTest {

    private static final OptionSet options = new OptionSet(false);

    private static final Option<String> searchCpOption = options.newStringOption("search-cp", null,
        "The restricted class path to use when matching compilation_specs. This must be a " +
        "subset of the classpath (i.e. the classpath specified to the underlying JVM running this process).");
    private static final Option<Integer> traceOption = options.newIntegerOption("trace", 0,
        "Set the tracing level of the Maxine VM and runtime.");
    private static final Option<Integer> verboseOption = options.newIntegerOption("verbose", 1,
        "Set the verbosity level of the testing framework.");
    private static final Option<Boolean> printBailoutOption = options.newBooleanOption("print-bailout", false,
        "Print bailout exceptions.");
    private static final Option<Boolean> printBailoutSizeOption = options.newBooleanOption("print-bailout-size", false,
        "Print the size of bailed out methods, which helps choosing the simplest failure case for debugging..");
    private static final Option<File> outFileOption = options.newFileOption("o", (File) null,
        "A file to which output should be sent. If not specified, then output is sent to stdout.");
    private static final Option<Boolean> nowarnOption = options.newBooleanOption("nowarn", false,
        "Do not print ClassNotFoundException warnings.");
    private static final Option<Boolean> clinitOption = options.newBooleanOption("clinit", true,
        "Compile class initializer (<clinit>) methods");
    private static final Option<Boolean> failFastOption = options.newBooleanOption("fail-fast", true,
        "Stop compilation upon the first bailout.");
    private static final Option<String> compilerOption = options.newStringOption("compiler-name", "c1x",
        "Select the compiler; boot,jit,opt,c1x");
    private static final Option<Boolean> compileTargetMethod = options.newBooleanOption("compile-target-method", false,
        "Use the C1X compiler to compile all the way to a TargetMethod instance.");
    private static final Option<Integer> timingOption = options.newIntegerOption("timing", 0,
        "Perform the specified number of timing runs.");
    private static final Option<Boolean> c1xOptionsOption = options.newBooleanOption("c1x-options", false,
        "Print settings of C1XOptions.");
    private static final Option<Boolean> scatterOption = options.newBooleanOption("scatter-data", false,
        "Report timings in X\\tY\\n format for easy cut and paste to scatter plot.");
    private static final Option<Boolean> averageOption = options.newBooleanOption("average", true,
        "Report only the average compilation speed.");
    private static final Option<Boolean> spreadsheetOption = options.newBooleanOption("spreadsheet", false,
        "Report timing information in a spreadsheet-friendly format with tab characters.");
    private static final Option<Long> longerThanOption = options.newLongOption("longer-than", 0L,
        "Report only the compilation times that took longer than the specified number of nanoseconds.");
    private static final Option<Long> slowerThanOption = options.newLongOption("slower-than", 10000000000L,
        "Report only the compilation speeds that were slower than the specified number of bytes per second.");
    private static final Option<Long> biggerThanOption = options.newLongOption("bigger-than", 0L,
        "Report only the compilation speeds for methods larger than the specified threshold.");
    private static final Option<Integer> warmupOption = options.newIntegerOption("warmup", 0,
        "Set the number of warmup runs to execute before initiating the timed run.");
    private static final Option<Boolean> resetMetricsOption = options.newBooleanOption("reset-metrics", true,
        "Reset the metrics before each timing run.");
    private static final Option<Boolean> helpOption = options.newBooleanOption("help", false,
        "Show help message and exit.");
    private static final Option<Integer> c1xOptLevel = options.newIntegerOption("C1X:OptLevel", -1,
        "Set the overall optimization level of C1X (-1 to use default settings)");
    private static final Option<List<String>> metricsOption = options.newStringListOption("print-metrics", new String[0],
        "A list of metrics from the C1XMetrics class to print.");

    static {
        // add all the fields from C1XOptions as options
        options.addFieldOptions(C1XOptions.class, "C1X");
    }

    private static final List<Timing> timings = new ArrayList<Timing>();

    private static PrintStream out = System.out;
    private static int totalBytes;
    private static int totalInlinedBytes;
    private static int totalInstrs;
    private static int totalFailures;
    private static long totalNs;
    private static long cumulNs;
    private static long lastRunNs;
    private static final double ONE_BILLION = 1000000000;

    public static void main(String[] args) {
        // set the default optimization level before parsing options
        options.parseArguments(args);
        Integer optLevel = c1xOptLevel.getValue();
        if (optLevel >= 0) {
            C1XOptions.setOptimizationLevel(optLevel);
        }

        options.setValuesAgain();
        reportC1XOptions();
        final String[] arguments = options.getArguments();

        if (helpOption.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }

        ClassMethodActor.hostedVerificationDisabled = true;

        if (outFileOption.getValue() != null) {
            try {
                out = new PrintStream(new FileOutputStream(outFileOption.getValue()));
                Trace.setStream(out);
            } catch (FileNotFoundException e) {
                System.err.println("Could not open " + outFileOption.getValue() + " for writing: " + e);
                System.exit(1);
            }
        }

        if (timingOption.getValue() > 0) {
            verboseOption.setValue(0);
            failFastOption.setValue(false);
            printBailoutOption.setValue(false);
        }

        Trace.on(traceOption.getValue());

        // create the prototype
        if (verboseOption.getValue() > 0) {
            out.print("Creating Java prototype... ");
        }
        new PrototypeGenerator(options).createJavaPrototype(false);
        if (verboseOption.getValue() > 0) {
            out.println("done");
        }

        // create MaxineRuntime
        VMConfiguration configuration = VMConfiguration.target();
        final RuntimeCompilerScheme compilerScheme;

        String compilerName = compilerOption.getValue();
        if (compilerName.equals("c1x")) {
            compilerScheme = C1XCompilerScheme.create(configuration);
        } else if (compilerName.equals("boot")) {
            configuration.initializeSchemes(MaxineVM.Phase.COMPILING);
            compilerScheme = configuration.bootCompilerScheme();
        } else if (compilerName.equals("jit")) {
            configuration.initializeSchemes(MaxineVM.Phase.COMPILING);
            compilerScheme = configuration.jitCompilerScheme();
        } else if (compilerName.equals("opt")) {
            configuration.initializeSchemes(MaxineVM.Phase.COMPILING);
            compilerScheme = configuration.optCompilerScheme();
        } else {
            throw ProgramError.unexpected("Unknown compiler: " + compilerName);
        }

        final List<MethodActor> methods = findMethodsToCompile(arguments);
        final ProgressPrinter progress = new ProgressPrinter(out, methods.size(), verboseOption.getValue(), false);

        MaxineVM.usingTarget(new Runnable() {
            public void run() {
                doWarmup(compilerScheme, methods);
                doCompile(compilerScheme, methods, progress);
            }
        });

        if (verboseOption.getValue() > 0) {
            progress.report();
        }
        reportTimings();
        reportMetrics();
    }

    private static void doCompile(RuntimeCompilerScheme compilerScheme, List<MethodActor> methods, ProgressPrinter progress) {
        if (timingOption.getValue() > 0) {
            // do a timing run
            int max = timingOption.getValue();
            out.println("Timing...");
            for (int i = 0; i < max; i++) {
                if (i > 0 && resetMetricsOption.getValue()) {
                    resetMetrics();
                }
                doTimingRun(compilerScheme, methods);
                // only aggressively resolve on the first run
                C1XOptions.AggressivelyResolveCPEs = false;
            }
        } else {
            // compile all the methods and report progress
            for (MethodActor methodActor : methods) {
                progress.begin(methodActor.toString());
                boolean result;
                if (compilerScheme instanceof C1XCompilerScheme && !compileTargetMethod.getValue()) {
                    C1XCompilerScheme c1x = (C1XCompilerScheme) compilerScheme;
                    result = compile(c1x.getCompiler(), c1x.getRuntime(), c1x.getXirGenerator(), methodActor, printBailoutOption.getValue(), false);
                } else {
                    result = compile(compilerScheme, methodActor, printBailoutOption.getValue(), false);
                }
                if (result) {
                    progress.pass();
                } else {
                    progress.fail("failed");
                    if (failFastOption.getValue()) {
                        out.println("");
                        break;
                    }
                }
            }
        }
    }

    private static void resetMetrics() {
        for (Field f : C1XMetrics.class.getFields()) {
            if (f.getType() == int.class) {
                try {
                    f.set(null, 0);
                } catch (IllegalAccessException e) {
                    // do nothing.
                }
            }
        }
    }

    private static void doTimingRun(RuntimeCompilerScheme compilerScheme, List<MethodActor> methods) {
        C1XTimers.reset();
        long start = System.nanoTime();
        totalBytes = 0;
        totalInlinedBytes = 0;
        totalNs = 0;
        totalInstrs = 0;
        totalFailures = 0;
        if (compilerScheme instanceof C1XCompilerScheme && !compileTargetMethod.getValue()) {
            C1XCompilerScheme c1x = (C1XCompilerScheme) compilerScheme;
            for (MethodActor methodActor : methods) {
                if (!compile(c1x.getCompiler(), c1x.getRuntime(), c1x.getXirGenerator(), methodActor, false, true)) {
                    totalFailures++;
                }
            }
        } else {
            for (MethodActor methodActor : methods) {
                if (!compile(compilerScheme, methodActor, false, true)) {
                    totalFailures++;
                }
            }
        }
        cumulNs += totalNs;
        lastRunNs = System.nanoTime() - start;
        reportAverage();

        if (C1XOptions.PrintTimers) {
            C1XTimers.print();
        }
    }

    private static void doWarmup(RuntimeCompilerScheme compilerScheme, List<MethodActor> methods) {
        // compile all the methods in the list some number of times first to warmup the host VM
        int max = warmupOption.getValue();
        if (max > 0) {
            out.print("Warming up");
            for (int i = 0; i < max; i++) {
                out.print(".");
                out.flush();
                if (compilerScheme instanceof C1XCompilerScheme && !compileTargetMethod.getValue()) {
                    C1XCompilerScheme c1x = (C1XCompilerScheme) compilerScheme;
                    for (MethodActor actor : methods) {
                        compile(c1x.getCompiler(), c1x.getRuntime(), c1x.getXirGenerator(), actor, false, false);
                    }
                } else {
                    for (MethodActor actor : methods) {
                        compile(compilerScheme, actor, false, false);
                    }
                }
            }
            out.println();
        }
    }

    private static boolean compile(RuntimeCompilerScheme compilerScheme, MethodActor method, boolean printBailout, boolean timing) {
        // compile a single method
        ClassMethodActor classMethodActor = (ClassMethodActor) method;
        Throwable thrown = null;
        final long startNs = System.nanoTime();
        try {
            compilerScheme.compile(classMethodActor);
        } catch (Throwable t) {
            thrown = t;
        }
        if (timing && thrown == null) {
            long timeNs = System.nanoTime() - startNs;
            recordTime(method, 0, 0, timeNs);
        }
        if (printBailout && thrown != null) {
            out.println("");
            out.println(method);
            if (printBailoutSizeOption.getValue()) {
                out.println(classMethodActor.codeAttribute().code().length + " bytes");
            }
            thrown.printStackTrace();
        }

        return thrown == null;
    }

    private static boolean compile(CiCompiler compiler, MaxRiRuntime runtime, RiXirGenerator xirGenerator, MethodActor method, boolean printBailout, boolean timing) {
        // compile a single method
        RiMethod riMethod = runtime.getRiMethod((ClassMethodActor) method);
        final long startNs = System.nanoTime();
        CiResult result = compiler.compileMethod(riMethod, xirGenerator);
        if (timing && result.bailout() == null) {
            long timeNs = System.nanoTime() - startNs;
            recordTime(method, result.statistics().byteCount, result.statistics().nodeCount, timeNs);
        }
        if (printBailout && result.bailout() != null) {
            out.println("");
            out.println(method);
            if (printBailoutSizeOption.getValue()) {
                out.println(result.statistics().byteCount + " bytes");
            }
            result.bailout().printStackTrace();
        }

        return result.bailout() == null;
    }

    private static boolean isCompilable(MethodActor method) {
        return method instanceof ClassMethodActor && !method.isAbstract() && !method.isNative() && !method.isBuiltin() && !method.isIntrinsic();
    }

    enum PatternType {
        EXACT("matching") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.equals(pattern);
            }
        },
        PREFIX("starting with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.startsWith(pattern);
            }
        },
        SUFFIX("ending with") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.endsWith(pattern);
            }
        },
        CONTAINS("containing") {
            @Override
            public boolean matches(String input, String pattern) {
                return input.contains(pattern);
            }
        };

        final String relationship;

        private PatternType(String relationship) {
            this.relationship = relationship;
        }

        abstract boolean matches(String input, String pattern);

        @Override
        public String toString() {
            return relationship;
        }
    }

    static class PatternMatcher {
        final String pattern;
        // 1: exact, 2: prefix, 3: suffix, 4: substring
        final PatternType type;

        public PatternMatcher(String pattern) {
            if (pattern.startsWith("^") && pattern.endsWith("^") && pattern.length() != 1) {
                this.type = EXACT;
                this.pattern = pattern.substring(1, pattern.length() - 1);
            } else if (pattern.startsWith("^")) {
                this.type = PREFIX;
                this.pattern = pattern.length() == 1 ? "" : pattern.substring(1);
            } else if (pattern.endsWith("^")) {
                this.type = SUFFIX;
                this.pattern = pattern.substring(0, pattern.length() - 1);
            } else {
                this.type = CONTAINS;
                this.pattern = pattern;
            }
        }

        boolean matches(String input) {
            return type.matches(input, pattern);
        }
    }

    private static List<MethodActor> findMethodsToCompile(String[] arguments) {
        String searchCp = searchCpOption.getValue();
        final Classpath classpath = searchCp == null || searchCp.length() == 0 ? Classpath.fromSystem() : new Classpath(searchCp);

        final List<MethodActor> methods = new ArrayList<MethodActor>();
        final Set<String> exclusions = new HashSet<String>();

        for (int i = 0; i != arguments.length; ++i) {
            final String argument = arguments[i];
            if (argument.startsWith("!")) {
                exclusions.add(argument.substring(1));
                arguments[i] = null;
            }
        }

        for (int i = 0; i != arguments.length; ++i) {
            final String argument = arguments[i];
            if (argument == null) {
                continue;
            }
            final int colonIndex = argument.indexOf(':');
            final PatternMatcher classNamePattern = new PatternMatcher(colonIndex == -1 ? argument : argument.substring(0, colonIndex));

            // search for matching classes on the class path
            final AppendableSequence<String> matchingClasses = new ArrayListSequence<String>();
            if (verboseOption.getValue() > 0) {
                out.print("Classes " + classNamePattern.type + " '" + classNamePattern.pattern + "'... ");
            }

            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (!className.endsWith("package-info")) {
                        if (classNamePattern.matches(className)) {
                            for (String exclusion : exclusions) {
                                if (className.contains(exclusion)) {
                                    return true;
                                }
                            }
                            matchingClasses.append(className);
                        }
                    }
                    return true;
                }
            }.run(classpath);

            if (verboseOption.getValue() > 0) {
                out.println(matchingClasses.length());
            }

            final int startMethods = methods.size();
            if (verboseOption.getValue() > 0) {
                out.print("Gathering methods");
            }
            // for all found classes, search for matching methods
            for (String className : matchingClasses) {
                try {
                    Class<?> javaClass = null;
                    try {
                        javaClass = Class.forName(className, false, C1XTest.class.getClassLoader());
                    } catch (NoClassDefFoundError noClassDefFoundError) {
                        throw new ClassNotFoundException(className, noClassDefFoundError);
                    }
                    final ClassActor classActor = getClassActorNonfatal(javaClass);
                    if (classActor == null) {
                        continue;
                    }

                    if (colonIndex == -1) {
                        // Class only: compile all methods in class
                        for (MethodActor actor : classActor.localStaticMethodActors()) {
                            if (clinitOption.getValue() || actor != classActor.clinit) {
                                addMethod(methods, actor, exclusions);
                            }
                        }
                        for (MethodActor methodActor : classActor.localVirtualMethodActors()) {
                            addMethod(methods, methodActor, exclusions);
                        }
                    } else {
                        // a method pattern was specified, find matching methods
                        final int parenIndex = argument.indexOf('(', colonIndex + 1);
                        final PatternMatcher methodNamePattern;
                        final SignatureDescriptor signature;
                        if (parenIndex == -1) {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1));
                            signature = null;
                        } else {
                            methodNamePattern = new PatternMatcher(argument.substring(colonIndex + 1, parenIndex));
                            signature = SignatureDescriptor.create(argument.substring(parenIndex));
                        }
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localStaticMethodActors(), exclusions);
                        addMatchingMethods(methods, classActor, methodNamePattern, signature, classActor.localVirtualMethodActors(), exclusions);
                    }
                } catch (ClassNotFoundException classNotFoundException) {
                    if (!nowarnOption.getValue()) {
                        ProgramWarning.message(classNotFoundException.toString() + (classNotFoundException.getCause() == null ? "" : " (cause: " + classNotFoundException.getCause() + ")"));
                    }
                }
            }
            if (verboseOption.getValue() > 0) {
                out.println(" " + (methods.size() - startMethods));
            }
        }
        return methods;
    }

    private static void addMethod(List<MethodActor> methods, MethodActor methodActor, Set<String> exclusions) {
        for (String exclusion : exclusions) {
            if (methodActor.name.string.contains(exclusion)) {
                return;
            }
        }
        if (isCompilable(methodActor)) {
            if (C1XOptions.CanonicalizeFoldableMethods && Actor.isDeclaredFoldable(methodActor.flags())) {
                final Method method = methodActor.toJava();
                assert method != null;
                C1XIntrinsic.registerFoldableMethod(C1XCompilerScheme.globalRuntime.getRiMethod((ClassMethodActor) methodActor), method);
            }
            methods.add(methodActor);
            if ((methods.size() % 1000) == 0 && verboseOption.getValue() >= 1) {
                out.print('.');
            }
        }
    }

    private static ClassActor getClassActorNonfatal(Class<?> javaClass) {
        ClassActor classActor = null;
        try {
            classActor = ClassActor.fromJava(javaClass);
        } catch (Throwable t) {
            // do nothing.
        }
        return classActor;
    }

    private static void addMatchingMethods(final List<MethodActor> methods, final ClassActor classActor, final PatternMatcher methodNamePattern, final SignatureDescriptor signature, MethodActor[] methodActors, Set<String> exclusions) {
        for (final MethodActor method : methodActors) {
            if (methodNamePattern.matches(method.name.toString())) {
                final SignatureDescriptor methodSignature = method.descriptor();
                if (signature == null || signature.equals(methodSignature)) {
                    addMethod(methods, method, exclusions);
                }
            }
        }
    }

    private static void recordTime(MethodActor method, int inlinedBytes, int instructions, long ns) {
        if (!averageOption.getValue()) {
            timings.add(new Timing((ClassMethodActor) method, instructions, ns));
        }
        totalBytes += ((ClassMethodActor) method).originalCodeAttribute().code().length;
        totalInlinedBytes += inlinedBytes;
        totalInstrs += instructions;
        totalNs += ns;
    }

    private static void reportTimings() {
        if (C1XOptions.PrintTimers) {
            C1XTimers.print();
        }

        if (timingOption.getValue() > 0 && !averageOption.getValue()) {
            long longerThan = longerThanOption.getValue();
            long slowerThan = slowerThanOption.getValue();
            long biggerThan = biggerThanOption.getValue();
            for (Timing timing : timings) {
                final MethodActor method = timing.classMethodActor;
                final long ns = timing.nanoSeconds;
                final int bytecodes = timing.bytecodes();
                final double bcps = timing.bytecodesPerSecond();
                final double ips = timing.instructionsPerSecond();
                if (ns >= longerThan && bcps <= slowerThan && bytecodes >= biggerThan) {
                    if (scatterOption.getValue()) {
                        out.print(bytecodes);
                        out.print('\t');
                        out.print(ns);
                    } else {
                        out.print(Strings.padLengthWithSpaces("#" + timing.number, 6));
                        out.print(Strings.padLengthWithSpaces(method.toString(), 80) + ": \n\t\t");
                        out.print(Strings.padLengthWithSpaces(6, bytecodes + " bytes   "));
                        out.print(Strings.padLengthWithSpaces(13, ns + " ns "));
                        out.print(Strings.padLengthWithSpaces(20, formatDouble(bcps, 12, 2) + " bytes/s"));
                        out.print(Strings.padLengthWithSpaces(20, formatDouble(ips, 12, 2) + " insts/s"));
                    }
                    out.println();
                }
            }
        }
    }

    private static void reportAverage() {
        double totalBcps = ONE_BILLION * (totalBytes / (double) totalNs);
        double totalIBcps = ONE_BILLION * (totalInlinedBytes / (double) totalNs);
        double totalIps = ONE_BILLION * (totalInstrs / (double) totalNs);
        double seconds = lastRunNs / ONE_BILLION;
        String secs = formatDouble(seconds, 14, 6);
        String bcps = formatDouble(totalBcps, 12, 2);
        String ibcps = formatDouble(totalIBcps, 12, 2);
        String ips = formatDouble(totalIps, 12, 2);
        if (!spreadsheetOption.getValue()) {
            out.print("Time: " + secs + " seconds   ");
            out.print(bcps + " bytes/s   ");
            if (totalIBcps > 0) {
                out.print(ibcps + " bytes/s   ");
            }
            if (totalIps > 0) {
                out.print(ips + " insts/s");
            }
            if (totalFailures > 0) {
                out.print("  (" + totalFailures + " failures)   ");
            }
            out.print(totalBytes + " total bytes   ");
            out.println();
        } else {
            out.print(seconds + "\t");
            out.print(totalBcps + "\t");
            out.print(totalIBcps + "\t");
            out.print(totalIps + "\t");
            out.print(totalBytes + "\t");
            if (totalFailures > 0) {
                out.print("\t(" + totalFailures + " failures)");
            }
            out.println();
        }
    }

    private static String formatDouble(double val, int width, int places) {
        return Strings.padLengthWithSpaces(width, Strings.fixedDouble(val, places));
    }

    private static void reportMetrics() {
        if (C1XOptions.PrintMetrics) {
            printClassFields(C1XMetrics.class);
        }
        List<String> metrics = metricsOption.getValue();
        if (metrics.size() > 0) {
            for (String s : metrics) {
                out.print(s + "\t");
            }
            out.println();
            for (String s : metrics) {
                try {
                    printField(C1XMetrics.class.getDeclaredField(s), true);
                } catch (NoSuchFieldException e) {
                    out.println("-----");
                }
            }
            out.println();
        }
    }

    private static double averageTime() {
        return (cumulNs / (double) timingOption.getValue()) / ONE_BILLION;
    }

    private static void reportC1XOptions() {
        if (c1xOptionsOption.getValue()) {
            printClassFields(C1XOptions.class);
        }
    }

    private static String printMap(Map m) {
        StringBuilder sb = new StringBuilder();

        List<String> keys = new ArrayList<String>();
        for (Object key : m.keySet()) {
            keys.add((String) key);
        }
        Collections.sort(keys);

        for (String key : keys) {
            sb.append(key);
            sb.append("\t");
            sb.append(m.get(key));
            sb.append("\n");
        }

        return sb.toString();
    }

    private static void printClassFields(Class<?> javaClass) {
        final String className = javaClass.getSimpleName();
        out.println(className + " {");
        for (final Field field : javaClass.getFields()) {
            printField(field, false);
        }
        out.println("}");
    }

    private static void printField(final Field field, boolean tabbed) {
        final String fieldName = Strings.padLengthWithSpaces(field.getName(), 35);
        try {
            String prefix = tabbed ? "" : "    " + fieldName + " = ";
            String postfix = tabbed ? "\t" : "\n";
            if (field.getType() == int.class) {
                out.print(prefix + field.getInt(null) + postfix);
            } else if (field.getType() == boolean.class) {
                out.print(prefix + field.getBoolean(null) + postfix);
            } else if (field.getType() == float.class) {
                out.print(prefix + field.getFloat(null) + postfix);
            } else if (field.getType() == String.class) {
                out.print(prefix + field.get(null) + postfix);
            } else if (field.getType() == Map.class) {
                Map m = (Map) field.get(null);
                out.print(prefix + printMap(m) + postfix);
            } else {
                out.print(prefix + field.get(null) + postfix);
            }
        } catch (IllegalAccessException e) {
            // do nothing.
        }
    }

    private static void printField(String fieldName, long value) {
        out.print("    " + fieldName + " = " + value + "\n");
    }

    private static void printField(String fieldName, double value) {
        out.print("    " + fieldName + " = " + value + "\n");
    }

    private static class Timing {
        private final int number;
        private final ClassMethodActor classMethodActor;
        private final int instructions;
        private final long nanoSeconds;

        Timing(ClassMethodActor classMethodActor, int instructions, long ns) {
            this.number = timings.size();
            this.classMethodActor = classMethodActor;
            this.instructions = instructions;
            this.nanoSeconds = ns;
        }

        public double bytecodesPerSecond() {
            return ONE_BILLION * (bytecodes() / (double) nanoSeconds);
        }

        public int bytecodes() {
            return classMethodActor.originalCodeAttribute().code().length;
        }

        public double instructionsPerSecond() {
            return ONE_BILLION * (instructions / (double) nanoSeconds);
        }
    }
}

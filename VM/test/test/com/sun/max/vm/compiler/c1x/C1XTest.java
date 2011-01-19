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
package test.com.sun.max.vm.compiler.c1x;

import static com.sun.max.lang.Classes.*;
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import test.com.sun.max.vm.*;

import com.sun.c1x.*;
import com.sun.c1x.debug.*;
import com.sun.cri.ci.*;
import com.sun.cri.xir.*;
import com.sun.max.config.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.c1x.*;
import com.sun.max.vm.hosted.*;

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
    private static final Option<Boolean> printBailoutOption = options.newBooleanOption("print-bailout", true,
        "Print bailout exceptions.");
    private static final Option<Boolean> printBailoutSizeOption = options.newBooleanOption("print-bailout-size", false,
        "Print the size of bailed out methods, which helps choosing the simplest failure case for debugging..");
    private static final Option<File> outFileOption = options.newFileOption("o", (File) null,
        "A file to which output should be sent. If not specified, then output is sent to stdout.");
    private static final Option<Boolean> nowarnOption = options.newBooleanOption("nowarn", false,
        "Do not print ClassNotFoundException warnings.");
    private static final Option<Boolean> clinitOption = options.newBooleanOption("clinit", true,
        "Compile class initializer (<clinit>) methods");
    private static final Option<Boolean> failFastOption = options.newBooleanOption("fail-fast", false,
        "Stop compilation upon the first bailout.");
    private static final Option<Boolean> compileTargetMethod = options.newBooleanOption("compile-target-method", false,
        "Use the C1X compiler to compile all the way to a TargetMethod instance.");
    private static final Option<Integer> timingOption = options.newIntegerOption("timing", 0,
        "Perform the specified number of timing runs.");
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
    private static final Option<Integer> c1xOptLevel = options.newIntegerOption("C1X:OptLevel", 3,
        "Set the overall optimization level of C1X (-1 to use default settings)");
    private static final Option<List<String>> metricsOption = options.newStringListOption("print-metrics", new String[0],
        "A list of metrics from the C1XMetrics class to print.");

    static {
        // add all the fields from C1XOptions as options
        options.addFieldOptions(C1XOptions.class, "C1X", C1XCompilerScheme.getHelpMap());
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

    private static String[] expandArguments(String[] args) throws IOException {
        List<String> result = new ArrayList<String>(args.length);
        for (String arg : args) {
            if (arg.charAt(0) == '@') {
                File file = new File(arg.substring(1));
                result.addAll(Files.readLines(file));
            } else {
                result.add(arg);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public static void main(String[] args) throws IOException {
        // set the default optimization level before parsing options
        VMConfigurator vmConfigurator = new VMConfigurator(options);
        options.parseArguments(args);
        Integer optLevel = c1xOptLevel.getValue();
        if (optLevel >= 0) {
            C1XOptions.setOptimizationLevel(optLevel);
        }

        options.setValuesAgain();
        final String[] arguments = expandArguments(options.getArguments());

        if (helpOption.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }

        ClassMethodActor.hostedVerificationDisabled = true;

        if (outFileOption.getValue() != null) {
            try {
                out = new PrintStream(new FileOutputStream(outFileOption.getValue()));
                Trace.setStream(out);
                System.setProperty(TTY.C1X_TTY_LOG_FILE_PROPERTY, outFileOption.getValue().getPath());
            } catch (FileNotFoundException e) {
                System.err.println("Could not open " + outFileOption.getValue() + " for writing: " + e);
                System.exit(1);
            }
        }

        if (timingOption.getValue() > 0) {
            verboseOption.setValue(0);
            failFastOption.setValue(false);
            printBailoutOption.setValue(false);
            C1XOptions.DetailedAsserts = false;
            C1XOptions.IRChecking = false;
        }

        Trace.on(traceOption.getValue());

        String c1xPackage = getPackageName(C1XCompilerScheme.class);
        vmConfigurator.optScheme.setValue(c1xPackage);
        vmConfigurator.jitScheme.setValue(c1xPackage);
        vmConfigurator.create(true);

        // create the prototype
        if (verboseOption.getValue() > 0) {
            out.print("Initializing Java prototype... ");
        }
        JavaPrototype.initialize(false);
        if (verboseOption.getValue() > 0) {
            out.println("done");
        }

        // create MaxineRuntime
        VMConfiguration configuration = vmConfig();
        final RuntimeCompilerScheme compilerScheme = configuration.optCompilerScheme();


        String searchCp = searchCpOption.getValue();
        final Classpath classpath = searchCp == null || searchCp.length() == 0 ? Classpath.fromSystem() : new Classpath(searchCp);
        final List<MethodActor> methods = new MyMethodFinder().find(arguments, classpath, C1XTest.class.getClassLoader(), null);
        final ProgressPrinter progress = new ProgressPrinter(out, methods.size(), verboseOption.getValue(), false);

        doWarmup(compilerScheme, methods);
        doCompile(compilerScheme, methods, progress);

        if (verboseOption.getValue() > 0) {
            progress.report();
        }
        reportTimings();
        reportMetrics();

        // Non-zero exit code indicates number of failures
        System.exit(progress.failed());
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
            }
        } else {
            // compile all the methods and report progress
            for (MethodActor methodActor : methods) {
                progress.begin(methodActor.toString());
                boolean result;
                if (compilerScheme instanceof C1XCompilerScheme && !compileTargetMethod.getValue()) {
                    C1XCompilerScheme c1x = (C1XCompilerScheme) compilerScheme;
                    result = compile(c1x.compiler(), c1x.runtime, c1x.xirGenerator, methodActor, printBailoutOption.getValue(), false);
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
                if (!compile(c1x.compiler(), c1x.runtime, c1x.xirGenerator, methodActor, false, true)) {
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
                        compile(c1x.compiler(), c1x.runtime, c1x.xirGenerator, actor, false, false);
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
        final long startNs = System.nanoTime();
        CiResult result = compiler.compileMethod(method, -1, xirGenerator);
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

    static class MyMethodFinder extends MethodFinder {
        HashSet<PatternMatcher> patterns;
        @Override
        protected void addClassToProcess(PatternMatcher classNamePattern, String className, List<String> matchingClasses) {
            boolean added = patterns.add(classNamePattern);
            if (added && verboseOption.getValue() > 0) {
                out.print("Classes " + classNamePattern.type + " '" + classNamePattern.pattern + "'... ");
            }
            super.addClassToProcess(classNamePattern, className, matchingClasses);
        }

        @Override
        public List<MethodActor> find(String[] patterns, Classpath classpath, ClassLoader classLoader, List<Throwable> nonFatalErrors) {
            this.patterns = new HashSet<PatternMatcher>();
            return super.find(patterns, classpath, classLoader, nonFatalErrors);
        }

        private static boolean isCompilable(MethodActor method) {
            if (method.isNative()) {
                if (((ClassMethodActor) method).compilee() == method) {
                    // Non-substituted native method
                    if (!MaxRiRuntime.CAN_COMPILE_NATIVE_METHODS) {
                        return false;
                    }
                }
            }
            return method instanceof ClassMethodActor && !method.isAbstract() && !method.isBuiltin() && !method.isIntrinsic();
        }

        @Override
        protected void addMethod(MethodActor method, List<MethodActor> methods) {

            if (isCompilable(method) && (clinitOption.getValue() || method.name != SymbolTable.CLINIT)) {
                super.addMethod(method, methods);
                if ((methods.size() % 1000) == 0 && verboseOption.getValue() >= 1) {
                    out.print('.');
                }
            }
        }

        @Override
        protected ClassActor getClassActor(Class< ? > javaClass) {
            ClassActor classActor = null;
            try {
                classActor = ClassActor.fromJava(javaClass);

                BootImagePackage bootImagePackage = BootImagePackage.fromClass(javaClass);
                if (bootImagePackage != null && bootImagePackage.name().contains(".prototype")) {
                    return null;
                }
            } catch (Throwable t) {
                // do nothing.
            }
            return classActor;
        }
    }

    private static void recordTime(MethodActor method, int inlinedBytes, int instructions, long ns) {
        if (!averageOption.getValue()) {
            timings.add(new Timing((ClassMethodActor) method, instructions, ns));
        }
        totalBytes += ((ClassMethodActor) method).originalCodeAttribute(true).code().length;
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
                out.print(ibcps + " inlined bytes/s   ");
            }
            if (totalIps > 0) {
                out.print(ips + " insts/s   ");
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
            C1XMetrics.print();
        }
        List<String> metrics = metricsOption.getValue();
        if (metrics.size() > 0) {
            for (String s : metrics) {
                TTY.print(s + "\t");
            }
            TTY.println();
            for (String s : metrics) {
                try {
                    C1XMetrics.printField(C1XMetrics.class.getDeclaredField(s), true);
                } catch (NoSuchFieldException e) {
                    TTY.println("-----");
                }
            }
            TTY.println();
        }
    }

    private static double averageTime() {
        return (cumulNs / (double) timingOption.getValue()) / ONE_BILLION;
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
            return classMethodActor.originalCodeAttribute(true).code().length;
        }

        public double instructionsPerSecond() {
            return ONE_BILLION * (instructions / (double) nanoSeconds);
        }
    }
}

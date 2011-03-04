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
package test.com.sun.max.vm.compiler;

import java.io.*;
import java.util.*;

import test.com.sun.max.vm.jtrun.*;

import com.sun.max.io.*;
import com.sun.max.io.Streams.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.hosted.*;

public final class JavaTester {

    private JavaTester() {
    }

    public static final OptionSet options = new OptionSet(true);

    private static final Option<Boolean> HELP = options.newBooleanOption("help", false,
                    "Displays usage message and exits.");
    private static final Option<Integer> VERBOSE = options.newIntegerOption("verbose", 1,
                    "Selects the verbosity level of the testing framework.");
    private static final Option<Boolean> LOADING_PACKAGES = options.newBooleanOption("loadPackages", false,
                    "Determines whether the Java prototype is to load all VM packages.");
    private static final Option<List<String>> EXECS = options.newStringListOption("scenario", "reflect",
                    "Selects the scenarios in which to run each test.");
    private static final Option<Boolean> GEN_RUNSCHEME = options.newBooleanOption("gen-run-scheme", true,
                    "Selects whether the java tester will generate a run scheme for the " +
                    "tests, which can be used to create a target VM to run.");
    private static final Option<String> RUNSCHEME_PACKAGE = options.newStringOption("run-scheme-package", "some",
                    "Selects the target output package for the tests.");
    private static final Option<Boolean> GEN_IMAGE = options.newBooleanOption("gen-image", false,
                    "Selects whether the tester will generate a boot image with the specified tests.");
    private static final Option<Boolean> RUN_IMAGE = options.newBooleanOption("run-image", false,
                    "Selects whether the tester will run the target VM that has been generated.");
    private static final Option<Boolean> RESTART_IMAGE = options.newBooleanOption("restart", true,
                    "Selects whether the tester will generate an image that allows restarting the tests " +
                    "from a particular test number.");
    private static final Option<Boolean> SORT_OPTION = options.newBooleanOption("alphabetical", true,
                    "Selects whether the tester will sort the tests alphabetically when generating an image.");
    private static final Option<List<String>> GEN_IMAGE_ARGS = options.newStringListOption("gen-image-args", "",
                    "Additional args to pass to the image generator.");
    private static final Option<List<String>> GEN_IMAGE_PROPS = options.newStringListOption("gen-image-props", "",
                    "Additional system properties to pass to the image generator.");
    private static final Option<List<String>> RUN_IMAGE_ARGS = options.newStringListOption("run-image-args", "",
                    "Additional args to pass to the target image.");
    private static final Option<Boolean> NATIVE_TESTS = options.newBooleanOption("native-tests", false,
                    "When specified, the JavaTester will attempt to load " + System.mapLibraryName("javatest") + " in order to run " +
                    "native JNI and JVM tests.");
    private static final Option<Boolean> REPORT_METRICS = options.newBooleanOption("report-metrics", false,
                    "When specified, the JavaTester will report metrics that were gathered while running " +
                    "the tests.");

    private static boolean filesUpdated = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        Trace.addTo(options);
        // parse the arguments
        final String[] arguments = options.parseArguments(args).getArguments();
        if (SORT_OPTION.getValue()) {
            Arrays.sort(arguments);
        }

        if (HELP.getValue()) {
            options.printHelp(System.out, 80);
            return;
        }
        // create a registry of known VM executors.
        final Registry<JavaExecHarness.Executor> executors = Registry.newRegistry(JavaExecHarness.Executor.class);

        if (NATIVE_TESTS.getValue()) {
            if (MaxineVM.isHosted()) {
                Prototype.loadLibrary("javatest");
            } else {
                System.loadLibrary("javatest");
            }
        }

        executors.registerClass("reflect", ReflectiveExecutor.class);
        executors.registerClass("cir", "test.com.sun.max.vm.cps.CIRTestExecutor");
        executors.registerClass("dir", "test.com.sun.max.vm.cps.DIRTestExecutor");
        executors.registerClass("eir", "test.com.sun.max.vm.cps.EIRTestExecutor");

        // run each executor.
        for (String alias : EXECS.getValue()) {
            if ("target".equals(alias)) {
                runInTarget(arguments);
            } else {
                runExecutor(alias, arguments, executors);
            }
        }
        if (REPORT_METRICS.getValue()) {
            GlobalMetrics.report(System.out);
        }

        // If JavaTesterGenerator had to actually update
        if (filesUpdated) {
            System.exit(1);
        }
    }

    private static void runInTarget(final String[] arguments) throws IOException, InterruptedException {
        if (GEN_RUNSCHEME.getValue()) {
            // generate the run scheme if necessary
            Trace.line(0, "Generating target run scheme...");
            options.setValue("restart", String.valueOf(RESTART_IMAGE.getValue()));
            options.setValue("alphabetical", String.valueOf(SORT_OPTION.getValue()));
            options.setValue("package", String.valueOf(RUNSCHEME_PACKAGE.getValue()));
            filesUpdated = JTGenerator.generate(options, arguments);
        }
        if (GEN_IMAGE.getValue()) {
            // generate an image if necessary
            Trace.line(0, "Generating target image...");
            final String[] generatorArgs = {"-trace=1", "-run=" + JTAbstractRunScheme.class.getPackage().getName() + "." + RUNSCHEME_PACKAGE.getValue()};
            final String[] systemProperties = {};
            final String[] javaArgs = buildJavaArgs(BootImageGenerator.class,
                            appendArgs(systemProperties, GEN_IMAGE_PROPS.getValue()),
                            appendArgs(generatorArgs, GEN_IMAGE_ARGS.getValue()));
            exec(null, javaArgs, System.out, System.err, System.in);
        }
        if (RUN_IMAGE.getValue()) {
            // generate the run scheme if necessary
            Trace.line(0, "Running target image...");
            final File defaultImagePath = BootImageGenerator.getDefaultVMDirectory();
            final String[] imageArgs = {"./maxvm"};
            exec(defaultImagePath, appendArgs(imageArgs, RUN_IMAGE_ARGS.getValue()), System.out, System.err, System.in);
        }
    }

    private static String[] appendArgs(String[] args, List<String> extraArgs) {
        String[] result = args;
        if (extraArgs.size() > 0) {
            result = new String[args.length + extraArgs.size()];
            System.arraycopy(args, 0, result, 0, args.length);
            final String[] extraArgsArray = new String[extraArgs.size()];
            extraArgs.toArray(extraArgsArray);
            System.arraycopy(extraArgsArray, 0, result, args.length, extraArgs.size());
        }
        return result;
    }

    private static void runExecutor(String alias, String[] args, Registry<JavaExecHarness.Executor> executors) {
        Trace.line(0, "Beginning tests with " + alias + " executor...");
        final Registry<TestHarness> harnesses = new Registry<TestHarness>(TestHarness.class, true);
        final JavaExecHarness javaExecHarness = new JavaExecHarness(executors.getInstance(alias));
        harnesses.registerObject("java", javaExecHarness);
        final TestEngine engine = new TestEngine(harnesses);
        engine.setVerboseLevel(VERBOSE.getValue());
        engine.setLoadingPackages(LOADING_PACKAGES.getValue());
        engine.parseAndRunTests(args);
        engine.report(System.out);
        Trace.line(0);
    }

    private static String[] buildJavaArgs(Class javaMainClass, String[] systemProperties, String[] args) {
        final LinkedList<String> cmd = new LinkedList<String>();
        cmd.add("java");
        cmd.add("-d64");
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        for (int i = 0; i < systemProperties.length; i++) {
            cmd.add("-D" + systemProperties[i]);
        }
        cmd.add(javaMainClass.getName());
        for (String arg : args) {
            cmd.add(arg);
        }
        return cmd.toArray(new String[0]);
    }

    private static void exec(File workingDir, String[] command, OutputStream out, OutputStream err, InputStream in) throws IOException, InterruptedException {
        Trace.line(1, "Executing process in directory: " + workingDir);
        for (String c : command) {
            Trace.line(1, "  " + c);
        }
        final Process process = Runtime.getRuntime().exec(command, null, workingDir);
        try {
            final Redirector stderr = Streams.redirect(process, process.getErrorStream(), err, command + " [stderr]", 50);
            final Redirector stdout = Streams.redirect(process, process.getInputStream(), out, command + " [stdout]");
            final Redirector stdin = Streams.redirect(process, in, process.getOutputStream(), command + " [stdin]");
            final int exitValue = process.waitFor();
            stderr.close();
            stdout.close();
            stdin.close();
            if (exitValue != 0) {
                ProgramError.unexpected("execution of command failed: " + command + " [exit code = " + exitValue + "]");
            }
        } finally {
            process.destroy();
        }
    }

}

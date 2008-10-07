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
package test.com.sun.max.vm.compiler;

import java.io.*;
import java.util.*;

import test.com.sun.max.vm.testrun.*;

import com.sun.max.io.*;
import com.sun.max.io.Streams.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.prototype.*;


public final class JavaTester {

    private JavaTester() {
    }

    private static final OptionSet _options = new OptionSet(true);

    private static final Option<Boolean> HELP = _options.newBooleanOption("help", false,
                    "Displays usage message and exits.");
    private static final Option<Integer> VERBOSE = _options.newIntegerOption("verbose", 1,
                    "Selects the verbosity level of the testing framework.");
    private static final Option<Boolean> LOADING_PACKAGES = _options.newBooleanOption("loadPackages", false,
                    "Determines whether the Java prototype is to load all VM packages.");
    private static final Option<List<String>> EXECS = _options.newStringListOption("scenario", "reflect",
                    "Selects the scenarios in which to run each test.");
    private static final Option<Boolean> GEN_RUNSCHEME = _options.newBooleanOption("gen-run-scheme", true,
                    "Selects whether the java tester will generate a run scheme for the " +
                    "tests, which can be used to create a target VM to run.");
    private static final Option<String> RUNSCHEME_PACKAGE = _options.newStringOption("run-scheme-package", "some",
                    "Selects the target output package for the tests.");
    private static final Option<Boolean> GEN_IMAGE = _options.newBooleanOption("gen-image", false,
                    "Selects whether the tester will generate a boot image with the specified tests.");
    private static final Option<Boolean> RUN_IMAGE = _options.newBooleanOption("run-image", false,
                    "Selects whether the tester will run the target VM that has been generated.");
    private static final Option<Boolean> RESTART_IMAGE = _options.newBooleanOption("restart", true,
                    "Selects whether the tester will generate an image that allows restarting the tests " +
                    "from a particular test number.");
    private static final Option<Boolean> SORT_OPTION = _options.newBooleanOption("alphabetical", true,
                    "Selects whether the tester will sort the tests alphabetically when generating an image.");
    private static final Option<List<String>> GEN_IMAGE_ARGS = _options.newStringListOption("gen-image-args", "",
                    "Additional args to pass to the image generator.");
    private static final Option<List<String>> GEN_IMAGE_PROPS = _options.newStringListOption("gen-image-props", "",
                    "Additional system properties to pass to the image generator.");
    private static final Option<List<String>> RUN_IMAGE_ARGS = _options.newStringListOption("run-image-args", "",
                    "Additional args to pass to the target image.");
    private static final Option<Boolean> NATIVE_TESTS = _options.newBooleanOption("native-tests", false,
                    "When specified, the JavaTester will attempt to load " + System.mapLibraryName("javatest") + " in order to run " +
                    "native JNI and JVM tests.");
    private static final Option<Boolean> REPORT_METRICS = _options.newBooleanOption("report-metrics", false,
                    "When specified, the JavaTester will report metrics that were gathered while running " +
                    "the tests.");

    public static void main(String[] args) throws IOException, InterruptedException {
        Trace.addTo(_options);
        // parse the arguments
        final String[] arguments = _options.parseArguments(args).getArguments();

        if (HELP.getValue()) {
            _options.printHelp(System.out, 80);
            return;
        }
        // create a registry of known VM executors.
        final Registry<JavaExecHarness.Executor> executors = Registry.newRegistry(JavaExecHarness.Executor.class);

        if (NATIVE_TESTS.getValue()) {
            if (MaxineVM.isPrototyping()) {
                Prototype.loadLibrary("javatest");
            } else {
                System.loadLibrary("javatest");
            }
        }

        executors.registerClass("reflect", ReflectiveExecutor.class);
        executors.registerClass("cir", CIRTestExecutor.class);
        executors.registerClass("dir", DIRTestExecutor.class);
        executors.registerClass("eir", EIRTestExecutor.class);

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
    }

    private static void runInTarget(final String[] arguments) throws IOException, InterruptedException {
        if (GEN_RUNSCHEME.getValue()) {
            // generate the run scheme if necessary
            Trace.line(0, "Generating target run scheme...");
            _options.setValue("restart", String.valueOf(RESTART_IMAGE.getValue()));
            _options.setValue("alphabetical", String.valueOf(SORT_OPTION.getValue()));
            _options.setValue("package", String.valueOf(RUNSCHEME_PACKAGE.getValue()));
            JavaTesterGenerator.generate(_options, arguments);
        }
        if (GEN_IMAGE.getValue()) {
            // generate an image if necessary
            Trace.line(0, "Generating target image...");
            final String[] generatorArgs = {"-trace=1", "-run=" + AbstractTester.class.getPackage().getName() + "." + RUNSCHEME_PACKAGE.getValue()};
            final String[] systemProperties = {"sun.misc.ProxyGenerator.saveGeneratedFiles=true", "sun.reflect.noInflation=false", "sun.reflect.inflationThreshold=1000000"};
            final String[] javaArgs = buildJavaArgs(BinaryImageGenerator.class,
                            appendArgs(systemProperties, GEN_IMAGE_PROPS.getValue()),
                            appendArgs(generatorArgs, GEN_IMAGE_ARGS.getValue()));
            exec(null, javaArgs, System.out, System.err, System.in);
        }
        if (RUN_IMAGE.getValue()) {
            // generate the run scheme if necessary
            Trace.line(0, "Running target image...");
            final File defaultImagePath = BinaryImageGenerator.getDefaultBootImageFilePath().getParentFile();
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

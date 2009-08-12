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
package test.com.sun.max.vm;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import junit.framework.*;

import org.junit.internal.requests.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.AllTests;

import sun.management.*;
import test.com.sun.max.vm.MaxineTesterConfiguration.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.lang.Arrays;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.prototype.*;

/**
 * This class combines all the testing modes of the Maxine virtual machine into a central
 * place. It is capable of building images in various configurations and running tests
 * and user programs with the generated images.
 *
 * @author Ben L. Titzer
 * @author Doug Simon
 */
public class MaxineTester {

    private static final int PROCESS_TIMEOUT = -333;

    private static final OptionSet options = new OptionSet();
    private static final Option<Boolean> singleThreadedOption = options.newBooleanOption("s", false,
                    "Single threaded. Do not run any tests concurrently.");
    private static final Option<String> outputDirOption = options.newStringOption("output-dir", "maxine-tester",
                    "The output directory for the results of the maxine tester.");
    private static final Option<Integer> imageBuildTimeOutOption = options.newIntegerOption("image-build-timeout", 1200,
                    "The number of seconds to wait for an image build to complete before " +
                    "timing out and killing it.");
    private static final Option<String> javaExecutableOption = options.newStringOption("java-executable", "java",
                    "The name of or full path to the Java VM executable to use. This must be a JDK 6 or greater VM.");
    private static final Option<String> javaVMArgsOption = options.newStringOption("java-vm-args", "-d64 -Xmx1g",
                    "The VM options to be used when running the Java VM.");
    private static final Option<Integer> javaTesterTimeOutOption = options.newIntegerOption("java-tester-timeout", 50,
                    "The number of seconds to wait for the in-target Java tester tests to complete before " +
                    "timing out and killing it.");
    private static final Option<Integer> javaRunTimeOutOption = options.newIntegerOption("java-run-timeout", 60,
                    "The number of seconds to wait for the target VM to complete before " +
                    "timing out and killing it when running user programs.");
    private static final Option<Integer> traceOption = options.newIntegerOption("trace", 0,
                    "The tracing level for building the images and running the tests.");
    private static final Option<Boolean> skipImageGenOption = options.newBooleanOption("skip-image-gen", false,
                    "Skip the generation of the image, which is useful for testing the Maxine tester itself.");
    private static final Option<List<String>> javaTesterConfigsOption = options.newStringListOption("java-tester-configs",
                    MaxineTesterConfiguration.defaultJavaTesterConfigs(),
                    "A list of configurations for which to run the Java tester tests.");
    private static final Option<List<String>> testsOption = options.newStringListOption("tests", "junit,output,javatester",
                    "The list of test harnesses to run, which may include JUnit tests (junit), output tests (output), " +
                    "the JavaTester (javatester), DaCapo (dacapo), and SpecJVM98 (specjvm98).\n\nA subset of the JUnit/Output/Dacapo/SpecJVM98/Shootout tests " +
                    "can be specified by appending a ':' followed by a '+' separated list of test name substrings. For example:\n\n" +
                    "-tests=specjvm98:jess+db,dacapo:pmd+fop\n\nwill " +
                    "run the _202_jess and _209_db SpecJVM98 benchmarks as well as the pmd and fop Dacapo benchmarks.\n\n" +
                    "Output tests: " + Arrays.toString(MaxineTesterConfiguration.outputTests) + "\n\n" +
                    "Dacapo tests: " + Arrays.toString(MaxineTesterConfiguration.dacapoTests) + "\n\n" +
                    "SpecJVM98 tests: " + Arrays.toString(MaxineTesterConfiguration.specjvm98Tests) + "\n\n" +
                    "Shootout tests: " + Arrays.toString(MaxineTesterConfiguration.shootoutTests()));
    private static final Option<List<String>> maxvmConfigListOption = options.newStringListOption("maxvm-configs",
                    MaxineTesterConfiguration.defaultMaxvmOutputConfigs(),
                    "A list of configurations for which to run the Maxine output tests.");
    private static final Option<String> javaConfigAliasOption = options.newStringOption("java-config-alias", "optopt",
                    "The Java tester config to use for running Java programs. Omit this option to use a separate config for Java programs.");
    private static final Option<Integer> junitTestTimeOutOption = options.newIntegerOption("junit-test-timeout", 300,
                    "The number of seconds to wait for a JUnit test to complete before " +
                    "timing out and killing it.");
    private static final Option<Boolean> slowAutoTestsOption = options.newBooleanOption("slow-junit-tests", false,
                    "Include junit-tests known to be slow.");
    private static final Option<Boolean> failFastOption = options.newBooleanOption("fail-fast", false,
                    "Stop execution as soon as a single test fails.");
    private static final Option<File> specjvm98ZipOption = options.newFileOption("specjvm98", (File) null,
                    "Location of zipped up SpecJVM98 directory. If not provided, then the SPECJVM98_ZIP environment variable is used.");
    private static final Option<File> dacapoJarOption = options.newFileOption("dacapo", (File) null,
                    "Location of DaCapo JAR file. If not provided, then the DACAPO_JAR environment variable is used.");
    private static final Option<File> shootoutDirOption = options.newFileOption("shootout", (File) null,
                    "Location of the Programming Language Shootout tests. If not provided, then the SHOOTOUT_DIR environment variable is used.");
    private static final Option<Boolean> timingOption = options.newBooleanOption("timing", false,
                    "For the SpecJVM98 and DaCapo benchmarks, report internal timings compared to the baseline.");
    private static final Option<Boolean> execTimesOption = options.newBooleanOption("exec-times", true,
                    "Report the time taken for each executed subprocess.");
    private static final Option<Boolean> helpOption = options.newBooleanOption("help", false,
                    "Show help message and exit.");

    private static String javaConfigAlias = null;
    private static Date startDate;

    public static void main(String[] args) {
        try {
            options.parseArguments(args);

            if (helpOption.getValue()) {
                options.printHelp(System.out, 80);
                return;
            }

            startDate = new Date();
            javaConfigAlias = javaConfigAliasOption.getValue();
            if (javaConfigAlias != null) {
                ProgramError.check(MaxineTesterConfiguration.imageConfigs.containsKey(javaConfigAlias), "Unknown Java tester config '" + javaConfigAlias + "'");
            }
            final File outputDir = new File(outputDirOption.getValue()).getAbsoluteFile();
            makeDirectory(outputDir);
            Trace.on(traceOption.getValue());
            for (String test : testsOption.getValue()) {
                if (stopTesting()) {
                    break;
                } else if ("junit".equals(test)) {
                    // run the JUnit tests
                    new JUnitHarness(null).run();
                } else if (test.startsWith("junit:")) {
                    // run the JUnit tests
                    new JUnitHarness(test.substring("junit:".length()).split("\\+")).run();
                } else if ("output".equals(test)) {
                    // run the Output tests
                    new OutputHarness(MaxineTesterConfiguration.outputTestClasses).run();
                } else if (test.startsWith("output:")) {
                    // run the Output tests
                    new OutputHarness(filterTestClassesBySubstrings(MaxineTesterConfiguration.outputTestClasses, test.substring("output:".length()).split("\\+"))).run();
                } else if ("javatester".equals(test)) {
                    // run the JavaTester tests
                    new JavaTesterHarness().run();
                } else if ("dacapo".equals(test)) {
                    // run the DaCapo tests
                    new DaCapoHarness(MaxineTesterConfiguration.dacapoTests).run();
                } else if (test.startsWith("dacapo:")) {
                    // run the DaCapo tests
                    new DaCapoHarness(filterTestsBySubstrings(MaxineTesterConfiguration.dacapoTests, test.substring("dacapo:".length()).split("\\+"))).run();
                } else if ("specjvm98".equals(test)) {
                    // run the SpecJVM98 tests
                    new SpecJVM98Harness(MaxineTesterConfiguration.specjvm98Tests).run();
                } else if (test.startsWith("specjvm98:")) {
                    // run the SpecJVM98 tests
                    new SpecJVM98Harness(filterTestsBySubstrings(MaxineTesterConfiguration.specjvm98Tests, test.substring("specjvm98:".length()).split("\\+"))).run();
                } else if ("shootout".equals(test)) {
                    // run the shootout tests
                    new ShootoutHarness(MaxineTesterConfiguration.shootoutTests()).run();
                } else if (test.startsWith("shootout:")) {
                    // run the shootout tests
                    new ShootoutHarness(filterTestsBySubstrings(MaxineTesterConfiguration.shootoutTests(), test.substring("shootout:".length()).split("\\+"))).run();
                } else {
                    out().println("Unrecognized test harness: " + test);
                    System.exit(-1);
                }
            }

            System.exit(reportTestResults(out()));
        } catch (Throwable throwable) {
            throwable.printStackTrace(err());
            System.exit(-1);
        }
    }


    private static String[] filterTestsBySubstrings(String[] tests, String[] substrings) {

        final List<String> list = new ArrayList<String>(tests.length);
        for (String substring : substrings) {
            for (String test : tests) {
                if (test.contains(substring)) {
                    list.add(test);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    private static Class[] filterTestClassesBySubstrings(Class[] tests, String[] substrings) {

        final List<Class> list = new ArrayList<Class>(tests.length);
        for (String substring : substrings) {
            for (Class test : tests) {
                if (test.getSimpleName().contains(substring)) {
                    list.add(test);
                }
            }
        }
        return list.toArray(new Class[list.size()]);
    }

    private static final ThreadLocal<PrintStream> out = new ThreadLocal<PrintStream>() {
        @Override
        protected PrintStream initialValue() {
            return System.out;
        }
    };
    private static final ThreadLocal<PrintStream> err = new ThreadLocal<PrintStream>() {
        @Override
        protected PrintStream initialValue() {
            return System.err;
        }
    };

    private static PrintStream out() {
        return out.get();
    }
    private static PrintStream err() {
        return err.get();
    }

    /**
     * Runs a given runnable with all {@linkplain #out() standard} and {@linkplain #err() error} output redirect to
     * private buffers. The private buffers are then flushed to the global streams once the runnable completes.
     */
    private static void runWithSerializedOutput(Runnable runnable) {
        final PrintStream oldOut = out();
        final PrintStream oldErr = err();
        final ByteArrayPrintStream tmpOut = new ByteArrayPrintStream();
        final ByteArrayPrintStream tmpErr = new ByteArrayPrintStream();
        try {
            out.set(tmpOut);
            err.set(tmpErr);
            runnable.run();
        } finally {
            synchronized (oldOut) {
                tmpOut.writeTo(oldOut);
            }
            synchronized (oldErr) {
                tmpErr.writeTo(oldErr);
            }
            out.set(oldOut);
            err.set(oldErr);
        }
    }

    /**
     * Used for per-thread buffering of output.
     */
    static class ByteArrayPrintStream extends PrintStream {
        public ByteArrayPrintStream() {
            super(new ByteArrayOutputStream());
        }
        public void writeTo(PrintStream other) {
            try {
                ((ByteArrayOutputStream) out).writeTo(other);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void makeDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            ProgramError.unexpected("Could not make directory " + directory);
        }
        ProgramError.check(directory.isDirectory(), "Path is not a directory: " + directory);
        copyInputFiles(directory);
    }

    private static void copyInputFiles(File directory) {
        final Set<java.lang.Package> outputTestPackages = new HashSet<java.lang.Package>();
        for (Class mainClass : MaxineTesterConfiguration.outputTestClasses) {
            outputTestPackages.add(mainClass.getPackage());
        }
        final File parent = new File(new File("VM"), "test");
        ProgramError.check(parent != null && parent.exists(), "Could not find VM/test: trying running in the root of your Maxine repository");
        for (java.lang.Package p : outputTestPackages) {
            File dir = parent;
            for (String n : p.getName().split("\\.")) {
                dir = new File(dir, n);
            }
            final File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().endsWith(".input")) {
                        try {
                            Files.copy(f, new File(directory, f.getName()));
                        } catch (FileNotFoundException e) {
                            // do nothing.
                        } catch (IOException e) {
                            // do nothing.
                        }
                    }
                }
            }
        }
    }

    /**
     * A map from test names to a string describing a test failure or null if a test passed.
     */
    private static final Map<String, String> unexpectedFailures = Collections.synchronizedMap(new TreeMap<String, String>());
    private static final Map<String, String> unexpectedPasses = Collections.synchronizedMap(new TreeMap<String, String>());

    /**
     * Adds a test result to the global set of test results.
     *
     * @param testName the unique name of the test
     * @param failure a failure message or null if the test passed
     * @return {@code true} if the result (pass or fail) of the test matches the expected result, {@code false} otherwise
     */
    private static boolean addTestResult(String testName, String failure, ExpectedResult expectedResult) {
        final boolean passed = failure == null;
        if (!expectedResult.matchesActualResult(passed)) {
            if (expectedResult == ExpectedResult.FAIL) {
                unexpectedPasses.put(testName, failure);
            } else {
                assert expectedResult == ExpectedResult.PASS;
                unexpectedFailures.put(testName, failure);
            }
            return false;
        }
        return true;
    }

    private static boolean addTestResult(String testName, String failure) {
        return addTestResult(testName, failure, MaxineTesterConfiguration.expectedResult(testName, null));
    }

    /**
     * Summarizes the collected test results.
     *
     * @param out where the summary should be printed. This value can be null if only the return value is of interest.
     * @return an integer that is the total of all the unexpected passes, the unexpected failures, the number of failed
     *         attempts to generate an image and the number of JUnit test subprocesses that failed with an exception
     */
    private static int reportTestResults(PrintStream out) {
        if (out != null) {
            if (!execTimes.isEmpty() && execTimesOption.getValue()) {
                try {
                    final File timesOutFile = new File(outputDirOption.getValue(), "times.stdout");
                    final PrintStream timesOut = new PrintStream(new FileOutputStream(timesOutFile));
                    for (Map.Entry<String, Long> entry : execTimes.entrySet()) {
                        final double ms = entry.getValue().longValue();
                        timesOut.printf("%10.3f: %s%n", ms / 1000, entry.getKey());
                    }
                    timesOut.close();

                    out.println();
                    out.println("Timing info -> see: " + fileRef(timesOutFile));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            out.println();
            out.println("== Summary ==");
        }
        int failedImages = 0;
        for (Map.Entry<String, File> entry : generatedImages.entrySet()) {
            if (entry.getValue() == null) {
                if (out != null) {
                    out.println("Failed building image for configuration '" + entry.getKey() + "'");
                }
                failedImages++;
            }
        }

        int failedAutoTests = 0;
        for (String junitTest : junitTestsWithExceptions) {
            if (out != null) {
                out.println("Non-zero exit status for '" + junitTest + "'");
            }
            failedAutoTests++;
        }

        if (out != null) {
            if (!unexpectedFailures.isEmpty()) {
                out.println("Unexpected failures:");
                for (Map.Entry<String, String> entry : unexpectedFailures.entrySet()) {
                    out.println("  " + entry.getKey() + " " + entry.getValue());
                }
            }
            if (!unexpectedPasses.isEmpty()) {
                out.println("Unexpected passes:");
                for (String unexpectedPass : unexpectedPasses.keySet()) {
                    out.println("  " + unexpectedPass);
                }
            }
        }

        final int exitCode = unexpectedFailures.size() + unexpectedPasses.size() + failedImages + failedAutoTests;
        if (out != null) {
            final Date endDate = new Date();
            final long total = endDate.getTime() - startDate.getTime();
            final DateFormat dateFormat = DateFormat.getTimeInstance();
            out.println("Time: " + dateFormat.format(startDate) + " - " + dateFormat.format(endDate) + " [" + ((double) total) / 1000 + " seconds]");
            out.println("Exit code: " + exitCode);
        }

        return exitCode;
    }

    /**
     * A helper class for running one or more JUnit tests. This helper delegates to {@link JUnitCore} to do most of the work.
     */
    public static class JUnitTestRunner {

        static final String INCLUDE_SLOW_TESTS_PROPERTY = "includeSlowTests";

        private static Set<String> loadFailedTests(File file) {
            if (file.exists()) {
                System.out.println("Only running the tests listed in " + file.getAbsolutePath());
                try {
                    return new HashSet<String>(Iterables.toCollection(Files.readLines(file)));
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            return null;
        }

        /**
         * Runs the JUnit tests in a given class.
         *
         * @param args an array with the following three elements:
         *            <ol>
         *            <li>The name of a class containing the JUnit test(s) to be run.</li>
         *            <li>The path of a file to which the {@linkplain Description name} of the tests that pass will be
         *            written.</li>
         *            <li>The path of a file to which the name of the tests that fail will be written. If this file
         *            already exists, then only the tests listed in the file will be run.</li>
         *            </ol>
         */
        public static void main(String[] args) throws Throwable {
            System.setErr(System.out);

            final String testClassName = args[0];
            final File passedFile = new File(args[1]);
            final File failedFile = new File(args[2]);

            final Class<?> testClass = Class.forName(testClassName);
            final Test test = AllTests.testFromSuiteMethod(testClass);

            final boolean includeSlowTests = System.getProperty(INCLUDE_SLOW_TESTS_PROPERTY) != null;

            final Set<String> failedTestNames = loadFailedTests(failedFile);
            final Runner runner = new AllTests(testClass) {
                @Override
                public void run(RunNotifier notifier) {
                    final TestResult result = new TestResult() {
                        @Override
                        protected void run(TestCase testCase) {
                            final Description description = Description.createTestDescription(testCase.getClass(), testCase.getName());
                            if (!includeSlowTests && MaxineTesterConfiguration.isSlowAutoTestCase(testCase)) {
                                System.out.println("Omitted slow test: " + description);
                                return;
                            }
                            if (failedTestNames == null || failedTestNames.contains(description.toString())) {
                                super.run(testCase);
                            }
                        }
                    };
                    result.addListener(createAdaptingListener(notifier));
                    test.run(result);
                }
            };

            final PrintStream passed = new PrintStream(new FileOutputStream(passedFile));
            final PrintStream failed = new PrintStream(new FileOutputStream(failedFile));
            final JUnitCore junit = new JUnitCore();
            junit.addListener(new RunListener() {
                boolean failedFlag;

                @Override
                public void testStarted(Description description) throws Exception {
                    System.out.println("running " + description);
                }

                @Override
                public void testFailure(Failure failure) throws Exception {
                    failure.getException().printStackTrace(System.out);
                    failedFlag = true;
                }

                @Override
                public void testFinished(Description description) throws Exception {
                    if (this.failedFlag) {
                        failed.println(description);
                    } else {
                        passed.println(description);
                    }
                    this.failedFlag = false;
                }
            });

            final Request request = new ClassRequest(testClass) {
                @Override
                public Runner getRunner() {
                    return runner == null ? super.getRunner() : runner;
                }
            };

            junit.run(request);
            passed.close();
            failed.close();
        }
    }

    /**
     * A list of the {@linkplain JUnitHarness JUnit tests} that caused the Java process to exit with an exception.
     */
    private static AppendableSequence<String> junitTestsWithExceptions = new ArrayListSequence<String>();

    /**
     * Determines if {@linkplain #failFastOption fail fast} has been requested and at least one unexpected failure has
     * occurred.
     */
    static boolean stopTesting() {
        return failFastOption.getValue() && reportTestResults(null) != 0;
    }

    /**
     * Gets a string denoting a path to a given file in a standardized format.
     *
     * *NOTE*: This standardized format is expected by the Maxine gate scripts so do not change it
     * without making the necessary changes to these scripts.
     */
    private static String fileRef(File file) {
        final String basePath = new File(outputDirOption.getValue()).getAbsolutePath() + File.separator;
        final String path = file.getAbsolutePath();
        if (path.startsWith(basePath)) {
            return "file:" + path.substring(basePath.length());
        }
        return file.getAbsolutePath();
    }

    private static String left50(final String str) {
        return Strings.padLengthWithSpaces(str, 50);
    }

    private static String left16(final String str) {
        return Strings.padLengthWithSpaces(str, 16);
    }

    private static ExpectedResult printFailed(String testName, String config) {
        final ExpectedResult expectedResult = MaxineTesterConfiguration.expectedResult(testName, config);
        if (expectedResult == ExpectedResult.FAIL) {
            out().print(left16(config + ": (normal)"));
        } else if (expectedResult == ExpectedResult.NONDETERMINISTIC) {
            out().print(left16(config + ": (noluck) "));
        } else {
            out().print(left16(config + ": (failed)"));
        }
        out().flush();
        return expectedResult;
    }

    private static ExpectedResult printSuccess(String testName, String config) {
        final ExpectedResult expectedResult = MaxineTesterConfiguration.expectedResult(testName, config);
        if (expectedResult == ExpectedResult.PASS) {
            out().print(left16(config + ": OK"));
        } else if (expectedResult == ExpectedResult.NONDETERMINISTIC) {
            out().print(left16(config + ": (lucky) "));
        } else {
            out().print(left16(config + ": (passed)"));
        }
        out().flush();
        return expectedResult;
    }

    static class JavaTesterResult {
        final String summary;
        final String nextTestOption;

        JavaTesterResult(String summary, String nextTestOption) {
            this.nextTestOption = nextTestOption;
            this.summary = summary;
        }

    }

    /**
     * @param workingDir if {@code null}, then {@code imageDir} is used
     */
    private static int runMaxineVM(JavaCommand command, File imageDir, File workingDir, File outputFile, String name, int timeout) {
        String[] envp = null;
        if (OperatingSystem.current() == OperatingSystem.LINUX) {
            // Since the executable may not be in the default location, then the -rpath linker option used when
            // building the executable may not point to the location of libjvm.so any more. In this case,
            // LD_LIBRARY_PATH needs to be set appropriately.
            final Map<String, String> env = new HashMap<String, String>(System.getenv());
            String libraryPath = env.get("LD_LIBRARY_PATH");
            if (libraryPath != null) {
                libraryPath = libraryPath + File.pathSeparatorChar + imageDir.getAbsolutePath();
            } else {
                libraryPath = imageDir.getAbsolutePath();
            }
            env.put("LD_LIBRARY_PATH", libraryPath);

            final String string = env.toString();
            envp = string.substring(1, string.length() - 2).split(", ");
        }
        return exec(workingDir == null ? imageDir : workingDir, command.getExecutableCommand(imageDir.getAbsolutePath() + "/maxvm"), envp, outputFile, name, timeout);
    }

    /**
     * @param workingDir if {@code null}, then {@code imageDir} is used
     */
    private static int runJavaVM(String program, JavaCommand command, File imageDir, File workingDir, File outputFile, int timeout) {
        final String name = "JVM_" + program;
        return exec(workingDir == null ? imageDir : workingDir, command.getExecutableCommand(javaExecutableOption.getValue()), null, outputFile, name, timeout);
    }

    /**
     * Map from configuration names to the directory in which the image for the configuration was created.
     * If the image generation failed for a configuration, then it will have a {@code null} entry in this map.
     */
    private static final Map<String, File> generatedImages = new HashMap<String, File>();

    private static File generateJavaRunSchemeImage() {
        final String config = javaConfigAlias == null ? "java" : javaConfigAlias;
        final File imageDir = new File(outputDirOption.getValue(), config);
        if (skipImageGenOption.getValue()) {
            return imageDir;
        }
        out().println("Building Java run scheme: started");
        if (generateImage(imageDir, config)) {
            out().println("Building Java run scheme: OK");
            return imageDir;
        }
        out().println("Building Java run scheme: failed");
        final File outputFile = stdoutFile(imageDir, "IMAGEGEN", config);
        out().println("  -> see: " + fileRef(outputFile));
        out().println("  -> see: " + fileRef(stderrFile(outputFile)));
        return null;
    }

    private static boolean generateImage(File imageDir, String imageConfig) {
        if (generatedImages.containsKey(imageConfig)) {
            return generatedImages.get(imageConfig) != null;
        }
        final JavaCommand javaCommand = new JavaCommand(BinaryImageGenerator.class);
        javaCommand.addArguments(MaxineTesterConfiguration.getImageConfigArgs(imageConfig));
        javaCommand.addArgument("-output-dir=" + imageDir);
        javaCommand.addArgument("-trace=1");
        javaCommand.addVMOption("-XX:CompileCommand=exclude,com/sun/max/vm/jit/JitReferenceMapEditor,fillInMaps");
        javaCommand.addVMOptions(defaultJVMOptions());
        javaCommand.addClasspath(System.getProperty("java.class.path"));
        final String[] javaArgs = javaCommand.getExecutableCommand(javaExecutableOption.getValue());
        Trace.line(2, "Generating image for " + imageConfig + " configuration...");
        final File outputFile = stdoutFile(imageDir, "IMAGEGEN", imageConfig);

        final int exitValue = exec(null, javaArgs, null, outputFile, "Building " + imageDir.getName() + "/maxine.vm", imageBuildTimeOutOption.getValue());
        if (exitValue == 0) {
            // if the image was built correctly, copy the maxvm executable and shared libraries to the same directory
            copyBinary(imageDir, "maxvm");
            copyBinary(imageDir, mapLibraryName("jvm"));
            copyBinary(imageDir, mapLibraryName("javatest"));
            copyBinary(imageDir, mapLibraryName("prototype"));
            copyBinary(imageDir, mapLibraryName("tele"));

            if (OperatingSystem.current() == OperatingSystem.DARWIN) {
                // Darwin has funky behavior relating to the namespace for native libraries, use a workaround
                exec(null, new String[] {"bin/mod-macosx-javalib.sh", imageDir.getAbsolutePath(), System.getProperty("java.home")}, null, outputFile, true, null, 5);
            }

            generatedImages.put(imageConfig, imageDir);
            return true;
        } else if (exitValue == PROCESS_TIMEOUT) {
            out().println("(image build timed out): " + new File(imageDir, BinaryImageGenerator.getDefaultBootImageFilePath().getName()));
        }
        generatedImages.put(imageConfig, null);
        return false;
    }


    public static void testJavaProgram(String testName, JavaCommand command, File outputDir, File workingDir, File imageDir, String[] filteredLines) {
        if (stopTesting()) {
            return;
        }
        out().print(left50("Running " + testName + ": "));

        // first run the program on the reference JVM
        File jvmOutput = jvmStdoutFile(outputDir, testName);
        int jvmExitValue = runJavaVM(testName, command.copy(), imageDir, workingDir, jvmOutput, javaRunTimeOutOption.getValue());

        // now run the test on each of the MaxVM configurations
        for (String config : maxvmConfigListOption.getValue()) {
            if (stopTesting()) {
                out().println();
                return;
            }
            JavaCommand maxvmCommand = command.copy();
            maxvmCommand.addVMOptions(MaxineTesterConfiguration.getVMOptions(config));
            File maxvmOutput = maxvmStdoutFile(outputDir, testName, config);
            int maxvmExitValue = runMaxineVM(maxvmCommand, imageDir, workingDir, maxvmOutput, maxvmOutput.getName(), javaRunTimeOutOption.getValue());
            if (jvmExitValue != maxvmExitValue) {
                if (maxvmExitValue == PROCESS_TIMEOUT) {
                    final ExpectedResult expected = printFailed(testName, config);
                    addTestResult(testName, String.format("timed out", maxvmExitValue, jvmExitValue), expected);
                } else {
                    final ExpectedResult expected = printFailed(testName, config);
                    addTestResult(testName, String.format("bad exit value [received %d, expected %d]%n  -> see: %s%n  -> see: %s%n  -> see: %s%n  -> see: %s",
                        maxvmExitValue,
                            jvmExitValue,
                        fileRef(jvmOutput),
                        fileRef(stderrFile(jvmOutput)),
                        fileRef(maxvmOutput),
                        fileRef(stderrFile(maxvmOutput))),
                        expected);
                }
            } else if (Files.compareFiles(jvmOutput, maxvmOutput, filteredLines)) {
                ExpectedResult expected = printSuccess(testName, config);
                addTestResult(testName, null, expected);
            } else {
                ExpectedResult expected = printFailed(testName, config);
                addTestResult(testName, String.format("output did not match [compare %s with %s ]", fileRef(jvmOutput), fileRef(maxvmOutput)), expected);
            }
        }
        out().println();
    }

    private static File jvmStdoutFile(File outputDir, String testName) {
        return stdoutFile(outputDir, "JVM_" + testName.replace(' ', '_'), null);
    }

    private static File maxvmStdoutFile(File outputDir, String testName, String config) {
        return stdoutFile(outputDir, "MAXVM_" + testName.replace(' ', '_'), config);
    }

    private static String mapLibraryName(String name) {
        final String libName = System.mapLibraryName(name);
        if (OperatingSystem.current() == OperatingSystem.DARWIN && libName.endsWith(".jnilib")) {
            return Strings.chopSuffix(libName, ".jnilib") + ".dylib";
        }
        return libName;
    }

    /**
     * Copies a binary file from the default output directory used by the {@link BinaryImageGenerator} for
     * the output files it generates to a given directory.
     *
     * @param imageDir the destination directory
     * @param binary the name of the file in the source directory that is to be copied to {@code imageDir}
     */
    private static void copyBinary(File imageDir, String binary) {
        final File defaultImageDir = BinaryImageGenerator.getDefaultBootImageFilePath().getParentFile();
        final File defaultBinaryFile = new File(defaultImageDir, binary);
        final File binaryFile = new File(imageDir, binary);
        try {
            Files.copy(defaultBinaryFile, binaryFile);
            binaryFile.setExecutable(true);
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }
    }

    private static File getOutputFile(File outputDir, String outputFileName, String imageConfig, String suffix) {
        final String configString = imageConfig == null ? "" : "_" + imageConfig;
        final File file = new File(outputDir, outputFileName + configString + suffix);
        makeDirectory(file.getParentFile());
        return file;
    }

    private static File stdoutFile(File outputDir, String outputFileName, String imageConfig) {
        return getOutputFile(outputDir, outputFileName, imageConfig, ".stdout");
    }

    private static File stderrFile(File outputFile) {
        if (outputFile.getName().endsWith("stdout")) {
            return new File(Strings.chopSuffix(outputFile.getAbsolutePath(), "stdout") + "stderr");
        }
        return new File(outputFile.getAbsolutePath() + ".stderr");
    }

    private static File commandFile(File outputFile) {
        if (outputFile.getName().endsWith("stdout")) {
            return new File(Strings.chopSuffix(outputFile.getAbsolutePath(), "stdout") + "command");
        }
        return new File(outputFile.getAbsolutePath() + ".command");
    }

    private static String[] defaultJVMOptions() {
        final String value = javaVMArgsOption.getValue();
        if (value == null) {
            return null;
        }
        final String javaVMArgs = value.trim();
        return javaVMArgs.split("\\s+");
    }

    private static String escapeShellCharacters(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        for (int cursor = 0; cursor < s.length(); ++cursor) {
            final char cursorChar = s.charAt(cursor);
            if (cursorChar == '$') {
                sb.append("\\$");
            } else if (cursorChar == ' ') {
                sb.append("\\ ");
            } else {
                sb.append(cursorChar);
            }
        }
        return sb.toString();
    }

    private static int exec(File workingDir, String[] command, String[] env, File outputFile, String name, int timeout) {
        return exec(workingDir, command, env, outputFile, false, name, timeout);
    }

    private static final Map<String, Long> execTimes = Collections.synchronizedMap(new LinkedHashMap<String, Long>());

    /**
     * Executes a command in a sub-process. The execution uses a shell command to perform redirection of the standard
     * output and error streams.
     *
     * @param workingDir the working directory of the subprocess, or {@code null} if the subprocess should inherit the
     *            working directory of the current process
     * @param command the command and arguments to be executed
     * @param env array of strings, each element of which has environment variable settings in the format
     *            <i>name</i>=<i>value</i>, or <tt>null</tt> if the subprocess should inherit the environment of the
     *            current process
     * @param outputFile the file to which stdout and stderr should be redirected or {@code null} if these output
     *            streams are to be discarded
     * @param name a descriptive name for the command or {@code null} if {@code command[0]} should be used instead
     * @param timeout the timeout in seconds
     * @return
     */
    private static int exec(File workingDir, String[] command, String[] env, File outputFile, boolean append, String name, int timeout) {
        traceExec(workingDir, command);
        final long start = System.currentTimeMillis();
        try {
            final StringBuilder sb = new StringBuilder("exec ");
            for (String s : command) {
                sb.append(escapeShellCharacters(s)).append(' ');
            }
            if (outputFile != null) {
                sb.append((append ? ">>" : ">") + outputFile.getAbsolutePath());
                sb.append((append ? " 2>>" : " 2>") + stderrFile(outputFile));
            } else {
                sb.append(">/dev/null");
                sb.append(" 2>&1");
            }

            final String[] cmdarray = new String[] {"sh", "-c", sb.toString()};

            if (outputFile != null) {
                final File commandFile = commandFile(outputFile);
                final PrintStream ps = new PrintStream(new FileOutputStream(commandFile));
                ps.println(Arrays.toString(cmdarray, " "));
                for (int i = 0; i < cmdarray.length; ++i) {
                    ps.println("Command array[" + i + "] = \"" + cmdarray[i] + "\"");
                }
                ps.println("Working directory: " + (workingDir == null ? "CWD" : workingDir.getAbsolutePath()));
                ps.close();
            }

            final Process process = Runtime.getRuntime().exec(cmdarray, env, workingDir);
            final ProcessTimeoutThread processThread = new ProcessTimeoutThread(process, name != null ? name : command[0], timeout);
            final int exitValue = processThread.exitValue();
            return exitValue;
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
        } finally {
            if (name != null) {
                synchronized (execTimes) {
                    String key = name;
                    if (execTimes.containsKey(key)) {
                        int unique = 1;
                        do {
                            key = name + " (" + unique + ")";
                            unique++;
                        } while (execTimes.containsKey(key));
                    }
                    execTimes.put(key, System.currentTimeMillis() - start);
                }
            }
        }
    }

    private static void traceExec(File workingDir, String[] command) {
        if (Trace.hasLevel(2)) {
            final PrintStream stream = Trace.stream();
            synchronized (stream) {
                if (workingDir == null) {
                    stream.println("Executing process in current directory");
                } else {
                    stream.println("Executing process in directory: " + workingDir);
                }
                stream.print("Command line:");
                for (String c : command) {
                    stream.print(" " + c);
                }
                stream.println();
            }
        }
    }

    static String findLine(File file, String p1, String p2) {
        try {
            final BufferedReader f1Reader = new BufferedReader(new FileReader(file));
            try {
                String line1;
                while (true) {
                    line1 = f1Reader.readLine();
                    if (line1 == null) {
                        return null;
                    }
                    if (line1.contains(p1)) {
                        if (p2 != null && line1.contains(p2)) {
                            return line1;
                        }
                    }
                }
            } finally {
                f1Reader.close();
            }
        } catch (IOException e) {
            return null;
        }

    }

    /**
     * A dedicated thread to wait for the process and terminate it if it gets stuck.
     *
     * @author Ben L. Titzer
     */
    private static class ProcessTimeoutThread extends Thread {

        private final Process process;
        private final int timeoutMillis;
        protected Integer exitValue;
        private boolean timedOut;

        public ProcessTimeoutThread(Process process, String name, int timeoutSeconds) {
            super(name);
            this.process = process;
            this.timeoutMillis = 1000 * timeoutSeconds;
        }

        @Override
        public void run() {
            try {
                // Sleep for the prescribed timeout duration
                Thread.sleep(timeoutMillis);

                // Not interrupted: terminate associated process
                timedOut = true;
                process.destroy();
            } catch (InterruptedException e) {
                // Process completed within timeout
            }
        }

        public int exitValue() throws IOException {
            start();
            try {
                exitValue = process.waitFor();
                // Process exited: interrupt timeout thread so that it stops
                interrupt();
            } catch (InterruptedException interruptedException) {
                // do nothing.
            }

            try {
                // Wait for timeout thread to stop
                join();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }

            if (timedOut) {
                exitValue = PROCESS_TIMEOUT;
            }
            return exitValue;
        }
    }

    public interface Harness {
        boolean run();
    }

    /**
     * This class implements a harness capable of running the JUnit test cases in another VM.
     *
     * @author Ben L. Titzer
     */
    public static class JUnitHarness implements Harness {
        final String[] testList;
        public JUnitHarness(String[] testList) {
            this.testList = testList;
        }

        public boolean run() {
            final File outputDir = new File(outputDirOption.getValue(), "junit-tests");
            final Set<String> junitTests = new TreeSet<String>();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (className.endsWith(".AutoTest")) {
                        if (testList == null) {
                            junitTests.add(className);
                        } else {
                            for (String test : testList) {
                                if (className.contains(test)) {
                                    junitTests.add(className);
                                }
                            }
                        }
                    }
                    return true;
                }
            }.run(Classpath.fromSystem());

            if (singleThreadedOption.getValue()) {
                for (final String junitTest : junitTests) {
                    if (!stopTesting()) {
                        runJUnitTest(outputDir, junitTest);
                    }
                }
            } else {
                final int availableProcessors = Math.min(4, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
                final ExecutorService junitTesterService = Executors.newFixedThreadPool(availableProcessors);
                final CompletionService<Void> junitTesterCompletionService = new ExecutorCompletionService<Void>(junitTesterService);
                for (final String junitTest : junitTests) {
                    junitTesterCompletionService.submit(new Runnable() {
                        public void run() {
                            if (!stopTesting()) {
                                runWithSerializedOutput(new Runnable() {
                                    public void run() {
                                        runJUnitTest(outputDir, junitTest);
                                    }
                                });
                            }
                        }
                    }, null);
                }
                junitTesterService.shutdown();
                try {
                    junitTesterService.awaitTermination(javaTesterTimeOutOption.getValue() * 2 * junitTests.size(), TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        /**
         * Runs a single JUnit test.
         *
         * @param outputDir where the result logs of the JUnit test are to be placed
         * @param junitTest the JUnit test to run
         */
        private void runJUnitTest(final File outputDir, String junitTest) {
            final File outputFile = stdoutFile(outputDir, junitTest, null);
            final File passedFile = getOutputFile(outputDir, junitTest, null, ".passed");
            final File failedFile = getOutputFile(outputDir, junitTest, null, ".failed");

            final JavaCommand javaCommand = new JavaCommand(JUnitTestRunner.class);
            javaCommand.addVMOptions(defaultJVMOptions());
            javaCommand.addClasspath(System.getProperty("java.class.path"));
            javaCommand.addArgument(junitTest);
            javaCommand.addArgument(passedFile.getName());
            javaCommand.addArgument(failedFile.getName());
            if (slowAutoTestsOption.getValue()) {
                javaCommand.addSystemProperty(JUnitTestRunner.INCLUDE_SLOW_TESTS_PROPERTY, null);
            }

            final String[] command = javaCommand.getExecutableCommand(javaExecutableOption.getValue());

            final PrintStream out = out();

            out.println("JUnit auto-test: Started " + junitTest);
            out.flush();
            final long start = System.currentTimeMillis();
            final int exitValue = exec(outputDir, command, null, outputFile, junitTest, junitTestTimeOutOption.getValue());
            out.print("JUnit auto-test: Stopped " + junitTest);

            final Set<String> unexpectedResults = new HashSet<String>();
            parseAutoTestResults(passedFile, true, unexpectedResults);
            parseAutoTestResults(failedFile, false, unexpectedResults);

            if (exitValue != 0) {
                if (exitValue == PROCESS_TIMEOUT) {
                    out.print(" (timed out)");
                } else {
                    out.print(" (exit value == " + exitValue + ")");
                }
                junitTestsWithExceptions.append(junitTest);
            }
            final long runTime = System.currentTimeMillis() - start;
            out.println(" [Time: " + NumberFormat.getInstance().format((double) runTime / 1000) + " seconds]");
            for (String unexpectedResult : unexpectedResults) {
                out.println("    " + unexpectedResult);
            }
            if (!unexpectedResults.isEmpty()) {
                out.println("    see: " + fileRef(outputFile));
                out.println("    see: " + fileRef(stderrFile(outputFile)));
            }
        }

        /**
         * Parses a file of test names (one per line) run as part of an auto-test. The global records of test results are
         * {@linkplain MaxineTester#addTestResult(String, String, test.com.sun.max.vm.MaxineTesterConfiguration.ExpectedResult) updated} appropriately.
         *
         * @param resultsFile the file to parse
         * @param passed specifies if the file list tests that passed or failed
         * @param unexpectedResults if non-null, then all unexpected test results are added to this set
         */
        void parseAutoTestResults(File resultsFile, boolean passed, Set<String> unexpectedResults) {
            try {
                final Sequence<String> lines = Files.readLines(resultsFile);
                for (String line : lines) {
                    final String testName = line;
                    final boolean expectedResult = addTestResult(testName, passed ? null : "failed", MaxineTesterConfiguration.expectedResult(testName, null));
                    if (unexpectedResults != null && !expectedResult) {
                        unexpectedResults.add("unexpectedly "  + (passed ? "passed " : "failed ") + testName);
                    }
                }
            } catch (IOException ioException) {
                out().println("could not read '" + resultsFile.getAbsolutePath() + "': " + ioException);
            }
        }
    }

    /**
     * This class implements a test harness that builds a Maxine VM image and then runs
     * the JavaTester with that VM in a remote process.
     *
     * @author Ben L. Titzer
     * @author Doug Simon
     */
    public static class JavaTesterHarness implements Harness {
        private static final Pattern TEST_BEGIN_LINE = Pattern.compile("(\\d+): +(\\S+)\\s+next: '-XX:TesterStart=(\\d+)', end: '-XX:TesterEnd=(\\d+)'");

        public boolean run() {
            final List<String> javaTesterConfigs = javaTesterConfigsOption.getValue();
            for (final String config : javaTesterConfigs) {
                if (!stopTesting()) {
                    JavaTesterHarness.runJavaTesterTests(config);
                }
            }
            return true;
        }

        private static JavaTesterResult parseJavaTesterOutputFile(String config, File outputFile) {
            String nextTestOption = null;
            String lastTest = null;
            String lastTestNumber = null;
            try {
                BufferedReader reader = new BufferedReader(new FileReader(outputFile));
                AppendableSequence<String> failedLines = new ArrayListSequence<String>();
                try {
                    while (true) {
                        String line = reader.readLine();

                        if (line == null) {
                            break;
                        }

                        Matcher matcher = JavaTesterHarness.TEST_BEGIN_LINE.matcher(line);
                        if (matcher.matches()) {
                            if (lastTest != null) {
                                addTestResult(lastTest, null);
                            }
                            lastTestNumber = matcher.group(1);
                            lastTest = matcher.group(2);
                            String nextTestNumber = matcher.group(3);
                            String endTestNumber = matcher.group(4);
                            if (!nextTestNumber.equals(endTestNumber)) {
                                nextTestOption = "-XX:TesterStart=" + nextTestNumber;
                            } else {
                                nextTestOption = null;
                            }

                        } else if (line.contains("failed")) {
                            failedLines.append(line); // found a line with "failed"--probably a failed test
                            // (tw) bug?
                            if (lastTest != null) {
                                addTestResult(lastTest, line);
                            }
                            lastTest = null;
                            lastTestNumber = null;
                        } else if (line.startsWith("Done: ")) {
                            if (lastTest != null) {
                                addTestResult(lastTest, null);
                            }
                            lastTest = null;
                            lastTestNumber = null;
                            // found the terminating line indicating how many tests passed
                            if (failedLines.isEmpty()) {
                                assert nextTestOption == null;
                                return new JavaTesterResult(line, null);
                            }
                            break;
                        }
                    }
                    if (lastTest != null) {
                        addTestResult(lastTest, "never returned a result");
                        failedLines.append("\t" + lastTestNumber + ", " + lastTest + ": crashed or hung the VM");
                    }
                    if (failedLines.isEmpty()) {
                        return new JavaTesterResult("no failures", nextTestOption);
                    }
                    StringBuffer buffer = new StringBuffer("failures: ");
                    for (String failed : failedLines) {
                        buffer.append("\n").append(failed);
                    }
                    return new JavaTesterResult(buffer.toString(), nextTestOption);
                } finally {
                    reader.close();
                }
            } catch (IOException e) {
                return new JavaTesterResult("could not open file: " + outputFile.getPath(), null);
            }
        }

        private static void runJavaTesterTests(String config) {
            final File imageDir = new File(outputDirOption.getValue(), config);

            final PrintStream out = out();
            out.println("Java tester: Started " + config);
            if (skipImageGenOption.getValue() || generateImage(imageDir, config)) {
                String nextTestOption = "-XX:TesterStart=0";
                int executions = 0;
                while (nextTestOption != null) {
                    File outputFile = stdoutFile(imageDir, "JAVA_TESTER" + (executions == 0 ? "" : "-" + executions), config);
                    JavaCommand command = new JavaCommand((Class) null);
                    command.addArgument(nextTestOption);
                    int exitValue = runMaxineVM(command, imageDir, null, outputFile, outputFile.getName(), javaTesterTimeOutOption.getValue());
                    JavaTesterResult result = JavaTesterHarness.parseJavaTesterOutputFile(config, outputFile);
                    String summary = result.summary;
                    nextTestOption = result.nextTestOption;
                    out.print("Java tester: Stopped " + config + " - ");
                    if (exitValue == 0) {
                        out.println(summary);
                    } else if (exitValue == PROCESS_TIMEOUT) {
                        out.println("(timed out): " + summary);
                        out.println("  -> see: " + fileRef(outputFile));
                        out.println("  -> see: " + fileRef(stderrFile(outputFile)));
                    } else {
                        out.println("(exit = " + exitValue + "): " + summary);
                        out.println("  -> see: " + fileRef(outputFile));
                        out.println("  -> see: " + fileRef(stderrFile(outputFile)));
                    }
                    executions++;
                }
            } else {
                out.println("(image build failed)");
                final File outputFile = stdoutFile(imageDir, "IMAGEGEN", config);
                out.println("  -> see: " + fileRef(outputFile));
                out.println("  -> see: " + fileRef(stderrFile(outputFile)));
            }
        }
    }

    /**
     * This class implements a harness that is capable of building the Maxine VM and running
     * a number of Java programs with it, comparing their output to the output obtained
     * from running the same programs on a reference JVM.
     *
     * @author Ben L. Titzer
     */
    public static class OutputHarness implements Harness {
        final Class[] testList;
        OutputHarness(Class[] tests) {
            this.testList = tests;
        }

        public boolean run() {
            final File outputDir = new File(outputDirOption.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                runOutputTests(outputDir, imageDir);
                return true;
            }
            return false;
        }

        void runOutputTests(final File outputDir, final File imageDir) {
            out().println("Output tests key:");
            out().println("      OK: test passed");
            out().println("  failed: test failed (go debug)");
            out().println("  normal: expected failure and failed");
            out().println("  passed: expected failure but passed (consider removing from exclusion list)");
            out().println("  noluck: non-deterministic test failed (ignore)");
            out().println("   lucky: non-deterministic test passed (ignore)");
            for (Class mainClass : testList) {
                runOutputTest(outputDir, imageDir, mainClass);
            }
        }
        void runOutputTest(File outputDir, File imageDir, Class mainClass) {
            final JavaCommand command = new JavaCommand(mainClass);
            for (String option : defaultJVMOptions()) {
                command.addVMOption(option);
            }
            command.addClasspath(System.getProperty("java.class.path"));
            testJavaProgram(mainClass.getName(), command, outputDir, null, imageDir, null);
        }
    }

    /**
     * A timed harness runs programs that time themselves internally (e.g. SpecJVM98 and DaCapo).
     * These programs can serve as benchmarks and their times can be compared to the reference
     * VM's times.
     *
     * @author Ben L. Titzer
     */
    abstract static class TimedHarness {
        void reportTiming(String testName, File outputDir) {
            if (timingOption.getValue()) {
                final long baseline = getInternalTiming(jvmStdoutFile(outputDir, testName));
                out().print(left50("    --> " + testName + " (" + baseline + " ms)"));
                for (String config : maxvmConfigListOption.getValue()) {
                    final long timing = getInternalTiming(maxvmStdoutFile(outputDir, testName, config));
                    if (timing > 0 && baseline > 0) {
                        final float factor = timing / (float) baseline;
                        out().print(left16(config + ": " + Strings.fixedDouble(factor, 3)));
                    } else {
                        out().print(left16(config + ":"));
                    }
                }
                out().println();
            }
        }
        abstract long getInternalTiming(File stdout);
    }

    static File getFileFromOptionOrEnv(Option<File> option, String var) {
        final File value = option.getValue();
        if (value != null) {
            return value;
        }
        final String envValue = System.getenv(var);
        if (envValue != null) {
            return new File(envValue);
        }
        return null;
    }

    /**
     * This class implements a test harness that is capable of running the SpecJVM98 suite of programs
     * and comparing their outputs to that obtained by running each of them on a reference VM. Note that
     * internal timings do not include the VM startup time because they are reported by the program
     * itself. For that reason, external timings (i.e. by recording the total time to run the VM process)
     * should be used as well.
     *
     * @author Ben L. Titzer
     */
    public static class SpecJVM98Harness extends TimedHarness implements Harness {
        final String[] testList;
        SpecJVM98Harness(String[] tests) {
            this.testList = tests;
        }

        public boolean run() {
            final File specjvm98Zip = getFileFromOptionOrEnv(specjvm98ZipOption, "SPECJVM98_ZIP");
            if (specjvm98Zip == null) {
                out().println("Need to specify the location of SpecJVM98 ZIP file with -" + specjvm98ZipOption + " or in the SPECJVM98_ZIP environment variable");
                return false;
            }
            final File outputDir = new File(outputDirOption.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!specjvm98Zip.exists()) {
                    out().println("Couldn't find SpecJVM98 ZIP file " + specjvm98Zip);
                    return false;
                }
                final File specjvm98Dir = new File(outputDirOption.getValue(), "specjvm98");
                Files.unzip(specjvm98Zip, specjvm98Dir);
                for (String test : testList) {
                    runSpecJVM98Test(outputDir, imageDir, specjvm98Dir, test);
                }
                return true;
            }
            return false;
        }

        void runSpecJVM98Test(File outputDir, File imageDir, File workingDir, String test) {
            final String testName = "SpecJVM98 " + test;
            final JavaCommand command = new JavaCommand("SpecApplication");
            command.addClasspath(".");
            command.addArgument(test);
            testJavaProgram(testName, command, outputDir, workingDir, imageDir, MaxineTesterConfiguration.specjvm98IgnoredLinePatterns);
            reportTiming(testName, outputDir);
        }

        @Override
        long getInternalTiming(File stdout) {
            // SpecJVM98 performs internal timing and reports it to stdout in seconds
            String line = findLine(stdout, "======", "Finished in ");
            if (line != null) {
                line = line.substring(line.indexOf("Finished in") + 11);
                line = line.substring(0, line.indexOf(" secs"));
                return (long) (1000 * Float.parseFloat(line));
            }
            return -1;
        }
    }

    /**
     * This class implements a test harness that is capable of running the DaCapo suite of programs
     * and comparing their outputs to that obtained by running each of them on a reference VM.
     *
     * @author Ben L. Titzer
     */
    public static class DaCapoHarness extends TimedHarness implements Harness {
        final String[] testList;
        DaCapoHarness(String[] tests) {
            testList = tests;
        }

        public boolean run() {
            final File dacapoJar = getFileFromOptionOrEnv(dacapoJarOption, "DACAPO_JAR");
            if (dacapoJar == null) {
                out().println("Need to specify the location of Dacapo JAR file with -" + dacapoJarOption + " or in the DACAPO_JAR environment variable");
                return false;
            }
            final File outputDir = new File(outputDirOption.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!dacapoJar.exists()) {
                    out().println("Couldn't find DaCapo JAR file " + dacapoJar);
                    return false;
                }
                for (String test : testList) {
                    runDaCapoTest(outputDir, imageDir, test, dacapoJar);
                }
                return true;
            }
            return false;
        }

        void runDaCapoTest(File outputDir, File imageDir, String test, File dacapoJar) {
            final String testName = "DaCapo " + test;
            final JavaCommand command = new JavaCommand(dacapoJar);
            command.addArgument(test);
            testJavaProgram(testName, command, outputDir, null, imageDir, null);
            reportTiming(testName, outputDir);
        }

        @Override
        long getInternalTiming(File stdout) {
            // DaCapo performs internal timing and reports it to stderr in milliseconds
            String line = findLine(stderrFile(stdout), "===== DaCapo ", "PASSED in ");
            if (line != null) {
                line = line.substring(line.indexOf("PASSED in ") + 10);
                line = line.substring(0, line.indexOf(" msec"));
                return Long.parseLong(line);
            }
            return -1;
        }
    }

    /**
     * This class implements a test harness that is capable of running the Programming Language Shootout suite of programs
     * and comparing their outputs to that obtained by running each of them on a reference VM.
     *
     * @author Ben L. Titzer
     */
    public static class ShootoutHarness implements Harness {
        final String[] testList;
        ShootoutHarness(String[] tests) {
            this.testList = tests;
        }

        public boolean run() {
            final File shootoutDir = getFileFromOptionOrEnv(shootoutDirOption, "SHOOTOUT_DIR");
            if (shootoutDir == null) {
                out().println("Need to specify the location of the Programming Language Shootout directory with -" + shootoutDirOption + " or in the SHOOTOUT_DIR environment variable");
                return false;
            }
            final File outputDir = new File(outputDirOption.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!shootoutDir.exists()) {
                    out().println("Couldn't find shootout directory " + shootoutDir);
                    return false;
                }
                for (String test : testList) {
                    runShootoutTest(outputDir, imageDir, shootoutDir, test);
                }
                return true;
            }
            return false;
        }

        void runShootoutTest(File outputDir, File imageDir, File shootoutDir, String test) {
            final String testName = "Shootout " + test;
            final JavaCommand command = new JavaCommand("shootout." + test);
            command.addClasspath(new File(shootoutDir, "bin").getAbsolutePath());

            for (Object input : MaxineTesterConfiguration.shootoutInputs(test)) {
                final JavaCommand c = command.copy();
                if (input instanceof String) {
                    c.addArgument((String) input);
                    testJavaProgram(testName + "-" + input, c, outputDir, null, imageDir, null);
                }
            }
        }

    }
}

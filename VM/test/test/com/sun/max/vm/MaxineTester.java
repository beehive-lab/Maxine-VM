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
import org.junit.internal.runners.*;
import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.junit.runners.AllTests;

import sun.management.*;

import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.io.Streams.*;
import com.sun.max.lang.*;
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

    private static final OptionSet _options = new OptionSet();
    private static final Option<String> _outputDir = _options.newStringOption("output-dir", "maxine-tester",
                    "The output directory for the results of the maxine tester.");
    private static final Option<Integer> _imageBuildTimeOut = _options.newIntegerOption("image-build-timeout", 600,
                    "The number of seconds to wait for an image build to complete before " +
                    "timing out and killing it.");
    private static final Option<String> _javaExecutable = _options.newStringOption("java-executable", "java",
                    "The name of or full path to the Java VM executable to use. This must be a JDK 6 or greater VM.");
    private static final Option<Integer> _javaTesterTimeOut = _options.newIntegerOption("java-tester-timeout", 50,
                    "The number of seconds to wait for the in-target Java tester tests to complete before " +
                    "timing out and killing it.");
    private static final Option<Integer> _javaTesterConcurrency = _options.newIntegerOption("java-tester-concurrency", 1,
                    "The number of Java tester tests to run in parallel.");
    private static final Option<Integer> _javaRunTimeOut = _options.newIntegerOption("java-run-timeout", 50,
                    "The number of seconds to wait for the target VM to complete before " +
                    "timing out and killing it when running user programs.");
    private static final Option<Boolean> _skipOutputTests = _options.newBooleanOption("skip-output-tests", false,
                    "Skip running of the output tests.");
    private static final Option<Boolean> _skipJavaTesterTests = _options.newBooleanOption("skip-java-tester-tests", false,
                    "Skip running of the Java Tester tests.");
    private static final Option<Integer> _traceOption = _options.newIntegerOption("trace", 0,
                    "The tracing level for building the images and running the tests.");
    private static final Option<Boolean> _skipImageGen = _options.newBooleanOption("skip-image-gen", false,
                    "Skip the generation of the image, which is useful for testing the Maxine tester itself.");
    private static final Option<List<String>> _javaTesterConfigs = _options.newStringListOption("java-tester-configs",
                    MaxineTesterConfiguration.defaultJavaTesterConfigs(),
                    "A list of configurations for which to run the Java tester tests.");
    private static final Option<List<String>> _maxvmConfigList = _options.newStringListOption("maxvm-configs",
                    MaxineTesterConfiguration.defaultMaxvmOutputConfigs(),
                    "A list of configurations for which to run the Maxine output tests.");
    private static final Option<String> _javaConfigAliasOption = _options.newStringOption("java-config-alias", null,
                    "The Java tester config to use for running Java programs. Omit this option to use a separate config for Java programs.");
    private static final Option<Integer> _autoTestTimeOut = _options.newIntegerOption("auto-test-timeout", 300,
                    "The number of seconds to wait for a JUnit auto-test to complete before " +
                    "timing out and killing it.");
    private static final Option<Boolean> _skipAutoTests = _options.newBooleanOption("skip-auto-tests", false,
                    "Skip running of the JUnit auto-test classes found on the class path.");
    private static final Option<Boolean> _slowAutoTests = _options.newBooleanOption("slow-auto-tests", false,
                    "Include auto-tests known to be slow.");
    private static final Option<String> _autoTestFilter = _options.newStringOption("auto-test-filter", null,
                    "A pattern for selecting which auto-tests are run. If absent, all auto-tests on the class path are run. " +
                    "Otherwise only those whose name contains this value as a substring are run.");
    private static final Option<Boolean> _failFast = _options.newBooleanOption("fail-fast", true,
                    "Stop execution as soon a non-zero exit value will be the results of the tests.");

    private static String _javaConfigAlias = null;

    public static void main(String[] args) {
        try {
            _options.parseArguments(args);
            _javaConfigAlias = _javaConfigAliasOption.getValue();
            if (_javaConfigAlias != null) {
                ProgramError.check(MaxineTesterConfiguration._imageConfigs.containsKey(_javaConfigAlias), "Unknown Java tester config '" + _javaConfigAlias + "'");
            }
            final File outputDir = new File(_outputDir.getValue()).getAbsoluteFile();
            makeDirectory(outputDir);
            Trace.on(_traceOption.getValue());
            runAutoTests();
            buildJavaRunSchemeAndRunOutputTests();
            runJavaTesterTests();
            System.exit(reportTestResults(out()));
        } catch (Throwable throwable) {
            throwable.printStackTrace(err());
            System.exit(-1);
        }
    }

    private static final ThreadLocal<PrintStream> _out = new ThreadLocal<PrintStream>() {
        @Override
        protected PrintStream initialValue() {
            return System.out;
        }
    };
    private static final ThreadLocal<PrintStream> _err = new ThreadLocal<PrintStream>() {
        @Override
        protected PrintStream initialValue() {
            return System.err;
        }
    };

    private static PrintStream out() {
        return _out.get();
    }
    private static PrintStream err() {
        return _err.get();
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
        for (Class mainClass : MaxineTesterConfiguration._outputTestClasses) {
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
    private static final Map<String, String> _unexpectedFailures = Collections.synchronizedMap(new TreeMap<String, String>());
    private static final Map<String, String> _unexpectedPasses = Collections.synchronizedMap(new TreeMap<String, String>());

    /**
     * Adds a test result to the global set of test results.
     *
     * @param testName the unique name of the test
     * @param failure a failure message or null if the test passed
     * @param expectedFailure {@code true} if this test was expected to fail.
     * @return {@code true} if the result (pass or fail) of the test matches the expected result, {@code false} otherwise
     */
    private static boolean addTestResult(String testName, String failure, boolean expectedFailure) {
        if (expectedFailure && failure == null) {
            _unexpectedPasses.put(testName, failure);
            return false;
        } else if (!expectedFailure && failure != null) {
            _unexpectedFailures.put(testName, failure);
            return false;
        }
        return true;
    }

    private static boolean addTestResult(String testName, String failure) {
        return addTestResult(testName, failure, MaxineTesterConfiguration.isExpectedFailure(testName, null));
    }

    /**
     * Summarizes the collected test results.
     *
     * @param out where the summary should be printed. This value can be null if only the return value is of interest.
     * @return an integer that is the total of all the unexpected passes, the unexpected failures, the number of failed
     *         attempts to generate an image and the number of auto-tests subprocesses that failed with an exception
     */
    private static int reportTestResults(PrintStream out) {
        if (out != null) {
            out.println();
            out.println("MaxineTester Summary:");
        }
        int failedImages = 0;
        for (Map.Entry<String, File> entry : _generatedImages.entrySet()) {
            if (entry.getValue() == null) {
                if (out != null) {
                    out.println("Failed building image for configuration '" + entry.getKey() + "'");
                }
                failedImages++;
            }
        }

        int failedAutoTests = 0;
        for (String autoTest : _autoTestsWithExceptions) {
            if (out != null) {
                out.println("Non-zero exit status for'" + autoTest + "'");
            }
            failedAutoTests++;
        }

        if (out != null) {
            if (!_unexpectedFailures.isEmpty()) {
                out.println("Unexpected failures:");
                for (Map.Entry<String, String> entry : _unexpectedFailures.entrySet()) {
                    out.println("  " + entry.getKey() + "  " + entry.getValue());
                }
            }
            if (!_unexpectedPasses.isEmpty()) {
                out.println("Unexpected passes:");
                for (String testName : _unexpectedPasses.keySet()) {
                    out.println("  " + testName);
                }
            }
        }

        final int exitCode = _unexpectedFailures.size() + _unexpectedPasses.size() + failedImages + failedAutoTests;
        if (out != null) {
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
            final Runner runner = new OldTestClassRunner(test) {
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
                boolean _failed;

                @Override
                public void testStarted(Description description) throws Exception {
                    System.out.println("running " + description);
                }

                @Override
                public void testFailure(Failure failure) throws Exception {
                    failure.getException().printStackTrace(System.out);
                    _failed = true;
                }

                @Override
                public void testFinished(Description description) throws Exception {
                    if (_failed) {
                        failed.println(description);
                    } else {
                        passed.println(description);
                    }
                    _failed = false;
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
     * A list of the {@linkplain #runAutoTests auto-tests} that caused the Java process to exit with an exception.
     */
    private static AppendableSequence<String> _autoTestsWithExceptions = new ArrayListSequence<String>();

    /**
     * Parses a file of test names (one per line) run as part of an auto-test. The global records of test results are
     * {@linkplain #addTestResult(String, String, boolean) updated} appropriately.
     *
     * @param resultsFile the file to parse
     * @param passed specifies if the file list tests that passed or failed
     * @param unexpectedResults if non-null, then all unexpected test results are added to this set
     */
    static void parseAutoTestResults(File resultsFile, boolean passed, Set<String> unexpectedResults) {
        try {
            final Sequence<String> lines = Files.readLines(resultsFile);
            for (String line : lines) {
                final String testName = line;
                final boolean expectedFailure = MaxineTesterConfiguration.isExpectedFailure(testName, null);
                final boolean expectedResult = addTestResult(testName, passed ? null : "failed", expectedFailure);
                if (unexpectedResults != null && !expectedResult) {
                    unexpectedResults.add("unexpectedly "  + (passed ? "passed" : "failed") + testName);
                }
            }
        } catch (IOException ioException) {
            out().println("could not read '" + resultsFile.getAbsolutePath() + "': " + ioException);
        }
    }

    /**
     * Determines if {@linkplain #_failFast fail fast} has been requested and at least one unexpected failure has
     * occurred.
     */
    static boolean stopTesting() {
        return _failFast.getValue() && reportTestResults(null) != 0;
    }

    /**
     * Runs all the auto-tests available on the class path. An auto-test is a class whose unqualified name is "AutoTest"
     * that resides in a sub-package of the {@code test.com.sun.max} package. These classes are assumed to contain one
     * or more JUnit tests that can be run via {@link JUnitCore}.
     */
    private static void runAutoTests() {
        if (_skipAutoTests.getValue() || stopTesting()) {
            return;
        }
        final File outputDir = new File(_outputDir.getValue(), "auto-tests");

        final String filter = _autoTestFilter.getValue();
        final Set<String> autoTests = new TreeSet<String>();
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                if (className.startsWith(new test.com.sun.max.Package().name()) && className.endsWith(".AutoTest")) {
                    if (filter == null || className.contains(filter)) {
                        autoTests.add(className);
                    }
                }
                return true;
            }
        }.run(Classpath.fromSystem());

        final int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
        final ExecutorService autoTesterService = Executors.newFixedThreadPool(availableProcessors);
        final CompletionService<Void> autoTesterCompletionService = new ExecutorCompletionService<Void>(autoTesterService);
        for (final String autoTest : autoTests) {
            autoTesterCompletionService.submit(new Runnable() {
                public void run() {
                    runAutoTest(outputDir, autoTest);
                }

            }, null);
        }
        autoTesterService.shutdown();
        try {
            autoTesterService.awaitTermination(_javaTesterTimeOut.getValue() * 2 * autoTests.size(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Runs a single {@linkplain #runAutoTests() auto-test}.
     *
     * @param outputDir where the result logs of the auto-test are to be placed
     * @param autoTest the auto-test to run
     */
    private static void runAutoTest(final File outputDir, String autoTest) {
        if (stopTesting()) {
            return;
        }

        final File outputFile = getOutputFile(outputDir, autoTest, null);
        final File passedFile = getOutputFile(outputDir, autoTest, null, ".passed");
        final File failedFile = getOutputFile(outputDir, autoTest, null, ".failed");

        String[] systemProperties = null;
        if (_slowAutoTests.getValue()) {
            systemProperties = new String[] {JUnitTestRunner.INCLUDE_SLOW_TESTS_PROPERTY};
        }

        final String[] javaArgs = buildJavaArgs(JUnitTestRunner.class, null, new String[] {autoTest, passedFile.getName(), failedFile.getName()}, systemProperties);
        final String[] command = appendArgs(new String[] {_javaExecutable.getValue()}, javaArgs);

        final ByteArrayPrintStream out = new ByteArrayPrintStream();

        out.println("JUnit auto-test: Started " + autoTest);
        final long start = System.currentTimeMillis();
        final int exitValue = exec(outputDir, command, outputFile, autoTest, _autoTestTimeOut.getValue());
        out.print("JUnit auto-test: Stopped " + autoTest);

        final Set<String> unexpectedResults = new HashSet<String>();
        parseAutoTestResults(passedFile, true, unexpectedResults);
        parseAutoTestResults(failedFile, false, unexpectedResults);

        if (exitValue != 0) {
            if (exitValue == PROCESS_TIMEOUT) {
                out.print(" (timed out)");
            } else {
                out.print(" (exit value == " + exitValue + ")");
            }
            _autoTestsWithExceptions.append(autoTest);
        }
        final long runTime = System.currentTimeMillis() - start;
        out.println(" [Time: " + NumberFormat.getInstance().format((double) runTime / 1000) + " seconds]");
        for (String unexpectedResult : unexpectedResults) {
            out.println("    " + unexpectedResult);
        }
        if (!unexpectedResults.isEmpty()) {
            out.println("    see: " + outputFile.getAbsolutePath());
        }

        synchronized (out()) {
            out.writeTo(out());
            out().flush();
        }
    }

    private static void runJavaTesterTests() {
        if (stopTesting() || _skipJavaTesterTests.getValue()) {
            return;
        }
        final List<String> javaTesterConfigs = _javaTesterConfigs.getValue();

        final ExecutorService javaTesterService = Executors.newFixedThreadPool(_javaTesterConcurrency.getValue());
        final CompletionService<Void> javaTesterCompletionService = new ExecutorCompletionService<Void>(javaTesterService);
        for (final String config : javaTesterConfigs) {
            javaTesterCompletionService.submit(new Runnable() {
                public void run() {
                    runJavaTesterTests(config);
                }
            }, null);
        }

        javaTesterService.shutdown();
        try {
            javaTesterService.awaitTermination(_javaTesterTimeOut.getValue() * 2 * javaTesterConfigs.size(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
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

    private static void runJavaTesterTests(String config) {
        if (stopTesting()) {
            return;
        }
        final File imageDir = new File(_outputDir.getValue(), config);

        PrintStream out = out();
        if (_javaTesterConcurrency.getValue() != 1) {
            out = new ByteArrayPrintStream();
        }

        out.println("Java tester: Started " + config);
        if (_skipImageGen.getValue() || generateImage(imageDir, config)) {
            String nextTestOption = "-XX:TesterStart=0";
            int executions = 0;
            while (nextTestOption != null) {
                final File outputFile = getOutputFile(imageDir, "JAVA_TESTER" + (executions == 0 ? "" : "-" + executions), config);
                final int exitValue = runMaxineVM(null, new String[] {nextTestOption}, imageDir, outputFile, _javaTesterTimeOut.getValue());
                final JavaTesterResult result = parseJavaTesterOutputFile(config, outputFile);
                final String summary = result._summary;
                nextTestOption = result._nextTestOption;
                out.print("Java tester: Stopped " + config + " - ");
                if (exitValue == 0) {
                    out.println(summary);
                } else if (exitValue == PROCESS_TIMEOUT) {
                    out.println("(timed out): " + summary);
                    out.println("  -> see: " + outputFile.getAbsolutePath());
                } else {
                    out.println("(exit = " + exitValue + "): " + summary);
                    out.println("  -> see: " + outputFile.getAbsolutePath());
                }
                executions++;
            }
        } else {
            out.println("(image build failed)");
            final File outputFile = getOutputFile(imageDir, "IMAGEGEN", config);
            out.println("  -> see: " + outputFile.getAbsolutePath());
        }

        if (_javaTesterConcurrency.getValue() != 1) {
            synchronized (out()) {
                ((ByteArrayPrintStream) out).writeTo(out());
            }
        }
    }

    private static void buildJavaRunSchemeAndRunOutputTests() {
        if (_skipOutputTests.getValue() || stopTesting()) {
            return;
        }
        final String config = _javaConfigAlias == null ? "java" : _javaConfigAlias;
        final File outputDir = new File(_outputDir.getValue(), "java");
        final File imageDir = new File(_outputDir.getValue(), config);
        out().println("Building Java run scheme: started");
        if (_skipImageGen.getValue() || generateImage(imageDir, config)) {
            out().println("Building Java run scheme: OK");
            for (Class mainClass : MaxineTesterConfiguration._outputTestClasses) {
                runOutputTest(outputDir, imageDir, mainClass);
            }
        } else {
            out().println("Building Java run scheme: failed");
            final File outputFile = getOutputFile(imageDir, "IMAGEGEN", config);
            out().println("  -> see: " + outputFile.getAbsolutePath());
        }
    }

    private static void runOutputTest(File outputDir, File imageDir, Class mainClass) {
        if (stopTesting()) {
            return;
        }
        out().print(left50("Running " + mainClass.getName() + ": "));
        final File javaOutput = getOutputFile(outputDir, "JVM_" + mainClass.getSimpleName(), null);

        final String[] args = buildJavaArgs(mainClass, null, null, null);
        final int javaExitValue = runJavaVM(mainClass, args, imageDir, javaOutput, _javaRunTimeOut.getValue());
        for (String config : _maxvmConfigList.getValue()) {
            runMaxineVMOutputTest(config, outputDir, imageDir, mainClass, javaOutput, javaExitValue);
        }
        out().println();
    }
    private static String left50(final String str) {
        return Strings.padLengthWithSpaces(str, 50);
    }

    private static String left16(final String str) {
        return Strings.padLengthWithSpaces(str, 16);
    }

    private static boolean printFailed(Class mainClass, String config) {
        final boolean expected = MaxineTesterConfiguration.isExpectedFailure(mainClass.getName(), config);
        if (expected) {
            out().print(left16(config + ": (normal)"));
        } else {
            out().print(left16(config + ": (failed)"));
        }
        out().flush();
        return expected;
    }

    private static boolean printSuccess(Class mainClass, String config) {
        final boolean expected = MaxineTesterConfiguration.isExpectedFailure(mainClass.getName(), config);
        if (expected) {
            out().print(left16(config + ": (passed)"));
        } else {
            out().print(left16(config + ": OK"));
        }
        out().flush();
        return expected;
    }

    private static void runMaxineVMOutputTest(String config, File outputDir, File imageDir, Class mainClass, final File javaOutput, final int javaExitValue) {
        final String[] vmOptions = MaxineTesterConfiguration.getVMOptions(config);
        final String[] args = buildJavaArgs(mainClass, vmOptions, null, null);
        final File maxvmOutput = getOutputFile(outputDir, "MAXVM_" + mainClass.getSimpleName(), config);
        final int maxineExitValue = runMaxineVM(mainClass, args, imageDir, maxvmOutput, _javaRunTimeOut.getValue());
        if (javaExitValue != maxineExitValue) {
            if (maxineExitValue == PROCESS_TIMEOUT) {
                final boolean expected = printFailed(mainClass, config);
                addTestResult(mainClass.getName(), String.format("timed out", maxineExitValue, javaExitValue), expected);
            } else {
                final boolean expected = printFailed(mainClass, config);
                addTestResult(mainClass.getName(), String.format("bad exit value [received %d, expected %d]", maxineExitValue, javaExitValue), expected);
            }
        } else if (compareFiles(javaOutput, maxvmOutput)) {
            final boolean expected = printSuccess(mainClass, config);
            addTestResult(mainClass.getName(), null, expected);
        } else {
            final boolean expected = printFailed(mainClass, config);
            addTestResult(mainClass.getName(), String.format("output did not match [compare %s with %s]", javaOutput.getPath(), maxvmOutput.getPath()), expected);
        }
    }

    private static boolean compareFiles(File f1, File f2) {
        try {
            final FileInputStream f1Stream = new FileInputStream(f1);
            final FileInputStream f2Stream = new FileInputStream(f2);
            try {
                return Streams.equals(f1Stream, f2Stream);
            } finally {
                f1Stream.close();
                f2Stream.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    static class JavaTesterResult {
        final String _summary;
        final String _nextTestOption;

        JavaTesterResult(String summary, String nextTestOption) {
            _nextTestOption = nextTestOption;
            _summary = summary;
        }

    }

    private static final Pattern TEST_BEGIN_LINE = Pattern.compile("(\\d+): +(\\S+)\\s+next: '-XX:TesterStart=(\\d+)', end: '-XX:TesterEnd=(\\d+)'");

    private static JavaTesterResult parseJavaTesterOutputFile(String config, File outputFile) {
        String nextTestOption = null;
        String lastTest = null;
        String lastTestNumber = null;
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(outputFile));
            final AppendableSequence<String> failedLines = new ArrayListSequence<String>();
            try {
                while (true) {
                    final String line = reader.readLine();

                    if (line == null) {
                        break;
                    }

                    final Matcher matcher = TEST_BEGIN_LINE.matcher(line);
                    if (matcher.matches()) {
                        if (lastTest != null) {
                            addTestResult(lastTest, null);
                        }
                        lastTestNumber = matcher.group(1);
                        lastTest = matcher.group(2);
                        final String nextTestNumber = matcher.group(3);
                        final String endTestNumber = matcher.group(4);
                        if (!nextTestNumber.equals(endTestNumber)) {
                            nextTestOption = "-XX:TesterStart=" + nextTestNumber;
                        } else {
                            nextTestOption = null;
                        }

                    } else if (line.contains("failed")) {
                        failedLines.append(line); // found a line with "failed"--probably a failed test
                        addTestResult(lastTest, line);
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
                    failedLines.append("\t" + lastTestNumber + ": crashed or hung the VM");
                }
                if (failedLines.isEmpty()) {
                    return new JavaTesterResult("no failures", nextTestOption);
                }
                final StringBuffer buffer = new StringBuffer("failures: ");
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

    private static int runMaxineVM(Class mainClass, String[] args, File imageDir, File outputFile, int timeout) {
        final String name = imageDir.getName() + "/maxvm" + (mainClass == null ? "" : " " + mainClass.getName());
        if (mainClass != null && _javaConfigAlias != null) {
            return exec(imageDir, appendArgs(new String[] {"./maxvm"}, args), outputFile, name, timeout);
        }
        return exec(imageDir, appendArgs(new String[] {"./maxvm"}, args), outputFile, name, timeout);
    }

    private static int runJavaVM(Class mainClass, String[] args, File imageDir, File outputFile, int timeout) {
        final String name = "Executing " + mainClass.getName();
        return exec(imageDir, appendArgs(new String[] {_javaExecutable.getValue()}, args), outputFile, name, timeout);
    }

    /**
     * Map from configuration names to the directory in which the image for the configuration was created.
     * If the image generation failed for a configuration, then it will have a {@code null} entry in this map.
     */
    private static final Map<String, File> _generatedImages = new HashMap<String, File>();

    public static boolean generateImage(File imageDir, String imageConfig) {
        if (_generatedImages.containsKey(imageConfig)) {
            return _generatedImages.get(imageConfig) != null;
        }
        final String[] generatorArguments = MaxineTesterConfiguration._imageConfigs.get(imageConfig);
        if (generatorArguments == null) {
            ProgramError.unexpected("unknown image configuration: " + imageConfig);
        }
        Trace.line(2, "Generating image for " + imageConfig + " configuration...");
        final String[] imageArguments = appendArgs(new String[] {"-output-dir=" + imageDir, "-trace=1"}, generatorArguments);
        final String[] vmOptions = new String[] {"-Xss2m", "-Xms1G", "-Xmx2G"};
        String[] javaArgs = buildJavaArgs(BinaryImageGenerator.class, vmOptions, imageArguments, null);
        javaArgs = appendArgs(new String[] {_javaExecutable.getValue()}, javaArgs);
        final File outputFile = getOutputFile(imageDir, "IMAGEGEN", imageConfig);

        final int exitValue = exec(null, javaArgs, outputFile, "Building " + imageDir.getName() + "/maxine.vm", _imageBuildTimeOut.getValue());
        if (exitValue == 0) {
            // if the image was built correctly, copy the maxvm executable and shared libraries to the same directory
            copyBinary(imageDir, "maxvm");
            copyBinary(imageDir, mapLibraryName("jvm"));
            copyBinary(imageDir, mapLibraryName("javatest"));
            copyBinary(imageDir, mapLibraryName("prototype"));
            copyBinary(imageDir, mapLibraryName("inspector"));
            _generatedImages.put(imageConfig, imageDir);
            return true;
        } else if (exitValue == PROCESS_TIMEOUT) {
            out().println("(image build timed out): " + new File(imageDir, BinaryImageGenerator.getDefaultBootImageFilePath().getName()));
        }
        _generatedImages.put(imageConfig, null);
        return false;
    }

    private static String mapLibraryName(String name) {
        final String libName = System.mapLibraryName(name);
        if (OperatingSystem.current() == OperatingSystem.DARWIN && libName.endsWith(".jnilib")) {
            return Strings.chopSuffix(libName, ".jnilib") + ".dylib";
        }
        return libName;
    }

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

    private static File getOutputFile(File outputDir, String outputFileName, String imageConfig) {
        return getOutputFile(outputDir, outputFileName, imageConfig, ".output");
    }

    private static String[] appendArgs(String[] args, String... extraArgs) {
        String[] result = args;
        if (extraArgs.length > 0) {
            result = new String[args.length + extraArgs.length];
            System.arraycopy(args, 0, result, 0, args.length);
            System.arraycopy(extraArgs, 0, result, args.length, extraArgs.length);
        }
        return result;
    }

    private static String[] buildJavaArgs(Class javaMainClass, String[] vmArguments, String[] javaArguments, String[] systemProperties) {
        final LinkedList<String> cmd = new LinkedList<String>();
        cmd.add("-d64");
        cmd.add("-classpath");
        cmd.add(System.getProperty("java.class.path"));
        if (vmArguments != null) {
            for (String arg : vmArguments) {
                cmd.add(arg);
            }
        }
        if (systemProperties != null) {
            for (int i = 0; i < systemProperties.length; i++) {
                cmd.add("-D" + systemProperties[i]);
            }
        }
        cmd.add(javaMainClass.getName());
        if (javaArguments != null) {
            for (String arg : javaArguments) {
                cmd.add(arg);
            }
        }
        return cmd.toArray(new String[0]);
    }

    private static int exec(File workingDir, String[] command, File outputFile, String name, int timeout) {
        traceExec(workingDir, command);
        try {
            final FileOutputStream outFile = new FileOutputStream(outputFile);
            final Process process = Runtime.getRuntime().exec(command, null, workingDir);
            final ProcessThread processThread = new ProcessThread(System.in, outFile, outFile, process, name, timeout);
            final int exitValue = processThread.exitValue();
            outFile.close();
            return exitValue;
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
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
                for (String c : command) {
                    stream.println("    " + c);
                }
            }
        }
    }

    /**
     * A dedicated thread to wait for the process and terminate it if it gets stuck.
     *
     * @author Ben L. Titzer
     */
    private static class ProcessThread extends Thread {

        private final Process _process;
        private final int _timeoutMillis;
        protected int _exitValue;
        private Redirector _stderr;
        private Redirector _stdout;
        private Redirector _stdin;

        public ProcessThread(InputStream in, OutputStream out, OutputStream err, Process process, String name, int timeoutSeconds) {
            super(name);
            _process = process;
            _timeoutMillis = 1000 * timeoutSeconds;
            _stderr = Streams.redirect(_process, _process.getErrorStream(), err, "[stderr]");
            _stdout = Streams.redirect(_process, _process.getInputStream(), out, "[stdout]");
            _stdin = Streams.redirect(_process, System.in, _process.getOutputStream(), "[stdin]");
        }
        @Override

        public void run() {
            final long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < _timeoutMillis) {
                try {
                    Thread.sleep(50);
                    _exitValue = _process.exitValue();
                    Thread.sleep(100);
                    _stdout.close();
                    _stderr.close();
                    _stdin.close();
                    synchronized (this) {
                        // Timed out:
                        notifyAll();
                    }
                    return;
                } catch (IllegalThreadStateException e) {
                    // do nothing.
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            _exitValue = PROCESS_TIMEOUT;
            _stdout.close();
            _stderr.close();
            _stdin.close();
            synchronized (this) {
                // Timed out:
                _process.destroy();
                notifyAll();
            }
            return;
        }

        public int exitValue() {
            start();
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
            return _exitValue;
        }
    }
}

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

    private static final OptionSet _options = new OptionSet();
    private static final Option<String> _outputDir = _options.newStringOption("output-dir", "maxine-tester",
                    "The output directory for the results of the maxine tester.");
    private static final Option<Integer> _imageBuildTimeOut = _options.newIntegerOption("image-build-timeout", 600,
                    "The number of seconds to wait for an image build to complete before " +
                    "timing out and killing it.");
    private static final Option<String> _javaExecutable = _options.newStringOption("java-executable", "java",
                    "The name of or full path to the Java VM executable to use. This must be a JDK 6 or greater VM.");
    private static final Option<String> _javaVMArgs = _options.newStringOption("java-vm-args", "-d64 -Xmx1g",
                    "The VM options to be used when running the Java VM.");
    private static final Option<Integer> _javaTesterTimeOut = _options.newIntegerOption("java-tester-timeout", 50,
                    "The number of seconds to wait for the in-target Java tester tests to complete before " +
                    "timing out and killing it.");
    private static final Option<Integer> _javaTesterConcurrency = _options.newIntegerOption("java-tester-concurrency", 1,
                    "The number of Java tester tests to run in parallel.");
    private static final Option<Integer> _javaRunTimeOut = _options.newIntegerOption("java-run-timeout", 50,
                    "The number of seconds to wait for the target VM to complete before " +
                    "timing out and killing it when running user programs.");
    private static final Option<Integer> _traceOption = _options.newIntegerOption("trace", 0,
                    "The tracing level for building the images and running the tests.");
    private static final Option<Boolean> _skipImageGen = _options.newBooleanOption("skip-image-gen", false,
                    "Skip the generation of the image, which is useful for testing the Maxine tester itself.");
    private static final Option<List<String>> _javaTesterConfigs = _options.newStringListOption("java-tester-configs",
                    MaxineTesterConfiguration.defaultJavaTesterConfigs(),
                    "A list of configurations for which to run the Java tester tests.");
    private static final Option<List<String>> _tests = _options.newStringListOption("tests", "junit,output,javatester",
                    "The list of test harnesses to run, which may include JUnit tests (junit), output tests (output), " +
                    "the JavaTester (javatester), DaCapo (dacapo), and SpecJVM98 (specjvm98).\n\nA subset of the JUnit/Output/Dacapo/SpecJVM98/Shootout tests " +
                    "can be specified by appending a ':' followed by a '+' separated list of test name substrings. For example:\n\n" +
                    "-tests=specjvm98:jess+db,dacapo:pmd+fop\n\nwill " +
                    "run the _202_jess and _209_db SpecJVM98 benchmarks as well as the pmd and fop Dacapo benchmarks.\n\n" +
                    "Ouput tests: " + Arrays.toString(MaxineTesterConfiguration._outputTests) + "\n\n" +
                    "Dacapo tests: " + Arrays.toString(MaxineTesterConfiguration._dacapoTests) + "\n\n" +
                    "SpecJVM98 tests: " + Arrays.toString(MaxineTesterConfiguration._specjvm98Tests) + "\n\n" +
                    "Shootout tests: " + Arrays.toString(MaxineTesterConfiguration.shootoutTests()));
    private static final Option<List<String>> _maxvmConfigList = _options.newStringListOption("maxvm-configs",
                    MaxineTesterConfiguration.defaultMaxvmOutputConfigs(),
                    "A list of configurations for which to run the Maxine output tests.");
    private static final Option<String> _javaConfigAliasOption = _options.newStringOption("java-config-alias", null,
                    "The Java tester config to use for running Java programs. Omit this option to use a separate config for Java programs.");
    private static final Option<Integer> _junitTestTimeOut = _options.newIntegerOption("junit-test-timeout", 300,
                    "The number of seconds to wait for a JUnit test to complete before " +
                    "timing out and killing it.");
    private static final Option<Boolean> _slowAutoTests = _options.newBooleanOption("slow-junit-tests", false,
                    "Include junit-tests known to be slow.");
    private static final Option<Boolean> _failFast = _options.newBooleanOption("fail-fast", true,
                    "Stop execution as soon as a single test fails.");
    private static final Option<File> _specjvm98Zip = _options.newFileOption("specjvm98", (File) null,
                    "Location of zipped up SpecJVM98 directory. If not provided, then the SPECJVM98_ZIP environment variable is used.");
    private static final Option<File> _dacapoJar = _options.newFileOption("dacapo", (File) null,
                    "Location of DaCapo JAR file. If not provided, then the DACAPO_JAR environment variable is used.");
    private static final Option<File> _shootoutDir = _options.newFileOption("shootout", (File) null,
                    "Location of the Programming Language Shootout tests. If not provided, then the SHOOTOUT_DIR environment variable is used.");
    private static final Option<Boolean> _timing = _options.newBooleanOption("timing", false,
                    "For the SpecJVM98 and DaCapo benchmarks, report internal timings compared to the baseline.");
    private static final Option<Boolean> _help = _options.newBooleanOption("help", false,
                    "Show help message and exit.");

    private static String _javaConfigAlias = null;

    public static void main(String[] args) {
        try {
            _options.parseArguments(args);

            if (_help.getValue()) {
                _options.printHelp(System.out, 80);
                return;
            }

            _javaConfigAlias = _javaConfigAliasOption.getValue();
            if (_javaConfigAlias != null) {
                ProgramError.check(MaxineTesterConfiguration._imageConfigs.containsKey(_javaConfigAlias), "Unknown Java tester config '" + _javaConfigAlias + "'");
            }
            final File outputDir = new File(_outputDir.getValue()).getAbsoluteFile();
            makeDirectory(outputDir);
            Trace.on(_traceOption.getValue());
            for (String test : _tests.getValue()) {
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
                    new OutputHarness(MaxineTesterConfiguration._outputTestClasses).run();
                } else if (test.startsWith("output:")) {
                    // run the Output tests
                    new OutputHarness(filterTestClassesBySubstrings(MaxineTesterConfiguration._outputTestClasses, test.substring("output:".length()).split("\\+"))).run();
                } else if ("javatester".equals(test)) {
                    // run the JavaTester tests
                    new JavaTesterHarness().run();
                } else if ("dacapo".equals(test)) {
                    // run the DaCapo tests
                    new DaCapoHarness(MaxineTesterConfiguration._dacapoTests).run();
                } else if (test.startsWith("dacapo:")) {
                    // run the DaCapo tests
                    new DaCapoHarness(filterTestsBySubstrings(MaxineTesterConfiguration._dacapoTests, test.substring("dacapo:".length()).split("\\+"))).run();
                } else if ("specjvm98".equals(test)) {
                    // run the SpecJVM98 tests
                    new SpecJVM98Harness(MaxineTesterConfiguration._specjvm98Tests).run();
                } else if (test.startsWith("specjvm98:")) {
                    // run the SpecJVM98 tests
                    new SpecJVM98Harness(filterTestsBySubstrings(MaxineTesterConfiguration._specjvm98Tests, test.substring("specjvm98:".length()).split("\\+"))).run();
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

    /**
     * Runs a given runnable with all {@linkplain #out() standard} and {@linkplain #err() error} output redirect to
     * private buffers. The private buffers are then flushed to the global streams once the runnable completes.
     */
    private static void runWithSerializedOutput(Runnable runnable) {
        final PrintStream oldOut = out();
        final PrintStream oldErr = err();
        final ByteArrayPrintStream out = new ByteArrayPrintStream();
        final ByteArrayPrintStream err = new ByteArrayPrintStream();
        try {
            _out.set(out);
            _err.set(err);
            runnable.run();
        } finally {
            synchronized (oldOut) {
                out.writeTo(oldOut);
            }
            synchronized (oldErr) {
                err.writeTo(oldErr);
            }
            _out.set(oldOut);
            _err.set(oldErr);
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
     * @return {@code true} if the result (pass or fail) of the test matches the expected result, {@code false} otherwise
     */
    private static boolean addTestResult(String testName, String failure, ExpectedResult expectedResult) {
        final boolean passed = failure == null;
        if (!expectedResult.matchesActualResult(passed)) {
            if (expectedResult == ExpectedResult.FAIL) {
                _unexpectedPasses.put(testName, failure);
            } else {
                assert expectedResult == ExpectedResult.PASS;
                _unexpectedFailures.put(testName, failure);
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
            out.println();
            out.println("== Summary ==");
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
        for (String junitTest : _junitTestsWithExceptions) {
            if (out != null) {
                out.println("Non-zero exit status for '" + junitTest + "'");
            }
            failedAutoTests++;
        }

        if (out != null) {
            if (!_unexpectedFailures.isEmpty()) {
                out.println("Unexpected failures:");
                for (Map.Entry<String, String> entry : _unexpectedFailures.entrySet()) {
                    out.println("  " + entry.getKey() + " " + entry.getValue());
                }
            }
            if (!_unexpectedPasses.isEmpty()) {
                out.println("Unexpected passes:");
                for (String unexpectedPass : _unexpectedPasses.keySet()) {
                    out.println("  " + unexpectedPass);
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
     * A list of the {@linkplain JUnitHarness JUnit tests} that caused the Java process to exit with an exception.
     */
    private static AppendableSequence<String> _junitTestsWithExceptions = new ArrayListSequence<String>();

    /**
     * Determines if {@linkplain #_failFast fail fast} has been requested and at least one unexpected failure has
     * occurred.
     */
    static boolean stopTesting() {
        return _failFast.getValue() && reportTestResults(null) != 0;
    }

    private static String fileRef(File file) {
        final String basePath = new File(_outputDir.getValue()).getAbsolutePath() + File.separator;
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
        final String _summary;
        final String _nextTestOption;

        JavaTesterResult(String summary, String nextTestOption) {
            _nextTestOption = nextTestOption;
            _summary = summary;
        }

    }

    /**
     * @param workingDir if {@code null}, then {@code imageDir} is used
     */
    private static int runMaxineVM(JavaCommand command, File imageDir, File workingDir, File outputFile, int timeout) {
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
        return exec(workingDir == null ? imageDir : workingDir, command.getExecutableCommand(imageDir.getAbsolutePath() + "/maxvm"), envp, outputFile, null, timeout);
    }

    /**
     * @param workingDir if {@code null}, then {@code imageDir} is used
     */
    private static int runJavaVM(String program, JavaCommand command, File imageDir, File workingDir, File outputFile, int timeout) {
        final String name = "Executing " + program;
        return exec(workingDir == null ? imageDir : workingDir, command.getExecutableCommand(_javaExecutable.getValue()), null, outputFile, name, timeout);
    }

    /**
     * Map from configuration names to the directory in which the image for the configuration was created.
     * If the image generation failed for a configuration, then it will have a {@code null} entry in this map.
     */
    private static final Map<String, File> _generatedImages = new HashMap<String, File>();

    private static File generateJavaRunSchemeImage() {
        final String config = _javaConfigAlias == null ? "java" : _javaConfigAlias;
        final File imageDir = new File(_outputDir.getValue(), config);
        if (_skipImageGen.getValue()) {
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
        return null;
    }

    private static boolean generateImage(File imageDir, String imageConfig) {
        if (_generatedImages.containsKey(imageConfig)) {
            return _generatedImages.get(imageConfig) != null;
        }
        final JavaCommand javaCommand = new JavaCommand(BinaryImageGenerator.class);
        javaCommand.addArguments(MaxineTesterConfiguration.getImageConfigArgs(imageConfig));
        javaCommand.addArgument("-output-dir=" + imageDir);
        javaCommand.addArgument("-trace=1");
        javaCommand.addVMOption("-XX:CompileCommand=exclude,com/sun/max/vm/jit/JitReferenceMapEditor,fillInMaps");
        javaCommand.addVMOptions(defaultJVMOptions());
        javaCommand.addClasspath(System.getProperty("java.class.path"));
        final String[] javaArgs = javaCommand.getExecutableCommand(_javaExecutable.getValue());
        Trace.line(2, "Generating image for " + imageConfig + " configuration...");
        final File outputFile = stdoutFile(imageDir, "IMAGEGEN", imageConfig);

        final int exitValue = exec(null, javaArgs, null, outputFile, "Building " + imageDir.getName() + "/maxine.vm", _imageBuildTimeOut.getValue());
        if (exitValue == 0) {
            // if the image was built correctly, copy the maxvm executable and shared libraries to the same directory
            copyBinary(imageDir, "maxvm");
            copyBinary(imageDir, mapLibraryName("jvm"));
            copyBinary(imageDir, mapLibraryName("javatest"));
            copyBinary(imageDir, mapLibraryName("prototype"));
            copyBinary(imageDir, mapLibraryName("tele"));

            if (OperatingSystem.current() == OperatingSystem.DARWIN) {
                // Darwin has funky behavior relating to the namespace for native libraries, use a workaround
                exec(null, new String[] {"bin/mod-macosx-javalib.sh", imageDir.getAbsolutePath(), System.getProperty("java.home")}, null, new File("/dev/stdout"), null, 5);
            }

            _generatedImages.put(imageConfig, imageDir);
            return true;
        } else if (exitValue == PROCESS_TIMEOUT) {
            out().println("(image build timed out): " + new File(imageDir, BinaryImageGenerator.getDefaultBootImageFilePath().getName()));
        }
        _generatedImages.put(imageConfig, null);
        return false;
    }


    private static void testJavaProgram(String testName, JavaCommand command, File outputDir, File workingDir, File imageDir, String[] filteredLines) {
        if (stopTesting()) {
            return;
        }
        out().print(left50("Running " + testName + ": "));

        // first run the program on the reference JVM
        final File javaOutput = jvmStdoutFile(outputDir, testName);
        final int javaExitValue = runJavaVM(testName, command.copy(), imageDir, workingDir, javaOutput, _javaRunTimeOut.getValue());

        // now run the test on each of the MaxVM configurations
        for (String config : _maxvmConfigList.getValue()) {
            if (stopTesting()) {
                out().println();
                return;
            }
            final JavaCommand maxvmCommand = command.copy();
            maxvmCommand.addVMOptions(MaxineTesterConfiguration.getVMOptions(config));
            final File maxvmOutput = maxvmStdoutFile(outputDir, testName, config);
            final int maxvmExitValue = runMaxineVM(maxvmCommand, imageDir, workingDir, maxvmOutput, _javaRunTimeOut.getValue());
            if (javaExitValue != maxvmExitValue) {
                if (maxvmExitValue == PROCESS_TIMEOUT) {
                    final ExpectedResult expected = printFailed(testName, config);
                    addTestResult(testName, String.format("timed out", maxvmExitValue, javaExitValue), expected);
                } else {
                    final ExpectedResult expected = printFailed(testName, config);
                    addTestResult(testName, String.format("bad exit value [received %d, expected %d; see %s and %s ]", maxvmExitValue, javaExitValue, fileRef(javaOutput), fileRef(maxvmOutput)), expected);
                }
            } else if (Files.compareFiles(javaOutput, maxvmOutput, filteredLines)) {
                final ExpectedResult expected = printSuccess(testName, config);
                addTestResult(testName, null, expected);
            } else {
                final ExpectedResult expected = printFailed(testName, config);
                addTestResult(testName, String.format("output did not match [compare %s with %s ]", fileRef(javaOutput), fileRef(maxvmOutput)), expected);
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


    private static String[] defaultJVMOptions() {
        final String value = _javaVMArgs.getValue();
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
    private static int exec(File workingDir, String[] command, String[] env, File outputFile, String name, int timeout) {
        traceExec(workingDir, command);
        try {
            final StringBuilder sb = new StringBuilder("exec ");
            for (String s : command) {
                sb.append(escapeShellCharacters(s)).append(' ');
            }
            if (outputFile != null) {
                sb.append(">" + outputFile.getAbsolutePath());
                sb.append(" 2>" + stderrFile(outputFile));
            } else {
                sb.append(">/dev/null");
                sb.append(" 2>&1");
            }

            final Process process = Runtime.getRuntime().exec(new String[] {"sh", "-c", sb.toString()}, env, workingDir);
            final ProcessTimeoutThread processThread = new ProcessTimeoutThread(outputFile, process, name != null ? name : command[0], timeout);
            final int exitValue = processThread.exitValue();
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

        private final Process _process;
        private final int _timeoutMillis;
        protected Integer _exitValue;
        private boolean _timedOut;

        public ProcessTimeoutThread(File outputFile, Process process, String name, int timeoutSeconds) {
            super(name);
            _process = process;
            _timeoutMillis = 1000 * timeoutSeconds;
        }

        @Override
        public void run() {
            try {
                // Sleep for the prescribed timeout duration
                Thread.sleep(_timeoutMillis);

                // Not interrupted: terminate associated process
                _timedOut = true;
                _process.destroy();
            } catch (InterruptedException e) {
                // Process completed within timeout
            }
        }

        public int exitValue() throws IOException {
            start();
            try {
                _exitValue = _process.waitFor();
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

            if (_timedOut) {
                _exitValue = PROCESS_TIMEOUT;
            }
            return _exitValue;
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
        final String[] _testList;
        public JUnitHarness(String[] testList) {
            _testList = testList;
        }
        @Override
        public boolean run() {
            final File outputDir = new File(_outputDir.getValue(), "junit-tests");
            final Set<String> junitTests = new TreeSet<String>();
            new ClassSearch() {
                @Override
                protected boolean visitClass(String className) {
                    if (className.endsWith(".AutoTest")) {
                        if (_testList == null) {
                            junitTests.add(className);
                        } else {
                            for (String test : _testList) {
                                if (className.contains(test)) {
                                    junitTests.add(className);
                                }
                            }
                        }
                    }
                    return true;
                }
            }.run(Classpath.fromSystem());

            final int availableProcessors = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
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
                junitTesterService.awaitTermination(_javaTesterTimeOut.getValue() * 2 * junitTests.size(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
            if (_slowAutoTests.getValue()) {
                javaCommand.addSystemProperty(JUnitTestRunner.INCLUDE_SLOW_TESTS_PROPERTY, null);
            }

            final String[] command = javaCommand.getExecutableCommand(_javaExecutable.getValue());

            final PrintStream out = out();

            out.println("JUnit auto-test: Started " + junitTest);
            out.flush();
            final long start = System.currentTimeMillis();
            final int exitValue = exec(outputDir, command, null, outputFile, junitTest, _junitTestTimeOut.getValue());
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
                _junitTestsWithExceptions.append(junitTest);
            }
            final long runTime = System.currentTimeMillis() - start;
            out.println(" [Time: " + NumberFormat.getInstance().format((double) runTime / 1000) + " seconds]");
            for (String unexpectedResult : unexpectedResults) {
                out.println("    " + unexpectedResult);
            }
            if (!unexpectedResults.isEmpty()) {
                out.println("    see: " + fileRef(outputFile));
            }
        }

        /**
         * Parses a file of test names (one per line) run as part of an auto-test. The global records of test results are
         * {@linkplain MaxineTester#addTestResult(String, String, boolean) updated} appropriately.
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

        @Override
        public boolean run() {
            final List<String> javaTesterConfigs = _javaTesterConfigs.getValue();

            final ExecutorService javaTesterService = Executors.newFixedThreadPool(_javaTesterConcurrency.getValue());
            final CompletionService<Void> javaTesterCompletionService = new ExecutorCompletionService<Void>(javaTesterService);
            for (final String config : javaTesterConfigs) {
                javaTesterCompletionService.submit(new Runnable() {
                    public void run() {
                        if (!stopTesting()) {
                            runWithSerializedOutput(new Runnable() {
                                public void run() {
                                    JavaTesterHarness.runJavaTesterTests(config);
                                }
                            });
                        }
                    }
                }, null);
            }

            javaTesterService.shutdown();
            try {
                javaTesterService.awaitTermination(_javaTesterTimeOut.getValue() * 2 * javaTesterConfigs.size(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        }

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

                        final Matcher matcher = JavaTesterHarness.TEST_BEGIN_LINE.matcher(line);
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

        private static void runJavaTesterTests(String config) {
            final File imageDir = new File(_outputDir.getValue(), config);

            final PrintStream out = out();
            out.println("Java tester: Started " + config);
            if (_skipImageGen.getValue() || generateImage(imageDir, config)) {
                String nextTestOption = "-XX:TesterStart=0";
                int executions = 0;
                while (nextTestOption != null) {
                    final File outputFile = stdoutFile(imageDir, "JAVA_TESTER" + (executions == 0 ? "" : "-" + executions), config);
                    final JavaCommand command = new JavaCommand((Class) null);
                    command.addArgument(nextTestOption);
                    final int exitValue = runMaxineVM(command, imageDir, null, outputFile, _javaTesterTimeOut.getValue());
                    final JavaTesterResult result = JavaTesterHarness.parseJavaTesterOutputFile(config, outputFile);
                    final String summary = result._summary;
                    nextTestOption = result._nextTestOption;
                    out.print("Java tester: Stopped " + config + " - ");
                    if (exitValue == 0) {
                        out.println(summary);
                    } else if (exitValue == PROCESS_TIMEOUT) {
                        out.println("(timed out): " + summary);
                        out.println("  -> see: " + fileRef(outputFile));
                    } else {
                        out.println("(exit = " + exitValue + "): " + summary);
                        out.println("  -> see: " + fileRef(outputFile));
                    }
                    executions++;
                }
            } else {
                out.println("(image build failed)");
                final File outputFile = stdoutFile(imageDir, "IMAGEGEN", config);
                out.println("  -> see: " + fileRef(outputFile));
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
        final Class[] _testList;
        OutputHarness(Class[] tests) {
            _testList = tests;
        }
        @Override
        public boolean run() {
            final File outputDir = new File(_outputDir.getValue(), "java");
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
            for (Class mainClass : _testList) {
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
            if (_timing.getValue()) {
                final long baseline = getInternalTiming(jvmStdoutFile(outputDir, testName));
                out().print(left50("    --> " + testName + " (" + baseline + " ms)"));
                for (String config : _maxvmConfigList.getValue()) {
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
        final String[] _testList;
        SpecJVM98Harness(String[] tests) {
            _testList = tests;
        }
        @Override
        public boolean run() {
            final File specjvm98Zip = getFileFromOptionOrEnv(_specjvm98Zip, "SPECJVM98_ZIP");
            if (specjvm98Zip == null) {
                out().println("Need to specify the location of SpecJVM98 ZIP file with -" + _specjvm98Zip + " or in the SPECJVM98_ZIP environment variable");
                return false;
            }
            final File outputDir = new File(_outputDir.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!specjvm98Zip.exists()) {
                    out().println("Couldn't find SpecJVM98 ZIP file " + specjvm98Zip);
                    return false;
                }
                final File specjvm98Dir = new File(_outputDir.getValue(), "specjvm98");
                Files.unzip(specjvm98Zip, specjvm98Dir);
                for (String test : _testList) {
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
            testJavaProgram(testName, command, outputDir, workingDir, imageDir, MaxineTesterConfiguration._specjvm98IgnoredLinePatterns);
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
        final String[] _testList;
        DaCapoHarness(String[] tests) {
            _testList = tests;
        }
        @Override
        public boolean run() {
            final File dacapoJar = getFileFromOptionOrEnv(_dacapoJar, "DACAPO_JAR");
            if (dacapoJar == null) {
                out().println("Need to specify the location of Dacapo JAR file with -" + _dacapoJar + " or in the DACAPO_JAR environment variable");
                return false;
            }
            final File outputDir = new File(_outputDir.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!dacapoJar.exists()) {
                    out().println("Couldn't find DaCapo JAR file " + dacapoJar);
                    return false;
                }
                for (String test : _testList) {
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
        final String[] _testList;
        ShootoutHarness(String[] tests) {
            _testList = tests;
        }
        @Override
        public boolean run() {
            final File shootoutDir = getFileFromOptionOrEnv(_shootoutDir, "SHOOTOUT_DIR");
            if (shootoutDir == null) {
                out().println("Need to specify the location of the Programming Language Shootout directory with -" + _shootoutDir + " or in the SHOOTOUT_DIR environment variable");
                return false;
            }
            final File outputDir = new File(_outputDir.getValue(), "java");
            final File imageDir = generateJavaRunSchemeImage();
            if (imageDir != null) {
                if (!shootoutDir.exists()) {
                    out().println("Couldn't find shootout directory " + shootoutDir);
                    return false;
                }
                for (String test : _testList) {
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

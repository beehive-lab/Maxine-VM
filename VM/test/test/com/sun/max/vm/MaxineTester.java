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
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

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
    private static final Option<Boolean> _echoOutput = _options.newBooleanOption("echo-output", false,
                    "Causes the output of image builds and test runs to be directed to the console rather than " +
                    "output file(s)");
    private static final Option<Integer> _traceOption = _options.newIntegerOption("trace", 0,
                    "The tracing level for building the images and running the tests.");
    private static final Option<Boolean> _skipImageGen = _options.newBooleanOption("skip-image-gen", false,
                    "Skip the generation of the image, which is useful for testing the Maxine tester itself.");
    private static final Option<List<String>> _javaTesterConfigs = _options.newStringListOption("java-tester-configs", "optopt,jitopt,optjit,jitjit",
                    "A list of configurations for which to run the Java tester tests.");
    private static final Option<String> _javaConfigAliasOption = _options.newStringOption("java-config-alias", null,
                    "The Java tester config to use for running Java programs. Omit this option to use a separate config for Java programs.");
    private static final Option<File> _expect = _options.newFileOption("expect", (File) null,
                    "A file listing the tests expected to fail. The exit value of the MaxineTester is the sum of the unexpected failures, " +
                    "the unexpected passes and the number of image builds that failed.");

    private static final Map<String, String[]> _imageConfigs = new HashMap<String, String[]>();

    private static final Class[] _mainClasses = {
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.Recursion.class,
        test.output.FloatNanTest.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        util.GCTest1.class,
        util.GCTest2.class,
        util.GCTest3.class,
        util.GCTest4.class,
        util.GCTest5.class,
        util.GCTest6.class
    };

    private static String _javaConfigAlias = null;

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

    static {
        _imageConfigs.put("optopt", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests"});
        _imageConfigs.put("optjit", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-callee-jit"});
        _imageConfigs.put("jitopt", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-caller-jit"});
        _imageConfigs.put("jitjit", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-caller-jit", "-test-callee-jit"});
        _imageConfigs.put("java", new String[] {"-run=com.sun.max.vm.run.java"});
    }

    private static void makeDirectory(File directory) {
        if (directory.exists()) {
            ProgramError.check(directory.isDirectory(), "Path already exists but is not a directory: " + directory);
            return;
        }
        if (!directory.mkdirs()) {
            ProgramError.unexpected("Could not make directory " + directory);
        }
    }

    public static void main(String[] args) {
        try {
            _options.parseArguments(args);
            _javaConfigAlias = _javaConfigAliasOption.getValue();
            if (_javaConfigAlias != null) {
                ProgramError.check(_imageConfigs.containsKey(_javaConfigAlias), "Unknown Java tester config '" + _javaConfigAlias + "'");
            }
            final File outputDir = new File(_outputDir.getValue()).getAbsoluteFile();
            makeDirectory(outputDir);
            Trace.on(_traceOption.getValue());
            runJavaPrograms();
            runJavaTesterTests();
            System.exit(reportTestResults());
        } catch (Throwable throwable) {
            throwable.printStackTrace(err());
            System.exit(-1);
        }
    }

    /**
     * A map from test names to a string describing a test failure or null if a test passed.
     */
    private static final Map<String, String> _testResults = Collections.synchronizedMap(new TreeMap<String, String>());

    private static Set<String> loadExpectedFailures() {
        if (_expect.getValue() == null) {
            return Collections.emptySet();
        }
        final File expectedFailuresFile = _expect.getValue().getAbsoluteFile();
        if (expectedFailuresFile.exists()) {
            final Set<String> expectedFailures = new HashSet<String>();
            try {
                final BufferedReader br = new BufferedReader(new FileReader(expectedFailuresFile));
                String line;
                while ((line = br.readLine()) != null) {
                    final String testLine = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    // Up to the first space is the test name:
                    final int indexOfSpace = testLine.indexOf(' ');
                    final String testName = indexOfSpace == -1 ? testLine : testLine.substring(0, indexOfSpace);
                    expectedFailures.add(testName);
                }
                br.close();
                return expectedFailures;
            } catch (IOException ioException) {
                ProgramWarning.message("Error reading expected failures file " + expectedFailuresFile.getAbsolutePath() + ": " + ioException);
            }
        }
        return Collections.emptySet();
    }

    /**
     * Adds a test result to the global set of test results.
     *
     * @param testName the unique name of the test
     * @param failure a failure message or null if the test passed
     */
    private static void addTestResult(String testName, String failure) {
        _testResults.put(testName, failure);
    }

    private static int reportTestResults() {
        int failedImages = 0;
        for (Map.Entry<String, File> entry : _generatedImages.entrySet()) {
            if (entry.getValue() == null) {
                out().println("Failed building image for configuration '" + entry.getKey() + "'");
                failedImages++;
            }
        }

        final Set<String> expectedFailures = loadExpectedFailures();
        final Map<String, String> unexpectedFailures = new HashMap<String, String>();
        final Set<String> unexpectedPasses = new HashSet<String>();
        final Set<String> failures = new HashSet<String>();

        for (Map.Entry<String, String> entry : _testResults.entrySet()) {
            final String testName = entry.getKey();
            final String failureMessage = entry.getValue();
            final boolean expectedFailure = expectedFailures.contains(testName);
            if (failureMessage == null) {
                if (expectedFailure) {
                    unexpectedPasses.add(testName);
                }
            } else {
                failures.add(testName);
                if (!expectedFailure) {
                    unexpectedFailures.put(testName, failureMessage);
                }
            }
        }

        if (!unexpectedFailures.isEmpty()) {
            out().println("Unexpected failures: [add to file specified by -" + _expect.getName() + " option]");
            for (Map.Entry<String, String> entry : unexpectedFailures.entrySet()) {
                out().println(entry.getKey() + "  " + entry.getValue());
            }
        }
        if (!unexpectedPasses.isEmpty()) {
            out().println("Unexpected passes: [remove from file specified by -" + _expect.getName() + " option]");
            for (String testName : unexpectedPasses) {
                out().println(testName);
            }
        }

        return unexpectedFailures.size() + unexpectedPasses.size() + failedImages;
    }

    private static void runJavaTesterTests() {
        final List<String> javaTesterConfigs = _javaTesterConfigs.getValue();

        final ExecutorService javaTesterService = Executors.newFixedThreadPool(_javaTesterConcurrency.getValue());
        final CompletionService<Void> javaTesterCompletionService = new ExecutorCompletionService<Void>(javaTesterService);
        int submitted = 0;
        for (final String config : javaTesterConfigs) {
            javaTesterCompletionService.submit(new Runnable() {
                public void run() {
                    runJavaTesterTests(config);
                }

            }, null);
            submitted++;
        }

        for (int i = 0; i < submitted; ++i) {
            try {
                javaTesterCompletionService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        javaTesterService.shutdown();
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
                final File outputFile = getOutputFile(imageDir, "JAVA_TESTER_OUTPUT" + (executions == 0 ? "" : "-" + executions), config);
                final int exitValue = runMaxineVM(null, new String[] {nextTestOption}, imageDir, outputFile, _javaTesterTimeOut.getValue());
                final JavaTesterResult result = parseJavaTesterOutputFile(outputFile);
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
            final File outputFile = getOutputFile(imageDir, "IMAGE_GENERATION_OUTPUT", config);
            out.println("  -> see: " + outputFile.getAbsolutePath());
        }

        if (_javaTesterConcurrency.getValue() != 1) {
            synchronized (out()) {
                ((ByteArrayPrintStream) out).writeTo(out());
            }
        }
    }

    private static void runJavaPrograms() {
        final String config = _javaConfigAlias == null ? "java" : _javaConfigAlias;
        final File outputDir = new File(_outputDir.getValue(), "java");
        final File imageDir = new File(_outputDir.getValue(), config);
        out().println("Building Java run scheme: started");
        if (_skipImageGen.getValue() || generateImage(imageDir, config)) {
            out().println("Building Java run scheme: OK");
            for (Class mainClass : _mainClasses) {
                runJavaProgram(outputDir, imageDir, mainClass);
            }
        } else {
            out().println("Building Java run scheme: failed");
            final File outputFile = getOutputFile(imageDir, "IMAGE_GENERATION_OUTPUT", config);
            out().println("  -> see: " + outputFile.getAbsolutePath());
        }
    }

    private static void runJavaProgram(File outputDir, File imageDir, Class mainClass) {
        out().print("Running " + mainClass.getName() + "...");
        final File javaOutput = getOutputFile(outputDir, "JVM_" + mainClass.getSimpleName(), "output");
        final File maxvmOutput = getOutputFile(outputDir, "MAXVM_" + mainClass.getSimpleName(), "output");

        final String[] args = buildJavaArgs(mainClass, new String[0], new String[0]);

        final int javaExitValue = runJavaVM(mainClass, args, imageDir, javaOutput, _javaRunTimeOut.getValue());
        final int maxineExitValue = runMaxineVM(mainClass, args, imageDir, maxvmOutput, _javaRunTimeOut.getValue());
        if (javaExitValue != maxineExitValue) {
            if (maxineExitValue == PROCESS_TIMEOUT) {
                out().println("(timed out)");
            } else {
                out().println("(" + maxineExitValue + " != " + javaExitValue + ")");
            }
            out().println("  -> see: " + maxvmOutput.getAbsolutePath());
            addTestResult(mainClass.getName(), String.format("bad exit value [received %d, expected %d]", maxineExitValue, javaExitValue));
        } else if (compareFiles(javaOutput, maxvmOutput)) {
            out().println("OK");
            addTestResult(mainClass.getName(), null);
        } else {
            out().println("(output did not match)");
            out().println("  -> see: " + javaOutput.getAbsolutePath());
            out().println("  -> see: " + maxvmOutput.getAbsolutePath());
            addTestResult(mainClass.getName(), String.format("output did not match [compare %s with %s]", javaOutput.getPath(), maxvmOutput.getPath()));
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

    private static final Pattern TEST_BEGIN_LINE = Pattern.compile("\\d+: +(\\S+)\\s+next: '-XX:TesterStart=(\\d+)', end: '-XX:TesterEnd=(\\d+)'");

    private static JavaTesterResult parseJavaTesterOutputFile(File outputFile) {
        String nextTestOption = null;
        String lastTest = null;
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
                        lastTest = matcher.group(1);
                        addTestResult(lastTest, null);
                        final String nextTestNumber = matcher.group(2);
                        final String endTestNumber = matcher.group(3);
                        if (!nextTestNumber.equals(endTestNumber)) {
                            nextTestOption = "-XX:TesterStart=" + nextTestNumber;
                        } else {
                            nextTestOption = null;
                        }

                    } else if (line.contains("failed")) {
                        failedLines.append(line); // found a line with "failed"--probably a failed test
                        addTestResult(lastTest, line);
                    } else if (line.startsWith("Done: ")) {
                        lastTest = null;
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
                    failedLines.append(lastTest + " failed: never returned a result");
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
            return exec(imageDir, appendArgs(new String[] {"./maxvm", "-XX:TesterOff"}, args), outputFile, name, timeout);
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
        final String[] generatorArguments = _imageConfigs.get(imageConfig);
        if (generatorArguments == null) {
            ProgramError.unexpected("unknown image configuration: " + imageConfig);
        }
        Trace.line(2, "Generating image for " + imageConfig + " configuration...");
        final String[] imageArguments = appendArgs(new String[] {"-output-dir=" + imageDir, "-trace=1"}, generatorArguments);
        String[] javaArgs = buildJavaArgs(BinaryImageGenerator.class, imageArguments, new String[0]);
        javaArgs = appendArgs(new String[] {_javaExecutable.getValue(), "-Xss2m", "-Xms1G", "-Xmx2G"}, javaArgs);
        final File outputFile = getOutputFile(imageDir, "IMAGE_GENERATION_OUTPUT", imageConfig);

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

    private static File getOutputFile(File outputDir, String outputFileName, String imageConfig) {
        File outputFile = null;
        if (!_echoOutput.getValue()) {
            final File file = new File(outputDir, outputFileName + "." + imageConfig);
            makeDirectory(file.getParentFile());
            outputFile = file;
        }
        return outputFile;
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

    private static String[] buildJavaArgs(Class javaMainClass, String[] javaArguments, String[] systemProperties) {
        final LinkedList<String> cmd = new LinkedList<String>();
        cmd.add("-d64");
        cmd.add("-classpath");
        cmd.add(System.getProperty("java.class.path"));
        for (int i = 0; i < systemProperties.length; i++) {
            cmd.add("-D" + systemProperties[i]);
        }
        cmd.add(javaMainClass.getName());
        for (String arg : javaArguments) {
            cmd.add(arg);
        }
        return cmd.toArray(new String[0]);
    }

    private static int exec(File workingDir, String[] command, File outputFile, String name, int timeout) {
        traceExec(workingDir, command);
        try {
            OutputStream out = out();
            OutputStream err = err();
            FileOutputStream outFile = null;
            if (outputFile != null) {
                outFile = new FileOutputStream(outputFile);
                out = outFile;
                err = outFile;
            }
            final Process process = Runtime.getRuntime().exec(command, null, workingDir);
            final ProcessThread processThread = new ProcessThread(System.in, out, err, process, name, timeout);
            final int exitValue = processThread.exitValue();
            if (outFile != null) {
                outFile.close();
            }
            return exitValue;
        } catch (IOException e) {
            throw ProgramError.unexpected(e);
        }
    }

    private static void traceExec(File workingDir, String[] command) {
        if (Trace.hasLevel(2)) {
            if (workingDir == null) {
                Trace.line(2, "Executing process in current directory");
            } else {
                Trace.line(2, "Executing process in directory: " + workingDir);
            }
            for (String c : command) {
                Trace.line(2, "    " + c);
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
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // do nothing.
                }
                try {
                    _exitValue = _process.exitValue();
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

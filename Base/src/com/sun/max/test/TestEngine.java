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
package com.sun.max.test;

import java.io.*;
import java.util.*;
import java.util.Arrays;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.util.*;

/**
 * The {@code TestEngine} class implements the basic test engine for the testing framework, including loading of test
 * cases and organizing their output.
 *
 * @author Ben L. Titzer
 */
public class TestEngine {

    protected int verbose = 2;

    protected final LinkedList<TestCase> allTests;
    protected final LinkedList<TestCase> failTests;
    protected final LinkedList<TestCase> passTests;
    protected final LinkedList<File> skipFiles;
    protected final Queue<TestCase> queue;
    protected final Registry<TestHarness> registry;
    protected int finished;
    protected ProgressPrinter progress;

    public TestEngine(Registry<TestHarness> registry) {
        allTests = new LinkedList<TestCase>();
        failTests = new LinkedList<TestCase>();
        passTests = new LinkedList<TestCase>();
        skipFiles = new LinkedList<File>();
        queue = new LinkedList<TestCase>();
        this.registry = registry;
    }

    public static void main(String[] args) {
        final TestEngine e = new TestEngine(new Registry<TestHarness>(TestHarness.class, true));
        e.parseAndRunTests(args);
        e.report(System.out);
    }

    public synchronized void addTest(TestCase testCase) {
        testCase.testNumber = allTests.size();
        allTests.add(testCase);
        queue.offer(testCase);
    }

    public synchronized void skipFile(File file) {
        skipFiles.add(file);
    }

    public void setVerboseLevel(int level) {
        verbose = level;
    }

    public void report(PrintStream stream) {
        progress.report();
        if (skipFiles.size() > 0) {
            stream.println(skipFiles.size() + " file(s) skipped");
            for (File f : skipFiles) {
                stream.println(f.getName());
            }
        }
    }

    public void parseAndRunTests(String[] args) {
        parseTests(args, true);
        progress = new ProgressPrinter(System.out, allTests.size(), verbose, false);
        for (TestCase tcase = queue.poll(); tcase != null; tcase = queue.poll()) {
            runTest(tcase);
        }
    }

    public void parseTests(String[] args, boolean sort) {
        for (String arg : args) {
            final File f = new File(arg);
            parseTests(f, registry, sort);
        }
    }

    public Iterable<TestCase> getAllTests() {
        return allTests;
    }

    private synchronized TestCase dequeue() {
        return queue.remove();
    }

    private void runTest(TestCase testCase) {
        try {
            // run the test (records thrown exceptions internally)
            startTest(testCase);
            testCase.test();
            final Class<TestHarness<TestCase>> type = null;
            // evaluate the result of test
            final TestResult result = StaticLoophole.cast(type, testCase.harness).evaluateTest(this, testCase);
            testCase.result = result;
        } catch (Throwable t) {
            // there was an exception evaluating the result of the test
            testCase.result = new TestResult.UnexpectedException("Unexpected exception in test evaluation", t);
        } finally {
            finishTest(testCase);
        }
    }

    private synchronized void startTest(TestCase testCase) {
        progress.begin(testCase.file.toString());
    }

    private synchronized void finishTest(TestCase testCase) {
        final boolean passed = testCase.result.isSuccess();
        if (passed) {
            passTests.add(testCase);
            progress.pass();
        } else {
            failTests.add(testCase);
            progress.fail(testCase.result.failureMessage(testCase));
        }
    }

    private void parseTests(File file, Registry<TestHarness> reg, boolean sort) {
        if (!file.exists()) {
            throw new Error("file " + file + " not found.");
        }
        if (file.isDirectory()) {
            for (File dirFile : getFilesFromDirectory(file, sort)) {
                if (!dirFile.isDirectory()) {
                    parseFile(dirFile, reg);
                }
            }
        } else {
            parseFile(file, reg);
        }
    }

    private File[] getFilesFromDirectory(File dir, boolean sort) {
        final File[] list = dir.listFiles();
        if (sort) {
            Arrays.sort(list);
        }
        return list;
    }

    private void parseFile(File file, Registry<TestHarness> reg) {
        try {
            final Properties props = parseTestProperties(file);
            final String hname = props.getProperty("Harness");

            if (hname != null) {
                // only try to create tests if a harness is specified.
                try {
                    final TestHarness harness = reg.getInstance(hname, false);
                    if (harness == null) {
                        ProgramError.unexpected("invalid harness: " + hname);
                    } else {
                        harness.parseTests(this, file, props);
                    }
                } catch (Throwable t) {
                    ProgramError.unexpected("unexpected exception while parsing " + file, t);
                }
            } else {
                skipFile(file);
            }
        } catch (FileNotFoundException e) {
            ProgramError.unexpected("file " + file + " not found.");
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }
    }

    private Properties parseTestProperties(File file) throws FileNotFoundException, IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final Properties vars = new Properties();
        boolean lineFound = false;

        while (true) {
            // read any of the beginning lines that contain '@'
            final String line = reader.readLine();

            if (line == null) {
                break;
            }

            final int indx1 = line.indexOf('@');
            final int indx2 = line.indexOf(':');

            if (indx1 < 0 || indx2 < 0) {
                // this line does not match: break out if already matched
                if (lineFound) {
                    break;
                }
                continue;
            }
            lineFound = true;

            final String var = line.substring(indx1 + 1, indx2).trim();
            final String value = line.substring(indx2 + 1).trim();
            if (vars.get(var) != null) {
                // if there is already a value, append.
                vars.put(var, vars.get(var) + " " + value);
            } else {
                vars.put(var, value);
            }
        }
        reader.close();
        return vars;
    }

    private boolean loadingPackages;

    public boolean loadingPackages() {
        return loadingPackages;
    }

    public void setLoadingPackages(boolean loadingPackages) {
        this.loadingPackages = loadingPackages;
    }
}

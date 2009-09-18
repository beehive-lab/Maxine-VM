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
package test.com.sun.max.vm.jtrun;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.program.option.*;
import com.sun.max.test.*;
import com.sun.max.test.JavaExecHarness.*;
import com.sun.max.util.*;

public class JTGenerator {

    private static final OptionSet options = new OptionSet(true);

    private static final Option<Boolean> exceptionsOption = options.newBooleanOption("exceptions", true,
                    "Selects whether runs that are expected to throw exceptions will be tested.");
    private static final Option<Boolean> runStringsOption = options.newBooleanOption("run-strings", true,
                    "Selects whether the input values will be reported for a test case that fails.");
    private static final Option<Boolean> loadedOption = options.newBooleanOption("force-loaded", true,
                    "Specifies that all test classes will be loaded into the target.");
    private static final Option<Boolean> resolvedOption = options.newBooleanOption("force-resolved", true,
                    "Specifies that all test classes and method will be resolved in the target.");
    private static final Option<Boolean> restartOption = options.newBooleanOption("restart", true,
                    "Specifies that generated run scheme will allow starting the tests from a particular test number.");
    private static final Option<Integer> verboseOption = options.newIntegerOption("verbose", 3,
                    "Specifies the verbose level of the generated tests.");
    private static final Option<Boolean> compileOption = options.newBooleanOption("compile", true,
                    "Compiles the generate Java source code automatically using a Java source compiler.");
    private static final Option<Boolean> forceCompileOption = options.newBooleanOption("force-compile", false,
                    "Forces compilation of the generated Java source code, even if it was not updated.");
    private static final Option<String> packageOption = options.newStringOption("package", "some",
                    "");
    private static final Option<Boolean> sortOption = options.newBooleanOption("alphabetical", true,
                    "Generates the test cases in alphabetical order.");

    public static class Executor implements JavaExecHarness.Executor {
        public void initialize(JavaExecHarness.JavaTestCase tc, boolean loadingPackages) {
            // do nothing.
        }
        public Object execute(JavaTestCase c, Object[] vals) throws InvocationTargetException {
            return null;
        }
    }

    final IndentWriter writer;

    /** Generate, if necessary, the runSchemeFile and the testRunsFile.
     * @param extraOptions
     * @param args
     * @return true if either the runSchemeFile or the testRunsFile was actually updated.
     */
    public static boolean generate(OptionSet extraOptions, String[] args) {
        options.loadOptions(extraOptions);
        return generate(args);
    }

    public static void main(String[] args) {
        generate(options.parseArguments(args).getArguments());
    }

    private static boolean generate(final String[] arguments) throws ProgramError {
        final Registry<TestHarness> registry = new Registry<TestHarness>(TestHarness.class, false);
        final JavaExecHarness javaExecHarness = new JavaExecHarness(new Executor());
        registry.registerObject("java", javaExecHarness);
        final TestEngine engine = new TestEngine(registry);
        engine.parseTests(arguments, sortOption.getValue());
        boolean filesUpdated = false;
        try {
            final String runSchemeFile = fileName("JTConfig");
            final String testRunsFile = fileName("JTRuns");

            final LinkedList<JavaTestCase> cases = extractJavaTests(engine);

            if (generateConfigContent(new File(runSchemeFile), cases)) {
                filesUpdated = true;
                System.out.println(runSchemeFile + " updated.");
            }
            if (generateTestRunsContent(new File(testRunsFile), cases)) {
                filesUpdated = true;
                System.out.println(testRunsFile + " updated.");
            }

            if (forceCompileOption.getValue() || (filesUpdated && compileOption.getValue()))  {
                ToolChain.compile(new String[] {className("JavaTesterRunScheme"), className("JavaTesterTests")});
                System.out.println(runSchemeFile + " recompiled.");
                System.out.println(testRunsFile + " recompiled.");
            }
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }
        return filesUpdated;
    }

    private static String className(String className) {
        return (new Package().name() + "." + packageOption.getValue() + ".") + className;
    }

    private static String fileName(String className) {
        return className(className).replace('.', File.separatorChar) + ".java";
    }

    private static boolean generateConfigContent(final File configFile, final LinkedList<JavaTestCase> cases) throws IOException {
        final Writer writer = new StringWriter();
        final JTGenerator gen = new JTGenerator(writer);
        gen.genClassList(cases);
        writer.close();
        return Files.updateGeneratedContent(configFile, ReadableSource.Static.fromString(writer.toString()), "// GENERATED TEST CLASS LIST", "// END GENERATED TEST CLASS LIST");
    }

    private static boolean generateTestRunsContent(final File testRunsFile, final LinkedList<JavaTestCase> cases) throws IOException {
        final Writer writer = new StringWriter();
        final JTGenerator gen = new JTGenerator(writer);
        gen.genRunMethod(cases);
        gen.genTestRuns(cases);
        writer.close();
        return Files.updateGeneratedContent(testRunsFile, ReadableSource.Static.fromString(writer.toString()), "// GENERATED TEST RUNS", "// END GENERATED TEST RUNS");
    }

    private static LinkedList<JavaExecHarness.JavaTestCase> extractJavaTests(final TestEngine engine) {
        final Iterable<TestCase> tests = engine.getAllTests();
        final LinkedList<JavaExecHarness.JavaTestCase> list = new LinkedList<JavaExecHarness.JavaTestCase>();
        for (TestCase tc : tests) {
            if (tc instanceof JavaExecHarness.JavaTestCase) {
                list.add((JavaExecHarness.JavaTestCase) tc);
            }
        }
        return list;
    }

    public JTGenerator(Writer w) {
        writer = new IndentWriter(w);
    }

    public void genClassList(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        writer.indent();
        writer.println("private static final Class<?>[] classList = {");
        writer.indent();
        final Iterator<JavaExecHarness.JavaTestCase> iterator = testCases.iterator();
        while (iterator.hasNext()) {
            writer.print(getClassLiteral(iterator.next()));
            if (iterator.hasNext()) {
                writer.println(",");
            } else {
                writer.println("");
            }
        }
        writer.outdent();
        writer.println("};");
        writer.outdent();
    }

    public void genRunMethod(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        writer.indent();
        writer.println("public static void runTest(int num) {");
        writer.indent();
            writer.println("switch(num) {");
            writer.indent();
        int i = 0;
        for (JavaExecHarness.JavaTestCase testCase : testCases) {
            writer.print("case " + (i++) + ": ");
            writer.print("JavaTesterTests." + getTestCaseName(testCase) + "();");
            writer.println(" break;");
        }
        writer.outdent();
        writer.println("}");
        writer.outdent();
        writer.println("}");
    }

    private void genTestRuns(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        writer.indent();
        for (JavaExecHarness.JavaTestCase testCase : testCases) {
            genTestCase(testCase, exceptionsOption.getValue());
        }
        writer.outdent();
    }

    public void genTestCase(JavaExecHarness.JavaTestCase testCase, boolean testExceptions) {
        writer.print("static void ");
        writer.println(getTestCaseName(testCase) + "() {");
        writer.indent();
        if (verboseOption.getValue() > 2) {
            writer.println("JavaTesterRunScheme.begin(\"" + testCase.clazz.getName() + "\");");
        }
        writer.println("String runString = null;");
        writer.println("try {");
        for (JavaExecHarness.Run run : testCase.runs) {
            genRun(testCase, run, testExceptions);
        }
        writer.println("} catch (Throwable t) {");
        writer.println("    JavaTesterRunScheme.end(runString, t);");
        writer.println("    return;");
        writer.println("}");
        writer.println("JavaTesterRunScheme.end(null, true);");
        writer.outdent();
        writer.println("}");
    }

    private String getTestCaseName(JavaExecHarness.JavaTestCase testCase) {
        return testCase.clazz.getName().replaceAll("\\.", "_");
    }

    public void genRun(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run, boolean testExceptions) {
        if (!testExceptions) {
            return;
        }
        genRunComment(testCase, run);
        genTestRun(testCase, run);
    }

    private void genRunComment(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        writer.print("// ");
        writer.print(JavaExecHarness.inputToString(testCase.clazz, run, false));
        writer.print(" == ");
        writer.print(JavaExecHarness.resultToString(run.expectedValue, run.expectedException));
        writer.println();
    }

    private void genTestRun(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        writer.indent();
        String runString = "null";
        if (runStringsOption.getValue()) {
            runString = JavaExecHarness.inputToString(testCase.clazz, run, true);
        }
        if (run.expectedException != null) {
            // an exception is expected, a value is NOT expected
            writer.println("try {");
            writer.indent();
            writer.println("runString = " + runString + ";");
            genTestCall(testCase, run);
            writer.println(";");
            writer.println("JavaTesterRunScheme.end(runString, false);");
            writer.println("return;");
            writer.outdent();
            writer.println("} catch (Throwable e) {");
            writer.indent();
            writer.println("if (e.getClass() != " + getExceptionName(run) + ") {");
            writer.println("    JavaTesterRunScheme.end(runString, e);");
            writer.println("    return;");
            writer.println("}");
            writer.outdent();
            writer.println("}");
        } else {
            // check the return value against the expected return value
            if (useEquals(run.expectedValue)) {
                writer.println("runString = " + runString + ";");
                writer.print("if (");
                genValue(run.expectedValue);
                writer.print(" != ");
                genTestCall(testCase, run);
                writer.println(") {");
            } else {
                writer.println("runString = " + runString + ";");
                writer.print("if (!");
                genValue(run.expectedValue);
                writer.print(".equals(");
                genTestCall(testCase, run);
                writer.println(")) {");
            }
            writer.indent();
            writer.println("JavaTesterRunScheme.end(runString, false);");
            writer.println("return;");
            writer.outdent();
            writer.println("}");
        }
        writer.outdent();
    }

    private void genTestCall(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        writer.print(testCase.clazz.getName() + ".test(");
        for (int i = 0; i < run.input.length; i++) {
            if (i > 0) {
                writer.print(", ");
            }
            genValue(run.input[i]);
        }
        writer.print(")");
    }

    private void genValue(Object v) {
        if (v instanceof Character) {
            final Character chv = (Character) v;
            writer.print("(char) " + (int) chv.charValue());
        } else if (v instanceof Float) {
            writer.print(String.valueOf(v) + "f");
        } else if (v instanceof Long) {
            writer.print(String.valueOf(v) + "L");
        } else if (v instanceof Short) {
            writer.print("(short) " + String.valueOf(v));
        } else if (v instanceof Byte) {
            writer.print("(byte) " + String.valueOf(v));
        } else if (v instanceof String) {
            writer.print("\"" + String.valueOf(v) + "\"");
        } else {
            writer.print(String.valueOf(v));
        }
    }

    private boolean useEquals(Object expectedValue) {
        if (expectedValue == null) {
            return true;
        } else if (expectedValue instanceof Integer) {
            return true;
        } else if (expectedValue instanceof Boolean) {
            return true;
        } else if (expectedValue instanceof Character) {
            return true;
        } else if (expectedValue instanceof Float) {
            return true;
        } else if (expectedValue instanceof Double) {
            return true;
        } else if (expectedValue instanceof Long) {
            return true;
        } else if (expectedValue instanceof Short) {
            return true;
        } else if (expectedValue instanceof Byte) {
            return true;
        }
        return false;
    }

    private String getClassLiteral(JavaExecHarness.JavaTestCase testCase) {
        return testCase.clazz.getName() + ".class";
    }

    private String getExceptionName(Run run) {
        return run.expectedException.getName() + ".class";
    }
}

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
package test.com.sun.max.vm.testrun;

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

public class JavaTesterGenerator {

    private static final OptionSet _options = new OptionSet(true);

    private static final Option<Boolean> _exceptionsOption = _options.newBooleanOption("exceptions", true,
                    "Selects whether runs that are expected to throw exceptions will be tested.");
    private static final Option<Boolean> _runStringsOption = _options.newBooleanOption("run-strings", true,
                    "Selects whether the input values will be reported for a test case that fails.");
    private static final Option<Boolean> _loadedOption = _options.newBooleanOption("force-loaded", true,
                    "Specifies that all test classes will be loaded into the target.");
    private static final Option<Boolean> _resolvedOption = _options.newBooleanOption("force-resolved", true,
                    "Specifies that all test classes and method will be resolved in the target.");
    private static final Option<Boolean> _restartOption = _options.newBooleanOption("restart", true,
                    "Specifies that generated run scheme will allow starting the tests from a particular test number.");
    private static final Option<Integer> _verboseOption = _options.newIntegerOption("verbose", 3,
                    "Specifies the verbose level of the generated tests.");
    private static final Option<Boolean> _compileOption = _options.newBooleanOption("compile", true,
                    "Compiles the generate Java source code automatically using a Java source compiler.");
    private static final Option<String> _packageOption = _options.newStringOption("package", "some",
                    "");
    private static final Option<Boolean> _sortOption = _options.newBooleanOption("alphabetical", true,
                    "Generates the test cases in alphabetical order.");

    public static class Executor implements JavaExecHarness.Executor {
        public void initialize(JavaExecHarness.JavaTestCase tc, boolean loadingPackages) {
            // do nothing.
        }
        public Object execute(JavaTestCase c, Object[] vals) throws InvocationTargetException {
            return null;
        }
    }

    final IndentWriter _writer;

    public static void generate(OptionSet options, String[] args) {
        _options.loadOptions(options);
        generate(args);
    }

    public static void main(String[] args) {
        generate(_options.parseArguments(args).getArguments());
    }

    private static void generate(final String[] arguments) throws ProgramError {
        final Registry<TestHarness> registry = new Registry<TestHarness>(TestHarness.class, false);
        final JavaExecHarness javaExecHarness = new JavaExecHarness(new Executor());
        registry.registerObject("java", javaExecHarness);
        final TestEngine engine = new TestEngine(registry);
        engine.parseTests(arguments, _sortOption.getValue());
        try {
            final String runSchemeFile = fileName("JavaTesterRunScheme");
            final String testRunsFile = fileName("JavaTesterTests");

            final LinkedList<JavaTestCase> cases = extractJavaTests(engine);

            generateRunSchemeContent(new File(runSchemeFile), cases);
            generateTestRunsContent(new File(testRunsFile), cases);

            System.out.println(runSchemeFile + " updated.");
            if (_compileOption.getValue()) {
                ToolChain.compile(new String[] {className("JavaTesterRunScheme"), className("JavaTesterTests")});
                System.out.println(runSchemeFile + " recompiled.");
                System.out.println(testRunsFile + " recompiled.");
            }
        } catch (IOException e) {
            ProgramError.unexpected(e);
        }
    }

    private static String className(String className) {
        return (new Package().name() + "." + _packageOption.getValue() + ".") + className;
    }

    private static String fileName(String className) {
        return className(className).replace('.', File.separatorChar) + ".java";
    }

    private static void generateRunSchemeContent(final File runSchemeFile, final LinkedList<JavaTestCase> cases) throws IOException {
        final Writer writer = new StringWriter();
        final JavaTesterGenerator gen = new JavaTesterGenerator(writer);
        gen.genClassList(cases);
        gen.genRunMethod(cases);
        writer.close();
        Files.updateGeneratedContent(runSchemeFile, ReadableSource.Static.fromString(writer.toString()), "// GENERATED TEST RUNS", "// END GENERATED TEST RUNS");
    }

    private static void generateTestRunsContent(final File testRunsFile, final LinkedList<JavaTestCase> cases) throws IOException {
        final Writer writer = new StringWriter();
        final JavaTesterGenerator gen = new JavaTesterGenerator(writer);
        gen.genTestRuns(cases);
        writer.close();
        Files.updateGeneratedContent(testRunsFile, ReadableSource.Static.fromString(writer.toString()), "// GENERATED TEST RUNS", "// END GENERATED TEST RUNS");
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

    public JavaTesterGenerator(Writer w) {
        _writer = new IndentWriter(w);
    }

    public void genClassList(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        _writer.indent();
        _writer.println("private static final Class<?>[] _classList = {");
        _writer.indent();
        final Iterator<JavaExecHarness.JavaTestCase> iterator = testCases.iterator();
        while (iterator.hasNext()) {
            _writer.print(getClassLiteral(iterator.next()));
            if (iterator.hasNext()) {
                _writer.println(",");
            } else {
                _writer.println("");
            }
        }
        _writer.outdent();
        _writer.println("};");
        _writer.outdent();
    }
    public void genInitMethod(Iterable<JavaExecHarness.JavaTestCase> testCases) {
        if (_loadedOption.getValue()) {
            _writer.indent();
            _writer.println("@Override");
            _writer.println("public void initialize(VM.Phase phase) {");
            _writer.indent();
            _writer.println("_verbose = " + _verboseOption.getValue() + ";");
            _writer.println("if (VM.isPrototyping()) {");
            _writer.indent();
            _writer.println("for (Class<?> testClass : _classList) {");
            _writer.indent();
            _writer.println("addClassToImage(testClass);");
            _writer.outdent();
            _writer.println("}");
            _writer.outdent();
            _writer.println("}");
            _writer.outdent();
            _writer.println("}");
            _writer.outdent();
        }
    }

    public void genRunMethod(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        _writer.indent();
        _writer.println("@Override");
        _writer.println("public void runTests() {");
        _writer.indent();
        if (_restartOption.getValue()) {
            _writer.println("_total = _testEnd - _testStart;");
            _writer.println("_testNum = _testStart;");
            _writer.println("while (_testNum < _testEnd) {");
            _writer.indent();
            _writer.println("switch(_testNum) {");
            _writer.indent();
        } else {
            _writer.println("_total = " + testCases.size() + ";");
            _writer.println("_testNum = 0;");
        }
        int i = 0;
        for (JavaExecHarness.JavaTestCase testCase : testCases) {
            String spaces = "";
            if (_restartOption.getValue()) {
                spaces = "    ";
                if (i > 0) {
                    _writer.println(spaces + "break;");
                }
                _writer.println("case " + (i++) + ":");
            }
            _writer.println(spaces + "JavaTesterTests." + getTestCaseName(testCase) + "();");
        }
        if (_restartOption.getValue()) {
            _writer.outdent();
            _writer.println("}");
            _writer.outdent();
            _writer.println("}");
        }
        _writer.println("reportPassed(_passed, _total);");
        _writer.outdent();
        _writer.println("}");
        _writer.outdent();
    }

    private void genTestRuns(LinkedList<JavaExecHarness.JavaTestCase> testCases) {
        _writer.indent();
        for (JavaExecHarness.JavaTestCase testCase : testCases) {
            genTestCase(testCase, _exceptionsOption.getValue());
        }
        _writer.outdent();
    }

    public void genTestCase(JavaExecHarness.JavaTestCase testCase, boolean testExceptions) {
        _writer.print("static void ");
        _writer.println(getTestCaseName(testCase) + "() {");
        _writer.indent();
        if (_verboseOption.getValue() > 2) {
            _writer.println("JavaTesterRunScheme.begin(\"" + testCase._clazz.getName() + "\");");
        }
        _writer.println("String runString = null;");
        _writer.println("try {");
        for (JavaExecHarness.Run run : testCase._runs) {
            genRun(testCase, run, testExceptions);
        }
        _writer.println("} catch (Throwable t) {");
        _writer.println("    JavaTesterRunScheme.end(runString, t);");
        _writer.println("    return;");
        _writer.println("}");
        _writer.println("JavaTesterRunScheme.end(null, true);");
        _writer.outdent();
        _writer.println("}");
    }

    private String getTestCaseName(JavaExecHarness.JavaTestCase testCase) {
        return testCase._clazz.getName().replaceAll("\\.", "_");
    }

    public void genRun(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run, boolean testExceptions) {
        if (!testExceptions) {
            return;
        }
        genRunComment(testCase, run);
        genTestRun(testCase, run);
    }

    private void genRunComment(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        _writer.print("// ");
        _writer.print(JavaExecHarness.inputToString(testCase._clazz, run, false));
        _writer.print(" == ");
        _writer.print(JavaExecHarness.resultToString(run._expectedValue, run._expectedException));
        _writer.println();
    }

    private void genTestRun(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        _writer.indent();
        String runString = "null";
        if (_runStringsOption.getValue()) {
            runString = JavaExecHarness.inputToString(testCase._clazz, run, true);
        }
        if (run._expectedException != null) {
            // an exception is expected, a value is NOT expected
            _writer.println("try {");
            _writer.indent();
            _writer.println("runString = " + runString + ";");
            genTestCall(testCase, run);
            _writer.println(";");
            _writer.println("JavaTesterRunScheme.end(runString, false);");
            _writer.println("return;");
            _writer.outdent();
            _writer.println("} catch (Throwable e) {");
            _writer.indent();
            _writer.println("if (e.getClass() != " + getExceptionName(run) + ") {");
            _writer.println("    JavaTesterRunScheme.end(runString, e);");
            _writer.println("    return;");
            _writer.println("}");
            _writer.outdent();
            _writer.println("}");
        } else {
            // check the return value against the expected return value
            if (useEquals(run._expectedValue)) {
                _writer.println("runString = " + runString + ";");
                _writer.print("if (");
                genValue(run._expectedValue);
                _writer.print(" != ");
                genTestCall(testCase, run);
                _writer.println(") {");
            } else {
                _writer.println("runString = " + runString + ";");
                _writer.print("if (!");
                genValue(run._expectedValue);
                _writer.print(".equals(");
                genTestCall(testCase, run);
                _writer.println(")) {");
            }
            _writer.indent();
            _writer.println("JavaTesterRunScheme.end(runString, false);");
            _writer.println("return;");
            _writer.outdent();
            _writer.println("}");
        }
        _writer.outdent();
    }

    private void genTestCall(JavaExecHarness.JavaTestCase testCase, JavaExecHarness.Run run) {
        _writer.print(testCase._clazz.getName() + ".test(");
        for (int i = 0; i < run._input.length; i++) {
            if (i > 0) {
                _writer.print(", ");
            }
            genValue(run._input[i]);
        }
        _writer.print(")");
    }

    private void genValue(Object v) {
        if (v instanceof Character) {
            final Character chv = (Character) v;
            _writer.print("(char) " + (int) chv.charValue());
        } else if (v instanceof Float) {
            _writer.print(String.valueOf(v) + "f");
        } else if (v instanceof Long) {
            _writer.print(String.valueOf(v) + "L");
        } else if (v instanceof Short) {
            _writer.print("(short) " + String.valueOf(v));
        } else if (v instanceof Byte) {
            _writer.print("(byte) " + String.valueOf(v));
        } else if (v instanceof String) {
            _writer.print("\"" + String.valueOf(v) + "\"");
        } else {
            _writer.print(String.valueOf(v));
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
        return testCase._clazz.getName() + ".class";
    }

    private String getExceptionName(Run run) {
        return run._expectedException.getName() + ".class";
    }
}

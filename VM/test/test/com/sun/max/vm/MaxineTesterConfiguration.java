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

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.*;

import com.sun.max.collect.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;


/**
 * This class encapsulates the configuration of the Maxine tester, which includes
 * which tests to run, their expected results, example inputs, etc.
 *
 * @author Ben L. Titzer
 */
public class MaxineTesterConfiguration {

    private static final Expectation FAIL_ALL = new Expectation(null, null, ExpectedResult.FAIL);
    private static final Expectation FAIL_SPARC = new Expectation(OperatingSystem.SOLARIS, ProcessorModel.SPARCV9, ExpectedResult.FAIL);
    private static final Expectation FAIL_SOLARIS = new Expectation(OperatingSystem.SOLARIS, null, ExpectedResult.FAIL);
    private static final Expectation FAIL_DARWIN = new Expectation(OperatingSystem.DARWIN, null, ExpectedResult.FAIL);
    private static final Expectation FAIL_LINUX = new Expectation(OperatingSystem.LINUX, null, ExpectedResult.FAIL);

    private static final Expectation RAND_ALL = new Expectation(null, null, ExpectedResult.NONDETERMINISTIC);
    private static final Expectation RAND_LINUX = new Expectation(OperatingSystem.LINUX, null, ExpectedResult.NONDETERMINISTIC);
    private static final Expectation RAND_DARWIN = new Expectation(OperatingSystem.DARWIN, null, ExpectedResult.NONDETERMINISTIC);

    static final Object[] _outputTestList = {
        test.output.AWTFont.class,                  FAIL_SPARC, FAIL_ALL,
        test.output.JavacTest.class,                FAIL_SPARC, FAIL_LINUX, FAIL_SOLARIS,
        test.output.CatchOutOfMemory.class,         FAIL_SPARC,
        test.output.PrintDate.class,                FAIL_SPARC,
        test.output.HelloWorld.class,               FAIL_SPARC,
        test.output.HelloWorldGC.class,             FAIL_SPARC,
        test.output.ExitCode.class,                 FAIL_SPARC,
        test.output.FloatNanTest.class,             FAIL_SPARC,
        test.output.GetResource.class,              FAIL_SPARC,
        test.output.SafepointWhileInNative.class,   FAIL_SPARC, RAND_LINUX,
        test.output.SafepointWhileInJava.class,     FAIL_SPARC, RAND_LINUX,
        test.output.BlockingQueue.class,            FAIL_SPARC,
        test.output.Recursion.class,                FAIL_SPARC,
        test.output.StaticInitializers.class,       FAIL_SPARC,
        test.output.LocalCatch.class,               FAIL_SPARC,
        test.output.Printf.class,                   FAIL_SPARC,
        test.output.GCTest0.class,                  FAIL_SPARC,
        test.output.GCTest1.class,                  FAIL_SPARC,
        test.output.GCTest2.class,                  FAIL_SPARC,
        test.output.GCTest3.class,                  FAIL_SPARC,
        test.output.GCTest4.class,                  FAIL_SPARC,
        test.output.GCTest5.class,                  FAIL_SPARC,
        test.output.GCTest6.class,                  FAIL_SPARC,
        test.output.HelloWorldReflect.class,        FAIL_SPARC,
        test.output.JREJarLoadTest.class,           FAIL_SPARC,
        test.output.FileReader.class,               FAIL_SPARC,
        test.output.ZipFileReader.class,            FAIL_SPARC,
        test.output.WeakReferenceTest01.class,      FAIL_SPARC, RAND_ALL,
        test.output.WeakReferenceTest02.class,      FAIL_SPARC, RAND_ALL,
        test.output.WeakReferenceTest03.class,      FAIL_SPARC, RAND_ALL,
        test.output.WeakReferenceTest04.class,      FAIL_SPARC, RAND_ALL,
        test.output.MegaThreads.class,              FAIL_SPARC, RAND_ALL,
    };

    static final Object[] _javaTestList = {
        test.except.Catch_NPE_03.class,                FAIL_SPARC,
        test.except.Catch_NPE_04.class,                FAIL_SPARC,
        test.output.Thread_join04.class,               FAIL_SPARC,
        test.threads.Thread_isInterrupted02.class,                  FAIL_LINUX,
        test.jdk.EnumMap01.class,                                   RAND_ALL,
        test.output.ExitCode.class,                    FAIL_SPARC,
        test.hotpath.HP_series.class,                  FAIL_SPARC,
        test.hotpath.HP_array02.class,                 FAIL_SPARC,
        test.except.Catch_StackOverflowError_01.class, FAIL_SPARC,
        test.except.Catch_StackOverflowError_02.class, FAIL_SPARC,
    };

    static final String[] _dacapoTests = {
        "antlr",
        "bloat",
        "xalan",
        "hsqldb",
        "luindex",
        "lusearch",
        "jython",
        "chart",
        "eclipse",
        "fop",
        "pmd"
    };

    static final String[] _specjvm98Tests = {
        "_201_compress",
        "_202_jess",
        "_205_raytrace",
        "_209_db",
        "_213_javac",
        "_222_mpegaudio",
        "_227_mtrt",
        "_228_jack"
    };

    static final String[] _specjvm98IgnoredLinePatterns = {
        "Total memory",
        "## IO time",
        "Finished in",
        "Decoding time:"
    };

    static final Object[][] _shootoutBenchmarks = {
        {"ackermann",       "10"},
        {"ary",             "10000", "300000"},
        {"binarytrees",     "12", "16", "18"},
        {"chameneos",       "1000", "250000"},
        {"chameneosredux",  "1000", "250000"},
        {"except",          "10000", "100000", "1000000"},
        {"fannkuch",        "8", "10", "11"},
        {"fasta",           "1000", "250000"},
        {"fibo",            "22", "32", "42"},
        {"harmonic",        "1000000", "200000000"},
        {"hash",            "100000", "1000000"},
        {"hash2",           "100", "1000", "2000"},
        {"heapsort",        "10000", "1000000", "3000000"},
        {"knucleotide",     new File("knucleotide.stdin")},
        {"lists",           "10", "100", "1000"},
        {"magicsquares",    "3", "4"},
        {"mandelbrot",      "100", "1000", "5000"},
        {"matrix",          "1000", "10000", "20000"},
        {"message",         "1000", "5000", "15000"},
        {"meteor",          "2098"},
        {"methcall",        "100000000", "1000000000"},
        {"moments",         new File("moments.stdin")},
        {"nbody",           "500000", "5000000"},
        {"nestedloop",      "10", "20", "35"},
        {"nsieve",          "8", "10", "11"},
        {"nsievebits",      "8", "10", "11"},
        {"objinst",         "100000", "1000000", "5000000"},
        {"partialsums",     "10000", "2000000"},
        {"pidigits",        "30", "1000"},
        {"process",         "10", "250"},
        {"prodcons",        "100", "100000"},
        {"random",          "1000000", "500000000"},
        {"raytracer",       "10", "200"},
        {"recursive",       "10"},
        {"regexdna",        new File("regexdna.stdin")},
        {"regexmatch",      new File("regexmatch.stdin")},
        {"revcomp",         new File("revcomp.stdin")},
        {"reversefile",     new File("reversefile.stdin")},
        {"sieve",           "100", "20000"},
        {"spectralnorm",    "100", "3000"},
        {"spellcheck",      new File("spellcheck.stdin")},
        {"strcat",          "100000", "5000000"},
        {"sumcol",          new File("sumcol.stdin")},
        {"takfp",           "5", "11"},
        {"threadring",      "100", "50000"},
        {"wc",              new File("wc.stdin")},
        {"wordfreq",         new File("wordfreq.stdin")},
    };

    static final Map<String, Object[]> _shootoutInputs = buildShootoutMap();

    static final Class[] _outputTestClasses = buildOutputTestClassArray();

    static final Map<String, Expectation[]> _testExpectationMap = buildTestExpectationMap();

    static Map<String, Object[]> buildShootoutMap() {
        final Map<String, Object[]> map = new HashMap<String, Object[]>();
        for (Object[] array : _shootoutBenchmarks) {
            map.put((String) array[0], Arrays.copyOfRange(array, 1, array.length));
        }
        return map;
    }

    static Class[] buildOutputTestClassArray() {
        final List<Class> classes = new ArrayList<Class>();
        for (Object o : _outputTestList) {
            if (o instanceof Class) {
                classes.add((Class) o);
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }

    static Map<String, Expectation[]> buildTestExpectationMap() {
        final Map<String, Expectation[]> map = new HashMap<String, Expectation[]>();
        addTestExpectations(map, _outputTestList);
        addTestExpectations(map, _javaTestList);
        return map;
    }

    private static void addTestExpectations(final Map<String, Expectation[]> map, final Object[] testList) throws ProgramError {
        for (int i = 0; i < testList.length; i++) {
            final Object o = testList[i];
            if (o instanceof Class) {
                final List<Expectation> list = new ArrayList<Expectation>();
                // Checkstyle: stop
                for (i++; i < testList.length; i++) {
                    final Object e = testList[i];
                    if (e instanceof Expectation) {
                        list.add((Expectation) e);
                    } else if (e instanceof Class) {
                        i--;
                        break;
                    } else {
                        throw ProgramError.unexpected("format of output test class list is wrong");
                    }
                }
                // Checkstyle: resume
                map.put(((Class) o).getName(), list.toArray(new Expectation[list.size()]));
            } else {
                throw ProgramError.unexpected("format of output test class list is wrong");
            }
        }
    }

    static final String[] _outputTests = com.sun.max.lang.Arrays.map(_outputTestClasses, String.class, new MapFunction<Class, String>() {
        public String map(Class from) {
            return from.getSimpleName();
        }
    });

    static void addTestName(Object object, Set<String> testNames) {
        if (object instanceof Class) {
            final Class c = (Class) object;
            testNames.add(c.getName());
        } else if (object instanceof Iterable) {
            for (Object o : (Iterable) object) {
                addTestName(o, testNames);
            }
        } else if (object instanceof Object[]) {
            testNames.addAll(toTestNames((Object[]) object));
        } else {
            testNames.add(object.toString());
        }
    }

    static Set<String> toTestNames(Object... objects) {
        final Set<String> testNames = new HashSet<String>(objects.length);
        for (Object object : objects) {
            addTestName(object, testNames);
        }
        return testNames;
    }

    static final String[] _expectedAutoTestFailures = {
        "test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_arrayCopyForKinds(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)",
        "test_catchNull(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_throw)",
        "test_manyParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_nop(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_nop_cfunction(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_reference_identity(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_sameNullsArrayCopy(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)"
    };

    static final Map<String, String[]> _imageConfigs = new HashMap<String, String[]>();
    static final Map<String, String[]> _maxvmConfigs = new HashMap<String, String[]>();

    static {
        MaxineTesterConfiguration._imageConfigs.put("optopt", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests"});
        MaxineTesterConfiguration._imageConfigs.put("optjit", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-callee-jit"});
        MaxineTesterConfiguration._imageConfigs.put("jitopt", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-caller-jit"});
        MaxineTesterConfiguration._imageConfigs.put("jitjit", new String[] {"-run=test.com.sun.max.vm.testrun.all", "-native-tests", "-test-caller-jit", "-test-callee-jit"});
        MaxineTesterConfiguration._imageConfigs.put("java", new String[] {"-run=com.sun.max.vm.run.java"});

        MaxineTesterConfiguration._maxvmConfigs.put("std", new String[0]);
        MaxineTesterConfiguration._maxvmConfigs.put("jit", new String[] {"-Xjit"});
        MaxineTesterConfiguration._maxvmConfigs.put("pgi", new String[] {"-XX:+PGI"});
        MaxineTesterConfiguration._maxvmConfigs.put("mx256m", new String[] {"-Xmx256m"});
        MaxineTesterConfiguration._maxvmConfigs.put("mx512m", new String[] {"-Xmx512m"});

        for (String s : _expectedAutoTestFailures) {
            // add the failing autotests to the expectation map
            _testExpectationMap.put(s, new Expectation[] {FAIL_ALL});
        }
    }

    private static final String DEFAULT_MAXVM_OUTPUT_CONFIGS = "std,jit,pgi";
    private static final String DEFAULT_JAVA_TESTER_CONFIGS = "optopt,jitopt,optjit,jitjit";

    public static String defaultMaxvmOutputConfigs() {
        return "std,jit,pgi";
    }

    public static String defaultJavaTesterConfigs() {
        final Platform platform = Platform.host();
        if (platform.operatingSystem() == OperatingSystem.SOLARIS) {
            final ProcessorKind processorKind = platform.processorKind();
            if (processorKind.processorModel() == ProcessorModel.SPARCV9) {
                return "optopt";
            }
        }
        return DEFAULT_JAVA_TESTER_CONFIGS;
    }

    public static String[] getImageConfigArgs(String imageConfig) {
        final String[] args = _imageConfigs.get(imageConfig);
        if (args == null) {
            ProgramError.unexpected("unknown image config: " + imageConfig);
        }
        return args;
    }

    public static String[] getMaxvmConfigArgs(String maxvmConfig) {
        final String[] args = _maxvmConfigs.get(maxvmConfig);
        if (args == null) {
            ProgramError.unexpected("unknown maxvm config: " + maxvmConfig);
        }
        return args;
    }

    public enum ExpectedResult {
        PASS {
            @Override
            public boolean matchesActualResult(boolean passed) {
                return passed;
            }
        },
        FAIL {
            @Override
            public boolean matchesActualResult(boolean passed) {
                return !passed;
            }
        },
        NONDETERMINISTIC {
            @Override
            public boolean matchesActualResult(boolean passed) {
                return true;
            }
        };

        public abstract boolean matchesActualResult(boolean passed);
    }

    /**
     * Determines if a given test is known to fail.
     *
     * @param testName a unique identifier for the test
     * @param config the {@linkplain #_maxvmConfigs maxvm} configuration used during the test execution. This value may be null.
     */
    public static ExpectedResult expectedResult(String testName, String config) {
        final Expectation[] expect = _testExpectationMap.get(testName);
        if (expect != null) {
            final Platform platform = Platform.host();
            for (Expectation e : expect) {
                if (e.matches(platform)) {
                    return e._expectedResult;
                }
            }
        }
        return ExpectedResult.PASS;
    }


    static final Set<Class> _slowAutoTestClasses = new HashSet<Class>(Arrays.asList((Class)
                    CompilerTest_max.class,
                    CompilerTest_coreJava.class,
                    CompilerTest_large.class,
                    JitCompilerTestCase.class,
                    BytecodeTest_subtype.class));

    /**
     * Determines which JUnit test cases are known to take a non-trivial amount of time to execute.
     * These tests are omitted by the MaxineTester unless the
     * @param testCase
     * @return
     */
    public static boolean isSlowAutoTestCase(TestCase testCase) {
        for (Class<?> c : _slowAutoTestClasses) {
            if (c.isAssignableFrom(testCase.getClass())) {
                return true;
            }
        }
        return false;
    }

    public static String[] getVMOptions(String maxvmConfig) {
        if (!_maxvmConfigs.containsKey(maxvmConfig)) {
            ProgramError.unexpected("Unknown Maxine VM option configuration: " + maxvmConfig);
        }
        return _maxvmConfigs.get(maxvmConfig);
    }

    public static String[] shootoutTests() {
        return _shootoutInputs.keySet().toArray(new String[0]);
    }

    public static Object[] shootoutInputs(String benchmark) {
        return _shootoutInputs.get(benchmark);
    }

    private static class Expectation {
        private final OperatingSystem _os; // null indicates all OSs
        private final ProcessorModel _processor; // null indicates all processors
        private final ExpectedResult _expectedResult;

        Expectation(OperatingSystem os, ProcessorModel pm, ExpectedResult e) {
            _os = os;
            _processor = pm;
            _expectedResult = e;
        }

        public boolean matches(Platform platform) {
            if (_os == null || _os == platform.operatingSystem()) {
                if (_processor == null || _processor == platform.processorKind().processorModel()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            buffer.append(_os == null ? "ANY" : _os.toString());
            buffer.append("/");
            buffer.append(_processor == null ? "ANY" : _processor.toString());
            buffer.append(" = ");
            buffer.append(_expectedResult);
            return buffer.toString();
        }
    }

}

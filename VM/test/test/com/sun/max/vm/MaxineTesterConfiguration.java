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

import com.sun.max.platform.*;
import com.sun.max.program.*;



public class MaxineTesterConfiguration {

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

    static Map<String, Object[]> buildShootoutMap() {
        final Map<String, Object[]> map = new HashMap<String, Object[]>();
        for (Object[] array : _shootoutBenchmarks) {
            map.put((String) array[0], Arrays.copyOfRange(array, 1, array.length));
        }
        return map;
    }

    static final Class[] _outputTestClasses = {
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.ExitCode.class,
        test.output.FloatNanTest.class,
        test.output.GetResource.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.BlockingQueue.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest0.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
        test.output.GCTest5.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.FileReader.class,
        test.output.ZipFileReader.class,
        test.output.WeakReferenceTest01.class,
        test.output.WeakReferenceTest02.class,
        test.output.WeakReferenceTest03.class,
        test.output.MegaThreads.class
    };

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
        "test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.amd64.AMD64EirTranslatorTest_native)",
        "test_arrayCopyForKinds(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)",
        "test_catchNull(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_throw)",
        "test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.amd64.AMD64EirTranslatorTest_native)",
        "test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_manyParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_nop(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_nop_cfunction(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_reference_identity(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",
        "test_sameNullsArrayCopy(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)"
    };

    static final Set<String> _expectedFailuresSolarisAMD64 = toTestNames(
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        _expectedAutoTestFailures
    );

    static final Set<String> _expectedFailuresLinuxAMD64 = toTestNames(
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        test.threads.Thread_isInterrupted02.class,
        _expectedAutoTestFailures
    );

    static final Set<String> _nonDeterministicFailuresLinuxAMD64 = toTestNames(
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class
    );

    static final Set<String> _expectedFailuresDarwinAMD64 = toTestNames(
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        _expectedAutoTestFailures
    );

    static final Set<String> _nonDeterministicFailuresDarwinAMD64 = toTestNames();

    static final Set<String> _expectedFailuresSolarisSPARCV9 = toTestNames(
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.ExitCode.class,
        test.output.GetResource.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.BlockingQueue.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest0.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
        test.output.GCTest5.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.FileReader.class,
        test.output.ZipFileReader.class,
        test.output.FloatNanTest.class,
        test.output.WeakReferenceTest01.class,
        test.output.WeakReferenceTest02.class,
        test.output.WeakReferenceTest03.class,
        test.output.MegaThreads.class,
        test.output.Thread_join04.class,
        test.hotpath.HP_array02.class, // 329
        test.hotpath.HP_series.class, // 333
        test.except.Catch_StackOverflowError_01.class,
        test.except.Catch_StackOverflowError_02.class,
        _expectedAutoTestFailures
    );

    static final Set<String> _expectedJitFailuresSolarisSPARCV9 = toTestNames(
        test.output.JavacTest.class,
        test.output.PrintDate.class,
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.BlockingQueue.class,
        test.output.GetResource.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest0.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
        test.output.GCTest5.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.FileReader.class,
        test.output.ZipFileReader.class,
        test.output.FloatNanTest.class,
        test.output.FloatNanTest.class,
        test.output.JavacTest.class,
        test.output.WeakReferenceTest01.class,
        test.output.WeakReferenceTest02.class,
        test.output.WeakReferenceTest03.class,
        test.output.MegaThreads.class,
        test.bytecode.BC_frem.class,  // 45
        test.except.Catch_NPE_03.class, // 202
        test.except.Catch_NPE_04.class, // 203
        test.except.Catch_NPE_06.class, // 205
        test.lang.ClassLoader_loadClass01.class, // 231
        test.lang.Class_asSubclass01.class, // 233
        test.lang.Class_cast01.class,
        test.lang.Class_forName01.class,
        test.lang.Class_forName02.class,
        test.lang.Class_forName03.class,
        test.lang.Class_forName04.class,
        test.lang.Object_clone01.class,
        test.lang.Object_notify01.class,
        test.lang.Object_notifyAll01.class,
        test.lang.Object_wait01.class,
        test.output.Thread_join04.class,
        test.reflect.Array_get01.class,
        test.reflect.Array_get02.class,
        test.reflect.Array_get03.class,
        test.reflect.Array_getBoolean01.class,
        test.output.ExitCode.class,
        test.hotpath.HP_series.class, // 333
        test.except.Catch_StackOverflowError_01.class,
        test.except.Catch_StackOverflowError_02.class
    );

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
        MaxineTesterConfiguration._maxvmConfigs.put("pgi", new String[] {"-XX:PGI"});
        MaxineTesterConfiguration._maxvmConfigs.put("mx256m", new String[] {"-Xmx256m"});
        MaxineTesterConfiguration._maxvmConfigs.put("mx512m", new String[] {"-Xmx512m"});
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
        final Platform platform = Platform.host();
        if (platform.operatingSystem() == OperatingSystem.SOLARIS) {
            final ProcessorKind processorKind = platform.processorKind();
            if (processorKind.processorModel() == ProcessorModel.AMD64) {
                return _expectedFailuresSolarisAMD64.contains(testName) ? ExpectedResult.FAIL : ExpectedResult.PASS;
            } else if (processorKind.processorModel() == ProcessorModel.SPARCV9) {
                if (config != null && config.contains("jit")) {
                    return _expectedJitFailuresSolarisSPARCV9.contains(testName) ? ExpectedResult.FAIL : ExpectedResult.PASS;
                }
                return _expectedFailuresSolarisSPARCV9.contains(testName) ? ExpectedResult.FAIL : ExpectedResult.PASS;
            }
        } else if (platform.operatingSystem() == OperatingSystem.LINUX) {
            final ProcessorKind processorKind = platform.processorKind();
            if (processorKind.processorModel() == ProcessorModel.AMD64) {
                if (_nonDeterministicFailuresLinuxAMD64.contains(testName)) {
                    return ExpectedResult.NONDETERMINISTIC;
                }
                return _expectedFailuresLinuxAMD64.contains(testName) ? ExpectedResult.FAIL : ExpectedResult.PASS;
            }
        } else if (platform.operatingSystem() == OperatingSystem.DARWIN) {
            final ProcessorKind processorKind = platform.processorKind();
            if (_nonDeterministicFailuresDarwinAMD64.contains(testName)) {
                return ExpectedResult.NONDETERMINISTIC;
            }
            if (processorKind.processorModel() == ProcessorModel.AMD64) {
                return _expectedFailuresDarwinAMD64.contains(testName) ? ExpectedResult.FAIL : ExpectedResult.PASS;
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
}

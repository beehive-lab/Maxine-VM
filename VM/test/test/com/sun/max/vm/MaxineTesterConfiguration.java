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
import java.util.Arrays;

import junit.framework.*;

import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;

/**
 * This class encapsulates the configuration of the Maxine tester, which includes
 * which tests to run, their expected results, example inputs, etc.
 *
 * @author Ben L. Titzer
 */
public class MaxineTesterConfiguration {

    static final Expectation FAIL_ALL = new Expectation(null, null, ExpectedResult.FAIL);
    static final Expectation FAIL_SPARC = new Expectation(OperatingSystem.SOLARIS, ProcessorModel.SPARCV9, ExpectedResult.FAIL);
    static final Expectation FAIL_SOLARIS = new Expectation(OperatingSystem.SOLARIS, null, ExpectedResult.FAIL);
    static final Expectation FAIL_DARWIN = new Expectation(OperatingSystem.DARWIN, null, ExpectedResult.FAIL);
    static final Expectation FAIL_LINUX = new Expectation(OperatingSystem.LINUX, null, ExpectedResult.FAIL);

    static final Expectation RAND_ALL = new Expectation(null, null, ExpectedResult.NONDETERMINISTIC);
    static final Expectation RAND_LINUX = new Expectation(OperatingSystem.LINUX, null, ExpectedResult.NONDETERMINISTIC);
    static final Expectation RAND_DARWIN = new Expectation(OperatingSystem.DARWIN, null, ExpectedResult.NONDETERMINISTIC);
    static final Expectation RAND_AMD64 = new Expectation(null, ProcessorModel.AMD64, ExpectedResult.NONDETERMINISTIC);
    static final Expectation RAND_SPARC = new Expectation(OperatingSystem.SOLARIS, ProcessorModel.SPARCV9, ExpectedResult.NONDETERMINISTIC);

    static final Expectation PASS_SOLARIS_AMD64 = new Expectation(OperatingSystem.SOLARIS, ProcessorModel.AMD64, ExpectedResult.PASS);
    static final Expectation PASS_DARWIN_AMD64 = new Expectation(OperatingSystem.DARWIN, ProcessorModel.AMD64, ExpectedResult.PASS);

    static final List<Class> zeeOutputTests = new LinkedList<Class>();
    static final List<String> zeeDacapoTests = new LinkedList<String>();
    static final List<String> zeeSpecjvm98Tests = new LinkedList<String>();
    static final List<String> zeeShootoutTests = new LinkedList<String>();
    static final List<String> zeeMaxvmConfigs = new LinkedList<String>();

    static final Map<String, Expectation[]> configResultMap = new HashMap<String, Expectation[]>();
    static final Map<String, Expectation[]> resultMap = new HashMap<String, Expectation[]>();
    static final Map<Object, Object[]> inputMap = new HashMap<Object, Object[]>();
    static final Map<String, String[]> imageParams = new HashMap<String, String[]>();
    static final Map<String, String[]> jtLoadParams = new HashMap<String, String[]>();
    static final Map<String, String[]> maxvmParams = new HashMap<String, String[]>();

    private static Class[] findOutputTests() {
        final ArrayList<Class> result = new ArrayList<Class>();
        new ClassSearch() {
            @Override
            protected boolean visitClass(String className) {
                if (className.startsWith("test.output.")) {
                    Class<?> javaClass = Classes.forName(className, false, ClassSearch.class.getClassLoader());
                    try {
                        javaClass.getDeclaredMethod("main", String[].class);
                        result.add(javaClass);
                    } catch (Exception e) {
                    }
                }
                return true;
            }
        }.run(Classpath.fromSystem());
        Class[] classes = result.toArray(new Class[result.size()]);
        Arrays.sort(classes, new Comparator<Class>() {
            public int compare(Class o1, Class o2) {
                return o1.getSimpleName().compareTo(o2.getSimpleName());
            }
        });
        return classes;
    }

    static {
        // Register all "test.output.*" classes on the class path
        output(findOutputTests());

        // Refine expectation for certain output tests
        output(test.output.AWTFont.class,                  FAIL_DARWIN, RAND_SPARC);
        output(test.output.JavacTest.class,                RAND_LINUX);
        output(test.output.GCTest7.class,                  RAND_ALL);
        output(test.output.MegaThreads.class,              RAND_ALL);
        output(test.output.SafepointWhileInJava.class,     RAND_LINUX);


        jtt(jtt.jasm.Invokevirtual_private01.class, RAND_ALL); // may fail due to incorrect invokevirtual / invokespecial optimization
        jtt(jtt.except.BC_invokespecial01.class, RAND_ALL);      // may fail due to incorrect invokevirtual / invokespecial optimization
        jtt(jtt.except.BC_invokevirtual02.class, RAND_ALL);      // may fail due to incorrect invokevirtual / invokespecial optimization
        jtt(jtt.optimize.NCE_FlowSensitive02.class, RAND_ALL);   // Fails on all but C1X due to missing explicit null pointer checks
        jtt(jtt.threads.Thread_isInterrupted02.class,     FAIL_LINUX);
        jtt(jtt.threads.Thread_isInterrupted05.class,     RAND_LINUX);
        jtt(jtt.jdk.EnumMap01.class,                      RAND_ALL);
        jtt(jtt.jdk.EnumMap02.class,                      RAND_ALL);
        jtt(jtt.hotpath.HP_series.class,                  RAND_SPARC);  // Fails:  @jitcps, @cpscps
        jtt(jtt.hotpath.HP_array02.class,                 RAND_SPARC);  // Fails:  @jitcps, @cpscps

        jtt(jtt.except.Catch_StackOverflowError_01.class, FAIL_LINUX);  // For some reason, the mprotect of the yellow
        jtt(jtt.except.Catch_StackOverflowError_02.class, FAIL_LINUX);  // stack guard page is not working (at least
        jtt(jtt.except.Catch_StackOverflowError_03.class, FAIL_LINUX);  // on the linux gate machine - diy-3-18)

        dacapo("antlr");
        dacapo("bloat");
        dacapo("xalan",    FAIL_ALL);
        dacapo("hsqldb");
        dacapo("luindex");
        dacapo("lusearch", FAIL_ALL);
        dacapo("jython",   FAIL_ALL);
        dacapo("chart",    FAIL_ALL);
        dacapo("eclipse",  FAIL_ALL);
        dacapo("fop");
        dacapo("pmd");

        specjvm98("_201_compress");
        specjvm98("_202_jess");
        specjvm98("_205_raytrace");
        specjvm98("_209_db");
        specjvm98("_213_javac");
        specjvm98("_222_mpegaudio");
        specjvm98("_227_mtrt");
        specjvm98("_228_jack");

        shootout("ackermann",       "10");
        shootout("ary",             "10000", "300000");
        shootout("binarytrees",     "12", "16", "18");
        shootout("chameneos",       "1000", "250000");
        shootout("chameneosredux",  "1000", "250000");
        shootout("except",          "10000", "100000", "1000000");
        shootout("fannkuch",        "8", "10", "11");
        shootout("fasta",           "1000", "250000");
        shootout("fibo",            "22", "32", "42");
        shootout("harmonic",        "1000000", "200000000");
        shootout("hash",            "100000", "1000000");
        shootout("hash2",           "100", "1000", "2000");
        shootout("heapsort",        "10000", "1000000", "3000000");
        shootout("knucleotide",     new File("knucleotide.stdin"));
        shootout("lists",           "10", "100", "1000");
        shootout("magicsquares",    "3", "4");
        shootout("mandelbrot",      "100", "1000", "5000");
        shootout("matrix",          "1000", "10000", "20000");
        shootout("message",         "1000", "5000", "15000");
        shootout("meteor",          "2098");
        shootout("methcall",        "100000000", "1000000000");
        shootout("moments",         new File("moments.stdin"));
        shootout("nbody",           "500000", "5000000");
        shootout("nestedloop",      "10", "20", "35");
        shootout("nsieve",          "8", "10", "11");
        shootout("nsievebits",      "8", "10", "11");
        shootout("objinst",         "100000", "1000000", "5000000");
        shootout("partialsums",     "10000", "2000000");
        shootout("pidigits",        "30", "1000");
        shootout("process",         "10", "250");
        shootout("prodcons",        "100", "100000");
        shootout("random",          "1000000", "500000000");
        shootout("raytracer",       "10", "200");
        shootout("recursive",       "10");
        shootout("regexdna",        new File("regexdna.stdin"));
        shootout("regexmatch",      new File("regexmatch.stdin"));
        shootout("revcomp",         new File("revcomp.stdin"));
        shootout("reversefile",     new File("reversefile.stdin"));
        shootout("sieve",           "100", "20000");
        shootout("spectralnorm",    "100", "3000");
        shootout("spellcheck",      new File("spellcheck.stdin"));
        shootout("strcat",          "100000", "5000000");
        shootout("sumcol",          new File("sumcol.stdin"));
        shootout("takfp",           "5", "11");
        shootout("threadring",      "100", "50000");
        shootout("wc",              new File("wc.stdin"));
        shootout("wordfreq",        new File("wordfreq.stdin"));

        auto("test_arrayCopyForKinds(test.com.sun.max.vm.cps.eir.sparc.SPARCEirTranslatorTest_jdk_System)",  FAIL_ALL);
        auto("test_catchNull(test.com.sun.max.vm.cps.eir.sparc.SPARCEirTranslatorTest_throw)",               FAIL_ALL);
        auto("test_sameNullsArrayCopy(test.com.sun.max.vm.cps.eir.sparc.SPARCEirTranslatorTest_jdk_System)", FAIL_ALL);
        auto("test_count_bits(test.com.sun.max.vm.cps.eir.sparc.SPARCEirTranslatorTest_regressions)", FAIL_ALL);
        auto("test_count_bits(test.com.sun.max.vm.cps.sparc.SPARCTranslatorTest_regressions)", FAIL_ALL);

        imageConfig("java", "-run=java");
        imageConfig("cpscps", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests");
        imageConfig("cpsjit", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-callee-jit");
        imageConfig("jitcps", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-caller-jit");
        imageConfig("jitjit", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-caller-jit", "-test-callee-jit");
        imageConfig("cpsc1x", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-callee-c1x", PASS_SOLARIS_AMD64, PASS_DARWIN_AMD64);

        String c1xPackage = "com.sun.max.vm.compiler.c1x";
        String c1xClass = c1xPackage + ".C1XCompilerScheme";

        jtLoadConfig("cpscps", "-caller=cps", "-callee=cps");
        jtLoadConfig("cpsjit", "-caller=cps", "-callee=jit");
        jtLoadConfig("cpsc1x", "-caller=cps", "-callee=" + c1xClass);
        jtLoadConfig("jitcps", "-caller=jit", "-callee=cps");
        jtLoadConfig("jitjit", "-caller=jit", "-callee=jit");
        jtLoadConfig("jitc1x", "-caller=jit", "-callee=" + c1xClass);
        jtLoadConfig("c1xcps", "-caller=" + c1xClass, "-callee=cps");
        jtLoadConfig("c1xc1x", "-caller=" + c1xClass, "-callee=jit");
        jtLoadConfig("c1xjit", "-caller=" + c1xClass, "-callee=" + c1xClass);

        maxvmConfig("std");
        maxvmConfig("jit", "-Xjit");
        maxvmConfig("opt", "-Xopt");
        maxvmConfig("mx256m", "-Xmx256m");
        maxvmConfig("mx512m", "-Xmx512m");

        // VEE 2010 benchmarking configurations
        maxvmConfig("noGC", "-XX:+DisableGC", "-Xmx3g");
        maxvmConfig("GC", "-Xmx2g");

        imageConfig("jit-c1x0",  "-jit=" + c1xPackage, "-vmargs=-C1X:OptLevel=0");
        imageConfig("jit-c1x1",  "-jit=" + c1xPackage, "-vmargs=-C1X:OptLevel=1");
        imageConfig("jit-c1x2",  "-jit=" + c1xPackage, "-vmargs=-C1X:OptLevel=2");
        imageConfig("jit-c1x3",  "-jit=" + c1xPackage, "-vmargs=-C1X:OptLevel=3");

        imageConfig("opt-c1x0",  "-opt=c1x", "-vmargs=-C1X:OptLevel=0");
        imageConfig("opt-c1x1",  "-opt=c1x", "-vmargs=-C1X:OptLevel=1");
        imageConfig("opt-c1x2",  "-opt=c1x", "-vmargs=-C1X:OptLevel=2");
        imageConfig("opt-c1x3",  "-opt=c1x", "-vmargs=-C1X:OptLevel=3");
    }

    private static void output(Class javaClass, Expectation... results) {
        assert zeeOutputTests.contains(javaClass) : "Output test " + javaClass + " not found by findOutputTests()";
        addExpectedResults(javaClass.getName(), results);
    }

    private static void output(Class... javaClasses) {
        zeeOutputTests.addAll(Arrays.asList(javaClasses));
    }

    private static void jtt(Class javaClass, Expectation... results) {
        addExpectedResults(javaClass.getName(), results);
    }

    private static void dacapo(String name, Expectation... results) {
        zeeDacapoTests.add(name);
        addExpectedResults("Dacapo " + name, results);
    }

    private static void specjvm98(String name, Expectation... results) {
        zeeSpecjvm98Tests.add(name);
        addExpectedResults("SpecJVM98 " + name, results);
    }

    private static void shootout(String name, Object... inputs) {
        zeeShootoutTests.add(name);
        addExpectedResults("Shootout " + name);
        inputMap.put(name, inputs);
    }

    private static void auto(String name, Expectation... results) {
        addExpectedResults(name, results);
    }

    private static void imageConfig(String name, Object... spec) {
        String[] params = {};
        Expectation[] results = {};
        for (Object o : spec) {
            if (o instanceof String) {
                params = Arrays.copyOf(params, params.length + 1);
                params[params.length - 1] = (String) o;
            } else {
                assert o instanceof Expectation;
                results = Arrays.copyOf(results, results.length + 1);
                results[results.length - 1] = (Expectation) o;
            }
        }

        imageParams.put(name, params);
        if (results.length != 0) {
            configResultMap.put(name, results);
        }
    }

    private static void jtLoadConfig(String name, String... params) {
        jtLoadParams.put(name, params);
    }

    private static void maxvmConfig(String name, String... params) {
        zeeMaxvmConfigs.add(name);
        maxvmParams.put(name, params);
    }

    private static void addExpectedResults(String key, Expectation... results) {
        if (results != null && results.length > 0) {
            resultMap.put(key, results);
        }
    }

    public static String defaultMaxvmOutputConfigs() {
        return "jit";
    }

    public static String defaultJavaTesterConfigs() {
        final Platform platform = Platform.host();
        if (platform.processorKind.processorModel == ProcessorModel.SPARCV9) {
            return "cpscps,cpsjit,jitcps,jitjit";
        }
        return "cpsc1x,cpscps,jitcps,cpsjit,jitjit";
    }

    public static boolean isSupported(String config) {
        Expectation[] expect = configResultMap.get(config);
        if (expect != null) {
            final Platform platform = Platform.host();
            for (Expectation e : expect) {
                if (e.matches(platform)) {
                    if (e.expectedResult == ExpectedResult.PASS) {
                        return true;
                    }
                }
            }
            return false;
        }

        return true;
    }

    public static String[] getImageConfigArgs(String imageConfig) {
        final String[] args = imageParams.get(imageConfig);
        if (args == null) {
            ProgramError.unexpected("unknown image config: " + imageConfig);
        }
        return args;
    }

    public static String[] getMaxvmConfigArgs(String maxvmConfig) {
        final String[] args = maxvmParams.get(maxvmConfig);
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
     * @param config the {@linkplain #zeeMaxvmConfigs maxvm} configuration used during the test execution.
     * This value may be null.
     */
    public static ExpectedResult expectedResult(String testName, String config) {
        final Expectation[] expect = resultMap.get(testName);
        if (expect != null) {
            final Platform platform = Platform.host();
            for (Expectation e : expect) {
                if (e.matches(platform)) {
                    return e.expectedResult;
                }
            }
        }
        return ExpectedResult.PASS;
    }



    static final Set<Class> slowAutoTestClasses = new HashSet<Class>();

    private static void addIfExists(Set<Class> classes, String className) {
        try {
            classes.add(Class.forName(className));
        } catch (ClassNotFoundException e) {
        }
    }

    static {
        addIfExists(slowAutoTestClasses, "test.com.sun.max.vm.cps.CompilerTest_max");
        addIfExists(slowAutoTestClasses, "test.com.sun.max.vm.cps.CompilerTest_coreJava");
        addIfExists(slowAutoTestClasses, "test.com.sun.max.vm.cps.CompilerTest_large");
        addIfExists(slowAutoTestClasses, "test.com.sun.max.vm.cps.JitCompilerTestCase");
        addIfExists(slowAutoTestClasses, "test.com.sun.max.vm.cps.bytecode.BytecodeTest_subtype");
    }

    /**
     * Determines which JUnit test cases are known to take a non-trivial amount of time to execute.
     * These tests are omitted by the MaxineTester unless the
     * @param testCase the test case
     * @return {@code true} if the test is probably slow
     */
    public static boolean isSlowAutoTestCase(TestCase testCase) {
        for (Class<?> c : slowAutoTestClasses) {
            if (c.isAssignableFrom(testCase.getClass())) {
                return true;
            }
        }
        return false;
    }

    public static String[] getVMOptions(String maxvmConfig) {
        if (!maxvmParams.containsKey(maxvmConfig)) {
            ProgramError.unexpected("Unknown Maxine VM option configuration: " + maxvmConfig);
        }
        return maxvmParams.get(maxvmConfig);
    }

    private static class Expectation {
        private final OperatingSystem os; // null indicates all OSs
        private final ProcessorModel processor; // null indicates all processors
        private final ExpectedResult expectedResult;

        Expectation(OperatingSystem os, ProcessorModel pm, ExpectedResult e) {
            this.os = os;
            this.processor = pm;
            expectedResult = e;
        }

        public boolean matches(Platform platform) {
            if (os == null || os == platform.operatingSystem) {
                if (processor == null || processor == platform.processorKind.processorModel) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer();
            buffer.append(os == null ? "ANY" : os.toString());
            buffer.append("/");
            buffer.append(processor == null ? "ANY" : processor.toString());
            buffer.append(" = ");
            buffer.append(expectedResult);
            return buffer.toString();
        }
    }

}

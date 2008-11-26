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

import java.util.*;

import com.sun.max.platform.*;
import com.sun.max.program.*;



public class MaxineTesterConfiguration {

    static final Class[] _outputTestClasses = {
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
//        test.output.GCTest5.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.ZipFileReader.class,
        test.output.FloatNanTest.class,
        test.output.JavacTest.class,
    };

    static Set<String> toClassNames(Class<?>... classes) {
        final Set<String> classNames = new HashSet<String>(classes.length);
        for (Class c : classes) {
            classNames.add(c.getName());
        }
        return classNames;
    }

    static final Set<String> _expectedFailuresSolarisAMD64 = toClassNames(
        test.output.FloatNanTest.class,
        test.output.JavacTest.class
    );

    static final Set<String> _expectedFailuresSolarisSPARCV9 = toClassNames(
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.ZipFileReader.class,
        test.output.FloatNanTest.class,
        test.output.JavacTest.class,
        test.hotpath.HP_series.class// 333
    );

    static final Set<String> _expectedJitFailuresSolarisSPARCV9 = toClassNames(
        test.output.HelloWorld.class,
        test.output.HelloWorldGC.class,
        test.output.SafepointWhileInNative.class,
        test.output.SafepointWhileInJava.class,
        test.output.Recursion.class,
        test.output.StaticInitializers.class,
        test.output.LocalCatch.class,
        test.output.Printf.class,
        test.output.GCTest1.class,
        test.output.GCTest2.class,
        test.output.GCTest3.class,
        test.output.GCTest4.class,
        test.output.GCTest6.class,
        test.output.HelloWorldReflect.class,
        test.output.JREJarLoadTest.class,
        test.output.ZipFileReader.class,
        test.output.FloatNanTest.class,
        test.output.JavacTest.class,
        test.output.FloatNanTest.class,
        test.output.JavacTest.class,
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
        test.reflect.Array_get01.class,
        test.reflect.Array_get02.class,
        test.reflect.Array_get03.class,
        test.reflect.Array_getBoolean01.class,
        test.hotpath.HP_series.class // 333
    );

    static final Set<String> _expectedAutoTestFailures = new HashSet<String>(Arrays.asList(
        "test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.amd64.AMD64EirTranslatorTest_native)"
    ));

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

    public static String defaultMaxvmnOutputConfigs() {
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

    /**
     * Determines if a given test is known to fail.
     *
     * @param testName a unique identifier for the test
     * @param config the {@linkplain #_maxvmConfigs maxvm} configuration used during the test execution. This value may be null.
     */
    public static boolean isExpectedFailure(String testName, String config) {
        final Platform platform = Platform.host();
        if (platform.operatingSystem() == OperatingSystem.SOLARIS) {
            final ProcessorKind processorKind = platform.processorKind();
            if (processorKind.processorModel() == ProcessorModel.AMD64) {
                return _expectedFailuresSolarisAMD64.contains(testName);
            } else if (processorKind.processorModel() == ProcessorModel.SPARCV9) {
                if (config.contains("jit")) {
                    return _expectedJitFailuresSolarisSPARCV9.contains(testName);
                }
                return _expectedFailuresSolarisSPARCV9.contains(testName);
            }
        }
        if (_expectedAutoTestFailures.contains(testName)) {
            return true;
        }
        return false;
    }

    public static String[] getVMOptions(String maxvmConfig) {
        if (!_maxvmConfigs.containsKey(maxvmConfig)) {
            ProgramError.unexpected("Unknown Maxine VM option configuration: " + maxvmConfig);
        }
        return _maxvmConfigs.get(maxvmConfig);
    }
}

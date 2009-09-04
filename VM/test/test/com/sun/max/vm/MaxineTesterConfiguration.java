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

    static final List<Class> zeeOutputTests = new LinkedList<Class>();
    static final List<String> zeeDacapoTests = new LinkedList<String>();
    static final List<String> zeeSpecjvm98Tests = new LinkedList<String>();
    static final List<String> zeeShootoutTests = new LinkedList<String>();
    static final List<String> zeeImageConfigs = new LinkedList<String>();
    static final List<String> zeeMaxvmConfigs = new LinkedList<String>();

    static final Map<String, Expectation[]> resultMap = new HashMap<String, Expectation[]>();
    static final Map<Object, Object[]> inputMap = new HashMap<Object, Object[]>();
    static final Map<String, String[]> imageParams = new HashMap<String, String[]>();
    static final Map<String, String[]> maxvmParams = new HashMap<String, String[]>();

    static {
        output(test.output.AWTFont.class,                  FAIL_SPARC, FAIL_ALL);
        output(test.output.JavacTest.class,                FAIL_SPARC, RAND_LINUX);
        output(test.output.CatchOutOfMemory.class);
        output(test.output.PrintDate.class);
        output(test.output.HelloWorld.class);
        output(test.output.HelloWorldGC.class);
        output(test.output.ExitCode.class);
        output(test.output.FloatNanTest.class);
        output(test.output.GetResource.class,              FAIL_SPARC);
        output(test.output.SafepointWhileInNative.class);
        output(test.output.SafepointWhileInJava.class);
        output(test.output.BlockingQueue.class);
        output(test.output.Recursion.class);
        output(test.output.StaticInitializers.class);
        output(test.output.LocalCatch.class);
        output(test.output.Printf.class);
        output(test.output.GCTest0.class);
        output(test.output.GCTest1.class);
        output(test.output.GCTest2.class);
        output(test.output.GCTest3.class);
        output(test.output.GCTest4.class);
        output(test.output.GCTest5.class);
        output(test.output.GCTest6.class);
        output(test.output.HelloWorldReflect.class);
        output(test.output.JREJarLoadTest.class);
        output(test.output.FileReader.class);
        output(test.output.ZipFileReader.class);
        output(test.output.WeakReferenceTest01.class);
        output(test.output.WeakReferenceTest02.class);
        output(test.output.WeakReferenceTest03.class);
        output(test.output.WeakReferenceTest04.class);
        output(test.output.MegaThreads.class,             RAND_SPARC);

        jtt(jtt.threads.Thread_isInterrupted02.class,                  FAIL_LINUX);
        jtt(jtt.jdk.EnumMap01.class,                                   RAND_ALL);
        jtt(jtt.jdk.EnumMap02.class,                                   RAND_ALL);
        jtt(jtt.hotpath.HP_series.class,                  RAND_SPARC);  // Fails:                   @jitopt, @optopt
        jtt(jtt.hotpath.HP_array02.class,                 RAND_SPARC);  // Fails:                   @jitopt, @optopt
        jtt(jtt.except.Catch_StackOverflowError_01.class, RAND_SPARC);  // Fails: @jitjit, @optjit, @jitopt
        jtt(jtt.except.Catch_StackOverflowError_02.class, RAND_SPARC);  // Fails: @jitjit, @optjit, @jitopt
        jtt(jtt.except.Catch_StackOverflowError_03.class, RAND_SPARC);  // Fails: @jitjit, @optjit
        jtt(jtt.lang.ClassLoader_loadClass01.class,       RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.lang.Class_asSubclass01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.lang.Object_clone01.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.lang.Object_notify01.class,               RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.lang.Object_notifyAll01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.lang.Object_wait01.class,                 RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_get01.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_get02.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_get03.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getBoolean01.class,         RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getByte01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getChar01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getDouble01.class,          RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getFloat01.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getInt01.class,             RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getLength01.class,          RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getLong01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_getShort01.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_set01.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_set02.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_set03.class,                RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setBoolean01.class,         RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setByte01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setChar01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setDouble01.class,          RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setFloat01.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setInt01.class,             RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setLong01.class,            RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Array_setShort01.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getDeclaredField01.class,   RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getDeclaredMethod01.class,  RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getField01.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getField02.class,           RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getMethod01.class,          RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_getMethod02.class,          RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_newInstance02.class,        RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_newInstance03.class,        RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_newInstance06.class,        RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.reflect.Class_newInstance07.class,        RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.threads.Thread_holdsLock01.class,         RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.threads.Thread_setPriority01.class,       RAND_SPARC);  // Fails: @jitjit,          @jitopt
        jtt(jtt.bytecode.BC_iadd2.class,                  RAND_SPARC);  // Fails:          @optjit
        jtt(jtt.bytecode.BC_iadd3.class,                  RAND_SPARC);  // Fails:          @optjit
        jtt(jtt.bytecode.BC_wide01.class,                 RAND_SPARC);  // Fails:          @optjit
        jtt(jtt.bytecode.BC_wide02.class,                 RAND_SPARC);  // Fails:          @optjit
        jtt(jtt.bytecode.BC_athrow.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_aaload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_aastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_anewarray.class,                RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_arraylength.class,              RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_athrow.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_athrow1.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_athrow2.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_athrow3.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_baload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_bastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_caload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_castore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_checkcast.class,                RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_checkcast1.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_checkcast2.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_daload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_dastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_fastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_getfield.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_iaload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_iastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_idiv.class,                     RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_invokevirtual01.class,          RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_irem.class,                     RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_laload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_lastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_ldiv.class,                     RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_lrem.class,                     RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_monitorenter.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_multianewarray.class,           RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_newarray.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_putfield.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_saload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_sastore.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_faload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_faload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_faload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_faload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.BC_faload.class,                   RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_InCatch01.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_InCatch02.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_InCatch03.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_NPE_01.class,                RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_Synchronized01.class,        RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_Synchronized02.class,        RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_Synchronized03.class,        RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_Synchronized04.class,        RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.except.Throw_Synchronized05.class,        RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Class_cast01.class,                  RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Class_forName01.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Class_forName02.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Class_forName03.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Class_forName04.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.lang.Object_toString02.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_String01.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_boolean01.class,            RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_byte01.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_char01.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_float01.class,              RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_double01.class,             RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_int01.class,                RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_long01.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.micro.VarArgs_short01.class,              RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.optimize.Fold_Cast01.class,               RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.optimize.VN_Cast01.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.optimize.VN_Cast02.class,                 RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.optimize.VN_Field01.class,                RAND_SPARC);  // Fails:                   @jitopt
        jtt(jtt.optimize.VN_Field02.class,                RAND_SPARC);  // Fails:                   @jitopt

        dacapo("antlr",              FAIL_SPARC);
        dacapo("bloat",              FAIL_SPARC);
        dacapo("xalan",              FAIL_SPARC);
        dacapo("hsqldb",   FAIL_ALL, FAIL_SPARC);
        dacapo("luindex",            FAIL_SPARC);
        dacapo("lusearch",           FAIL_SPARC);
        dacapo("jython",             FAIL_SPARC);
        dacapo("chart",    FAIL_ALL, FAIL_SPARC);
        dacapo("eclipse",  FAIL_ALL, FAIL_SPARC);
        dacapo("fop",                FAIL_SPARC);
        dacapo("pmd",                FAIL_SPARC);

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

        auto("test_manyObjectParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",   FAIL_ALL);
        auto("test_arrayCopyForKinds(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)",  FAIL_ALL);
        auto("test_catchNull(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_throw)",               FAIL_ALL);
        auto("test_manyParameters(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",         FAIL_ALL);
        auto("test_nop(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",                    FAIL_ALL);
        auto("test_nop_cfunction(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",          FAIL_ALL);
        auto("test_reference_identity(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_native)",     FAIL_ALL);
        auto("test_sameNullsArrayCopy(test.com.sun.max.vm.compiler.eir.sparc.SPARCEirTranslatorTest_jdk_System)", FAIL_ALL);

        imageConfig("optopt", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests");
        imageConfig("optjit", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-callee-jit");
        imageConfig("jitopt", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-caller-jit");
        imageConfig("jitjit", "-run=test.com.sun.max.vm.jtrun.all", "-native-tests", "-test-caller-jit", "-test-callee-jit");
        imageConfig("optc1x", "-run=test.com.sun.max.vm.jtrun.c1x", "-native-tests", "-test-callee-c1x");
        imageConfig("java", "-run=com.sun.max.vm.run.java");

        maxvmConfig("std");
        maxvmConfig("jit", "-Xjit");
        maxvmConfig("pgi", "-XX:+PGI");
        maxvmConfig("mx256m", "-Xmx256m");
        maxvmConfig("mx512m", "-Xmx512m");

    }

    private static void output(Class javaClass, Expectation... results) {
        zeeOutputTests.add(javaClass);
        addExpectedResults(javaClass.getName(), results);
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

    private static void imageConfig(String name, String... params) {
        zeeImageConfigs.add(name);
        imageParams.put(name, params);
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
        return "std,jit";
    }

    public static String defaultJavaTesterConfigs() {
        final Platform platform = Platform.host();
        if (platform.processorKind.processorModel == ProcessorModel.SPARCV9) {
            return "optopt,optjit";
        }
        return "optopt,jitopt,optjit,jitjit";
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


    static final Set<Class> slowAutoTestClasses = new HashSet<Class>(Arrays.asList((Class)
                    CompilerTest_max.class,
                    CompilerTest_coreJava.class,
                    CompilerTest_large.class,
                    JitCompilerTestCase.class,
                    BytecodeTest_subtype.class));

    /**
     * Determines which JUnit test cases are known to take a non-trivial amount of time to execute.
     * These tests are omitted by the MaxineTester unless the
     * @param testCase the test case
     * @return <code>true</code> if the test is probably slow
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

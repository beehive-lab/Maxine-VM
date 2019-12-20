/*
 * Copyright (c) 2017-2019, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.oracle.max.vm.ext.t1x.aarch64;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.tests.crossisa.aarch64.asm.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.platform.*;
import com.sun.max.program.option.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * These JUnit tests are for testing protected members of Aarch64T1XCompilation which
 * are not visible from the test.armv7.t1x test package suite.
 *
 */

public class Aarch64T1XpTest extends MaxTestCase {
    private Aarch64Assembler      asm;
    private CiTarget              aarch64;
    private Aarch64CodeWriter     code;
    private T1X                   t1x;
    private C1X                   c1x;
    private Aarch64T1XCompilation theCompiler;
    private StaticMethodActor anMethod = null;
    private CodeAttribute codeAttr = null;
    private static boolean POST_CLEAN_FILES = true;

    public void initialiseFrameForCompilation() {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, new byte[15], (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("(Ljava/util/Map;)V"), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), Actor.JAVA_METHOD_FLAGS, codeAttr, new String());
    }

    public void initialiseFrameForCompilation(byte[] code, String sig, int flags) {
        // TODO: compute max stack
        codeAttr = new CodeAttribute(null, code, (char) 40, (char) 20, CodeAttribute.NO_EXCEPTION_HANDLER_TABLE, LineNumberTable.EMPTY, LocalVariableTable.EMPTY, null);
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create(sig), flags, codeAttr, new String());
    }

    static final class Pair {

        public final int first;
        public final int second;

        Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    private static final OptionSet options = new OptionSet(false);
    private static VMConfigurator vmConfigurator = null;
    private static boolean initialised = false;

    private static String[] expandArguments(String[] args) throws IOException {
        List<String> result = new ArrayList<String>(args.length);
        for (String arg : args) {
            if (arg.charAt(0) == '@') {
                File file = new File(arg.substring(1));
                result.addAll(Files.readLines(file));
            } else {
                result.add(arg);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static int[] valueTestSet = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static long[] scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x00000000ffffffffL};
    private static MaxineAarch64Tester.BitsFlag[] bitmasks = new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
    static {
        MaxineAarch64Tester.setAllBitMasks(bitmasks, MaxineAarch64Tester.BitsFlag.All64Bits);
    }
    private static boolean[] testValues = new boolean[MaxineAarch64Tester.NUM_REGS];

    private static void setIgnoreValue(int i, boolean value, boolean all) {
        testValues[i] = value;
    }

    private static void resetIgnoreValues() {
        for (int i = 0; i < testValues.length; i++) {
            testValues[i] = false;
        }
    }

    // The following values will be updated
    // to those expected to be found in a register after simulated execution of code.
    private static long[] expectedValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private static long[] expectedLongValues = {Long.MAX_VALUE - 100, Long.MAX_VALUE};

    private static void initialiseExpectedValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            expectedValues[i] = i;
        }
    }

    private static void initialiseTestValues() {
        for (int i = 0; i < MaxineAarch64Tester.NUM_REGS; i++) {
            testValues[i] = false;
        }
    }

    private long[] generateAndTest(long[] expected, boolean[] tests, MaxineAarch64Tester.BitsFlag[] masks) throws Exception {
        Aarch64CodeWriter code = new Aarch64CodeWriter(theCompiler.getMacroAssembler().codeBuffer);
        code.createCodeFile();
        MaxineAarch64Tester r = new MaxineAarch64Tester(expected, tests, masks);
        r.cleanFiles();
        r.cleanProcesses();
        r.compile();
        r.link();
        r.runSimulation();
        r.reset();
        return r.getSimulatedLongRegisters();
    }

    public Aarch64T1XpTest() {
        try {
            String[] args = new String[2];
            args[0] = new String("t1xp");
            args[1] = new String("HelloWorld");
            if (options != null) {
                options.parseArguments(args);
            }
            if (vmConfigurator == null) {
                vmConfigurator = new VMConfigurator(options);
            }
            String baselineCompilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            String optimizingCompilerName = new String("com.oracle.max.vm.ext.c1x.C1X");
            //String optimizingCompilerName = new String("com.oracle.max.vm.ext.graal.MaxGraal");

            RuntimeCompiler.baselineCompilerOption.setValue(baselineCompilerName);
            RuntimeCompiler.optimizingCompilerOption.setValue(optimizingCompilerName);
            if (!initialised) {
                Platform.set(Platform.parse("linux-aarch64"));
                vmConfigurator.create();
                vm().compilationBroker.setOffline(true);
                JavaPrototype.initialize(false);
                initialised = true;
            }
            t1x = (T1X) CompilationBroker.addCompiler("t1x", baselineCompilerName);
            c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            //graal = (MaxGraal)CompilationBroker.addCompiler("Graal", optimizingCompilerName);
            //graal.initialize(Phase.HOSTED_COMPILING);
            c1x.initialize(Phase.HOSTED_COMPILING);
            theCompiler = (Aarch64T1XCompilation) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void test_AssignWordReg() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3};

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = 12345678987654321L;
        expectedValues[3] = 1;

        for (int i = 0; i < 4; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.assignWordReg(regs[i], Aarch64.r16);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_AssignDouble() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] dregs = {Aarch64.d0, Aarch64.d1, Aarch64.d2, Aarch64.d3, Aarch64.d4};
        CiRegister [] lregs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        double [] dValues = {Double.MAX_VALUE, Double.MIN_VALUE, 1.0, 0.12345, -1.345E56};


        for (int  i = 0; i < 5; i++) {
            expectedValues[i] = Double.doubleToRawLongBits(dValues[i]);
            theCompiler.assignDouble(dregs[i], dValues[i]);
            masm.fmovFpu2Cpu(64, lregs[i], dregs[i]);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            Double d = Double.longBitsToDouble(simulatedValues[i]);
            assert d == dValues[i]
                            : i + "; Simulated: " + d + ", expected: " + dValues[i];
        }
    }

    public void test_AssignInt() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] iregs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        int [] values = {Integer.MAX_VALUE, Integer.MIN_VALUE, 1, 123456789, -123456789, 0};


        for (int  i = 0; i < 5; i++) {
            expectedValues[i] = values[i];
            theCompiler.assignInt(iregs[i], values[i]);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert (int) simulatedValues[i] == values[i]
                            : i + "; Simulated: " + (int) simulatedValues[i] + ", expected: " + values[i];
        }
    }

    public void test_AssignLong() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] lregs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        long [] values = {Long.MAX_VALUE, Long.MIN_VALUE, 1, 12345678987654321L, -12345678987654321L, 0};


        for (int  i = 0; i < 5; i++) {
            expectedValues[i] = values[i];
            theCompiler.assignLong(lregs[i], values[i]);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert simulatedValues[i] == values[i]
                            : i + "; Simulated: " + simulatedValues[i] + ", expected: " + values[i];
        }
    }

    public void test_AssignFloat() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister[] dregs = {Aarch64.d0, Aarch64.d1, Aarch64.d2, Aarch64.d3, Aarch64.d4};
        CiRegister[] iregs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        float[] fValues = {Float.MAX_VALUE, Float.MIN_VALUE, 1.0f, 0.12345f, -1.345E36f};


        for (int i = 0; i < 5; i++) {
            expectedValues[i] = Float.floatToRawIntBits(fValues[i]);
            theCompiler.assignFloat(dregs[i], fValues[i]);
            masm.fmovFpu2Cpu(32, iregs[i], dregs[i]);
        }

        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            Float f = Float.intBitsToFloat((int) simulatedValues[i]);
            assert f == fValues[i]
                    : i + "; Simulated: " + f + ", expected: " + fValues[i];
        }
    }

    public void fails_do_swap() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = -1;
        int v2 = 0x2;

        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_swap();

        expectedValues[0] = v2;
        expectedValues[1] = v1;

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);


        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 2; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == (0xffffffffL & expectedValues[i])
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    public void test_do_dup2_x2_4() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        long v1 = -1L;
        long v2 = 0x2fffffffffffffffL;

        theCompiler.do_lconst(v2);
        theCompiler.do_lconst(v1);
        theCompiler.do_dup2_x2();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v1;

        theCompiler.peekLong(Aarch64.r0, 0);
        theCompiler.peekLong(Aarch64.r1, 2);
        theCompiler.peekLong(Aarch64.r2, 4);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 3; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }
    // category 1/1/2
    public void test_do_dup2_x2_3() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = 234567890;
        int v2 = 123456789;
        long v3 = -1L;

        theCompiler.do_lconst(v3);
        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup2_x2();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v3;
        expectedValues[3] = v1;
        expectedValues[4] = v2;

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekLong(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 4);
        theCompiler.peekInt(Aarch64.r4, 5);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    // category 2/1/1
    public void test_do_dup2_x2_2() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v3 = 234567890;
        int v2 = 123456789;
        long v1 = -1L;

        theCompiler.do_iconst(v3);
        theCompiler.do_iconst(v2);
        theCompiler.do_lconst(v1);
        theCompiler.do_dup2_x2();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v3;
        expectedValues[3] = v1;

        theCompiler.peekLong(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 2);
        theCompiler.peekInt(Aarch64.r2, 3);
        theCompiler.peekLong(Aarch64.r3, 4);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }
    // all category 1 types
    public void test_do_dup2_x2() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = 1234;
        int v2 = 2345;
        int v3 = 3456;
        int v4 = 4567;

        theCompiler.do_iconst(v4);
        theCompiler.do_iconst(v3);
        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup2_x2();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v3;
        expectedValues[3] = v4;
        expectedValues[4] = v1;
        expectedValues[5] = v2;

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);
        theCompiler.peekInt(Aarch64.r4, 4);
        theCompiler.peekInt(Aarch64.r5, 5);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 6; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    // category 2 and 1 computational types
    public void test_do_dup2_x1_2() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v2 = 123456789;
        long v1 = -1L;

        theCompiler.do_iconst(v2);
        theCompiler.do_lconst(v1);
        theCompiler.do_dup2_x1();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v1;

        theCompiler.peekLong(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 2);
        theCompiler.peekLong(Aarch64.r2, 3);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 3; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }

    }


    // category 1 computational types.
    public void test_do_dup2_x1() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = 1234;
        int v2 = 2345;
        int v3 = 3456;

        theCompiler.do_iconst(v3);
        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup2_x1();

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v3;
        expectedValues[3] = v1;
        expectedValues[4] = v2;

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);
        theCompiler.peekInt(Aarch64.r4, 4);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }


    }

    // category 2 & 1 computational type test.
    public void test_do_dup_x2_2() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = 123456789;
        long v2 = -1L;

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v1;

        theCompiler.do_lconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup_x2();

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekLong(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 3);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);


        for (int i = 0; i < 3; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    // category 1 test
    public void fails_do_dup_x2_1() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int [] v = {-123456789, 123456789, 987654321};
        CiRegister [] reg = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3};

        theCompiler.do_iconst(v[0]);
        theCompiler.do_iconst(v[1]);
        theCompiler.do_iconst(v[2]);

        expectedValues[0] = v[2];
        expectedValues[1] = v[1];
        expectedValues[2] = v[0];
        expectedValues[3] = v[2];
        theCompiler.do_dup_x2();

        for (int i = 0; i < reg.length; i++) {
            theCompiler.peekInt(reg[i], i);
        }
        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            System.out.println(i + " " + simValues[i]);
            assert simValues[i] == (expectedValues[i] & 0xffffffffL)
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    public void fails_do_dup_x1() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        int v1 = -123456778;
        int v2 = 123456789;

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v1;

        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup_x1();

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 3; i++) {
            assert simValues[i] == (0xffffffffL & expectedValues[i])
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }

    }

    // category 2 type test
    public void test_do_dup2_2() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        long testVal = 0xaaaabbbbccccddddL;


        expectedValues[0] = testVal;
        expectedValues[1] = testVal;


        theCompiler.do_lconst(testVal);
        theCompiler.do_dup2();
        theCompiler.peekLong(Aarch64.r0, 0);
        theCompiler.peekLong(Aarch64.r1, 2);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 2; i++) {
            assert simValues[i] == expectedValues[i]
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }

    }

    // category 1 type test
    public void fails_do_dup2_1() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        int v1 = -12345678;
        int v2 = 12345678;

        expectedValues[0] = v1;
        expectedValues[1] = v2;
        expectedValues[2] = v1;
        expectedValues[3] = v2;


        theCompiler.do_iconst(v2);
        theCompiler.do_iconst(v1);
        theCompiler.do_dup2();
        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);


        for (int i = 0; i < 4; i++) {
            System.out.println(i + " " + Long.toHexString(simValues[i]));
            assert simValues[i] == (expectedValues[i] & 0xffffffffL)
                            : i + " expected " + expectedValues[i] + ", got " + simValues[i];
        }
    }

    public void test_do_dup() throws Exception {
        int testVal = (int) Math.random();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov32BitConstant(Aarch64.r16, testVal);
        theCompiler.incStack(1);

        expectedValues[0] = testVal;
        expectedValues[1] = testVal;
        theCompiler.pokeInt(Aarch64.r16, testVal);
        theCompiler.do_dup();
        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        assert simValues[0] == expectedValues[0];
        assert simValues[1] == expectedValues[1];
    }
    public void test_do_dconst() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        double[] values = {Double.MAX_VALUE, Double.MIN_VALUE, 0.0d, 1.1d, 1.234e100, -1.234e100};
        CiRegister[] regs = {Aarch64.r5, Aarch64.r4, Aarch64.r3, Aarch64.r2, Aarch64.r1, Aarch64.r0};

        for (int i = 0; i < values.length; i++) {
            expectedValues[i] = Double.doubleToRawLongBits(values[i]);
            theCompiler.do_dconst(values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            theCompiler.peekLong(regs[i], 0);
            theCompiler.decStack(2);
        }

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < values.length; i++) {
            assert values[i] == Double.longBitsToDouble(simValues[i])
                : i + "; Simulated: " + Double.longBitsToDouble(simValues[i]) + ", expected: " + values[i];
        }
    }

    public void test_do_lconst() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        Long[] values = {Long.MAX_VALUE, Long.MIN_VALUE, 0L, -1000L, 12345678987654321L, -12345678987654321L};
        CiRegister[] regs = {Aarch64.r5, Aarch64.r4, Aarch64.r3, Aarch64.r2, Aarch64.r1, Aarch64.r0};

        for (int i = 0; i < values.length; i++) {
            expectedValues[i] = values[i];
            theCompiler.do_lconst(values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            theCompiler.peekLong(regs[i], 0);
            theCompiler.decStack(2);
        }

        long [] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < values.length; i++) {
            System.out.println(i + " " + simValues[i]);
            assert expectedValues[i] == simValues[i]
                : i + "; Simulated: " + simValues[i] + ", expected: " + expectedValues[i];
        }
    }
    public void test_do_fconst() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        float[] values = {Float.MAX_VALUE, Float.MIN_VALUE, 0.0f, 1.1f, 123.456789f, -123.456789f};
        CiRegister[] regs = {Aarch64.r5, Aarch64.r4, Aarch64.r3, Aarch64.r2, Aarch64.r1, Aarch64.r0};

        for (int i = 0; i < values.length; i++) {
            expectedValues[i] = Float.floatToRawIntBits(values[i]);
            theCompiler.do_fconst(values[i]);
        }

        for (int i = 0; i < values.length; i++) {
            theCompiler.peekInt(regs[i], i);
        }

        long[] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < values.length; i++) {
            assert values[i] == Float.intBitsToFloat((int) simValues[i])
                : i + "; Simulated: " + Float.intBitsToFloat((int) simValues[i]) + ", expected: " + values[i];
        }
    }
    public void test_do_iconst() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        int[] values = {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 1, 123456789, -123456789};
        CiRegister[] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4, Aarch64.r5};

        for (int i = 0; i < values.length; i++) {
            expectedValues[i] = values[i];
            theCompiler.do_iconst(values[i]);
            theCompiler.peekInt(regs[i], 0);
        }

        long[] simValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < values.length; i++) {
            assert values[i] == (int) simValues[i]
                : i + "; Simulated: " + simValues[i] + ", expected: " + values[i];
        }
    }

    public void test_LoadStoreObject() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2};

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = 12345678987654321L;
        expectedValues[2] = 1;

        for (int i = 0; i < 3; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.storeObject(Aarch64.r16, i);
            theCompiler.loadObject(regs[i], i);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 3; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }
    public void test_LoadStoreWord() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2};

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = 12345678987654321L;
        expectedValues[2] = 1;

        for (int i = 0; i < 3; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.storeWord(Aarch64.r16, i);
            theCompiler.loadWord(regs[i], i);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 3; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }
    public void test_LoadStoreLong() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -12345678987654321L;
        expectedValues[3] = 12345678987654321L;
        expectedValues[4] = -1;

        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.storeLong(Aarch64.r16, i);
            theCompiler.loadLong(regs[i], i);
        }

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_bcond() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 1);
        masm.mov32BitConstant(Aarch64.r1, 1);
        masm.cmp(32, Aarch64.r0, Aarch64.r1);
        masm.b(Aarch64Assembler.ConditionFlag.EQ, 4);
        masm.mov32BitConstant(Aarch64.r3, 20);
        masm.mov32BitConstant(Aarch64.r4, 10);
        masm.nop();
        expectedValues[3] = 20;
        testValues[3] = true;

        long [] sim = generateAndTest(expectedValues, testValues, bitmasks);
        assert expectedValues[3] == sim[3];


    }

    public void test_LoadStoreInt() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        CiRegister [] regs = {Aarch64.r0, Aarch64.r1, Aarch64.r2, Aarch64.r3, Aarch64.r4};

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 123456789;
        expectedValues[4] = -1;

        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            theCompiler.storeInt(Aarch64.r16, i);
            //theCompiler.loadInt(regs[i], i);
        }

        for (int i = 0; i < 5; i++) {
            theCompiler.loadInt(regs[i], i);
        }
        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                            : "Register " + i + " " + (int) simulatedValues[i] + " expected " + (int) expectedValues[i];
        }

    }

    public void test_emitPrologueTests() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.initFrame(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
    }

    public void do_nothing() {
        return;
    }

    public void test_iinc() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        byte[] instructions = new byte[5];
        int i = 0;
        instructions[i++] = (byte) Bytecodes.ICONST_0;
        instructions[i++] = (byte) Bytecodes.ISTORE_1;
        instructions[i++] = (byte) Bytecodes.IINC;
        instructions[i++] = (byte) 1;
        instructions[i++] = (byte) 1;

        initialiseFrameForCompilation(instructions, "(II)I");
        theCompiler.offlineT1XCompile(anMethod, codeAttr, instructions, 5);
        masm.pop(32, Aarch64.r0);
        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
    }

}

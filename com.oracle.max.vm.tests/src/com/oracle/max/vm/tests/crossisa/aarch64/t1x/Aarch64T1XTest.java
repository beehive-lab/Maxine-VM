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
package com.oracle.max.vm.tests.crossisa.aarch64.t1x;

import static com.sun.max.vm.MaxineVM.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.target.aarch64.*;
import com.oracle.max.vm.ext.c1x.*;
import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.t1x.aarch64.*;
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

public class Aarch64T1XTest extends MaxTestCase {

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
        anMethod = new StaticMethodActor(null, SignatureDescriptor.create("()V"), Actor.ACC_STATIC, codeAttr, new String());
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

    private static int[]                          valueTestSet   = {0, 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65535};
    private static long[]                         scratchTestSet = {0, 1, 0xff, 0xffff, 0xffffff, 0xfffffff, 0x00000000ffffffffL};
    private static MaxineAarch64Tester.BitsFlag[] bitmasks       = new MaxineAarch64Tester.BitsFlag[MaxineAarch64Tester.NUM_REGS];
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

    public Aarch64T1XTest() {
        try {
            String[] args = new String[2];
            args[0] = new String("t1x");
            args[1] = new String("HelloWorld");
            if (options != null) {
                options.parseArguments(args);
            }
            if (vmConfigurator == null) {
                vmConfigurator = new VMConfigurator(options);
            }
            String baselineCompilerName = new String("com.oracle.max.vm.ext.t1x.T1X");
            String optimizingCompilerName = new String("com.oracle.max.vm.ext.c1x.C1X");
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
            // c1x = (C1X) CompilationBroker.addCompiler("c1x", optimizingCompilerName);
            // c1x.initializeOffline(Phase.HOSTED_COMPILING);
            theCompiler = (Aarch64T1XCompilation) t1x.getT1XCompilation();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Aarch64T1XTest.class);
    }

    public void test_DecStack() throws Exception {
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        theCompiler.incStack(3);
        masm.mov(64, Aarch64.r0, Aarch64.sp); // copy stack value into r0
        theCompiler.decStack(1);
        masm.mov(64, Aarch64.r1, Aarch64.sp); // copy stack value onto r1
        theCompiler.decStack(2);
        masm.mov(64, Aarch64.r2, Aarch64.sp);

        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[1] - simulatedValues[0]) == (simulatedValues[2] - simulatedValues[1]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void test_IncStack() throws Exception {
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.mov(64, Aarch64.r0, Aarch64.sp); // copy stack value into r0
        theCompiler.incStack(1);
        masm.mov(64, Aarch64.r1, Aarch64.sp); // copy stack value onto r1
        theCompiler.incStack(2);
        masm.mov(64, Aarch64.r2, Aarch64.sp);

        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 16; i++) {
            assert 2 * (simulatedValues[0] - simulatedValues[1]) == (simulatedValues[1] - simulatedValues[2]) : "Register " + i + " Value " + simulatedValues[i];
        }
    }

    public void test_AdjustReg() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        masm.mov32BitConstant(Aarch64.r0, 0);
        masm.mov32BitConstant(Aarch64.r1, Integer.MAX_VALUE);
        masm.mov32BitConstant(Aarch64.r2, Integer.MIN_VALUE);
        masm.mov32BitConstant(Aarch64.r3, 0);
        masm.mov32BitConstant(Aarch64.r4, Integer.MAX_VALUE);
        masm.mov32BitConstant(Aarch64.r5, 0);

        masm.increment32(Aarch64.r0, 1);
        masm.increment32(Aarch64.r1, 1);
        masm.increment32(Aarch64.r2, -1);
        masm.increment32(Aarch64.r3, Integer.MAX_VALUE);
        masm.increment32(Aarch64.r4, Integer.MIN_VALUE);
        masm.increment32(Aarch64.r5, 0);

        expectedValues[0] = 1;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = Integer.MAX_VALUE;
        expectedValues[3] = Integer.MAX_VALUE;
        expectedValues[4] = -1;
        expectedValues[5] = 0;
        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 6; i++) {
            System.out.println("Register " + i + " " + (int) simulatedValues[i] + " expected " + (int) expectedValues[i]);
        }

        for (int i = 0; i < 6; i++) {
            assert (int) simulatedValues[i] == (int) expectedValues[i]
            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PokeInt() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;
        theCompiler.incStack(4);
        for (int i = 0; i < 5; i++) {
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            theCompiler.pokeInt(Aarch64.r16, i);
        }

        theCompiler.peekInt(Aarch64.r0, 0);
        theCompiler.peekInt(Aarch64.r1, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r3, 3);
        theCompiler.peekInt(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_AssignLong() throws Exception {
        long[] expectedLongValues = new long[5];
        expectedLongValues[0] = Long.MIN_VALUE;
        expectedLongValues[1] = Long.MAX_VALUE;
        expectedLongValues[2] = 0xabdef01023456789L;
        expectedLongValues[3] = 111;
        expectedLongValues[4] = 0;

        for (int i = 0; i < 5; i++) {
            theCompiler.assignmentTests(Aarch64.cpuRegisters[i], expectedLongValues[i]);
        }

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 5; i++) {
            assert registerValues[i] == expectedLongValues[i] :
                "Failed incorrect value " + Long.toString(registerValues[i], 16) + " "
                            + Long.toString(expectedLongValues[i], 16);
        }
    }

    public void test_PeekLong() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[2] = Long.MIN_VALUE;
        expectedValues[4] = -12345678987654321L;
        expectedValues[6] = 12345678987654321L;
        expectedValues[8] = 1;

        for (int i = 0; i < 10; i += 2) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(Aarch64.r16);
            masm.push(Aarch64.zr);
        }

        theCompiler.peekLong(Aarch64.r8, 0);
        theCompiler.peekLong(Aarch64.r6, 2);
        theCompiler.peekLong(Aarch64.r4, 4);
        theCompiler.peekLong(Aarch64.r2, 6);
        theCompiler.peekLong(Aarch64.r0, 8);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 10; i += 2) {
            System.out.println(i + " sim: " + simulatedValues[i] + ", exp: " + expectedValues[i]);
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PokeLong() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -12345678987654321L;
        expectedValues[3] = 0;
        expectedValues[4] = 12345678987654321L;
        theCompiler.incStack(8);
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.pokeLong(Aarch64.r16, i);
        }

        theCompiler.peekLong(Aarch64.r0, 0);
        theCompiler.peekLong(Aarch64.r1, 1);
        theCompiler.peekLong(Aarch64.r2, 2);
        theCompiler.peekLong(Aarch64.r3, 3);
        theCompiler.peekLong(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PeekInt() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Integer.MAX_VALUE;
        expectedValues[1] = Integer.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 123456789;
        expectedValues[4] = 1;

        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            masm.push(32, Aarch64.r16);
        }

        theCompiler.peekInt(Aarch64.r4, 0);
        theCompiler.peekInt(Aarch64.r3, 1);
        theCompiler.peekInt(Aarch64.r2, 2);
        theCompiler.peekInt(Aarch64.r1, 3);
        theCompiler.peekInt(Aarch64.r0, 4);


        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println("i " + (int) simulatedValues[i]);
        }
        for (int i = 0; i < 5; i++) {
            assert (int) expectedValues[i] == (int) simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void test_PeekFloat() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Float.floatToRawIntBits(Float.MAX_VALUE);
        expectedValues[1] = Float.floatToRawIntBits(Float.MIN_VALUE);
        expectedValues[2] = Float.floatToRawIntBits(0.0f);
        expectedValues[3] = Float.floatToRawIntBits(-1.0F);
        expectedValues[4] = Float.floatToRawIntBits(-123.89F);

        theCompiler.incStack(5);
        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov32BitConstant(Aarch64.r16, (int) expectedValues[i]);
            masm.fmovCpu2Fpu(32, Aarch64.d16, Aarch64.r16);
            theCompiler.pokeFloat(Aarch64.d16, i);
        }

        theCompiler.peekFloat(Aarch64.d4, 4);
        theCompiler.peekFloat(Aarch64.d3, 3);
        theCompiler.peekFloat(Aarch64.d2, 2);
        theCompiler.peekFloat(Aarch64.d1, 1);
        theCompiler.peekFloat(Aarch64.d0, 0);

        masm.fmovFpu2Cpu(32, Aarch64.r4, Aarch64.d4);
        masm.fmovFpu2Cpu(32, Aarch64.r3, Aarch64.d3);
        masm.fmovFpu2Cpu(32, Aarch64.r2, Aarch64.d2);
        masm.fmovFpu2Cpu(32, Aarch64.r1, Aarch64.d1);
        masm.fmovFpu2Cpu(32, Aarch64.r0, Aarch64.d0);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            System.out.println(i + " sim: " + Float.intBitsToFloat((int) simulatedValues[i]) + ", exp: " + Float.intBitsToFloat((int) expectedValues[i]));
            assert Float.intBitsToFloat((int) simulatedValues[i]) == Float.intBitsToFloat((int) expectedValues[i])
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void ignore_PokeFloat() throws Exception {
        /* not used - test incorporated in test_PeekFloat */
    }

    public void test_AssignDouble() throws Exception {
        long[] expectedLongValues = new long[5];
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();

        expectedLongValues[0] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedLongValues[1] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedLongValues[2] = Double.doubleToRawLongBits(0.0);
        expectedLongValues[3] = Double.doubleToRawLongBits(-1.0);
        expectedLongValues[4] = Double.doubleToRawLongBits(-100.75);

        for (int i = 0; i < 5; i++) {
            theCompiler.assignDoubleTest(Aarch64.fpuRegisters[i], Double.longBitsToDouble(expectedLongValues[i]));
        }

        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.cpuRegisters[i], -25);
        }

        masm.fmov(64, Aarch64.r0, Aarch64.d0);
        masm.fmov(64, Aarch64.r1, Aarch64.d1);
        masm.fmov(64, Aarch64.r2, Aarch64.d2);
        masm.fmov(64, Aarch64.r3, Aarch64.d3);
        masm.fmov(64, Aarch64.r4, Aarch64.d4);

        long[] returnValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i < 5; i++) {
                assert returnValues[i] == expectedLongValues[i];
        }
    }


    public void ignore_PokeDouble() throws Exception {
        /* not used - test incorporated in test_PeekDouble */
    }

    public void test_PeekDouble() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Double.doubleToRawLongBits(Double.MAX_VALUE);
        expectedValues[2] = Double.doubleToRawLongBits(Double.MIN_VALUE);
        expectedValues[4] = Double.doubleToRawLongBits(0.0);
        expectedValues[6] = Double.doubleToRawLongBits(-1.0);
        expectedValues[8] = Double.doubleToRawLongBits(-123.89);

        theCompiler.incStack(10);
        for (int i = 0; i < 10; i += 2) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.fmovCpu2Fpu(64, Aarch64.d16, Aarch64.r16);
            theCompiler.pokeDouble(Aarch64.d16, i);
        }

        theCompiler.peekDouble(Aarch64.d8, 8);
        theCompiler.peekDouble(Aarch64.d6, 6);
        theCompiler.peekDouble(Aarch64.d4, 4);
        theCompiler.peekDouble(Aarch64.d2, 2);
        theCompiler.peekDouble(Aarch64.d0, 0);

        masm.fmovFpu2Cpu(64, Aarch64.r8, Aarch64.d8);
        masm.fmovFpu2Cpu(64, Aarch64.r6, Aarch64.d6);
        masm.fmovFpu2Cpu(64, Aarch64.r4, Aarch64.d4);
        masm.fmovFpu2Cpu(64, Aarch64.r2, Aarch64.d2);
        masm.fmovFpu2Cpu(64, Aarch64.r0, Aarch64.d0);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 10; i += 2) {
            System.out.println(i + " sim: " + simulatedValues[i] + ", exp: " + expectedValues[i] + " dbl: " + Double.longBitsToDouble(expectedValues[i]));
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_DoLconst() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();

        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        masm.mov(64, Aarch64.r2, Aarch64.sp); // copy stack pointer to r2
        theCompiler.do_lconstTests(0xffffffff0000ffffL);
        masm.mov(64, Aarch64.r3, Aarch64.sp); // copy revised stack pointer to r3
        theCompiler.peekLong(Aarch64.r0, 0);

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        assert registerValues[0] == 0xffffffff0000ffffL;
        assert registerValues[2] - registerValues[3] == 32;
    }

    public void test_DoDconst() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();

        double myVal = 3.14123;

        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        masm.mov(64, Aarch64.r2, Aarch64.sp); // copy stack pointer to r2
        theCompiler.do_dconstTests(myVal);
        masm.mov(64, Aarch64.r3, Aarch64.sp); // copy revised stack pointer to r3
        theCompiler.peekLong(Aarch64.r0, 0);  // recover value pushed by do_dconstTests

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        assert registerValues[0] == Double.doubleToRawLongBits(myVal); // test that poke & peek work
        assert (registerValues[2] - registerValues[3]) == 32; // test if sp changes correctly
    }

    public void test_DoFconst() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();

        float myVal = 3.14123f;

        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        masm.mov(32, Aarch64.r2, Aarch64.sp); // copy stack pointer to r2
        theCompiler.do_fconstTests(myVal);
        masm.mov(32, Aarch64.r3, Aarch64.sp); // copy revised stack pointer to r3
        theCompiler.peekInt(Aarch64.r0, 0);  // recover value pushed by do_dconstTests

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        assert registerValues[0] == Float.floatToRawIntBits(myVal); // test that poke & peek work
        assert (registerValues[2] - registerValues[3]) == 16; // test if sp changes correctly
    }

    public void test_DoLoad() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        expectedValues[0] = -2;
        expectedValues[1] = -1;
        expectedValues[2] = 0;
        expectedValues[3] = 1;
        expectedValues[4] = 2;
        expectedValues[5] = 3;
        expectedValues[6] = 4;
        expectedValues[7] = 5;
        expectedValues[8] = 6;
        expectedValues[9] = 7;
        expectedValues[10] = 8;
        for (int i = 0; i < 11; i++) {
            testValues[i] = true;
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], expectedValues[i]);
        }
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512 | 1024);
        for (int i = 0; i <= 10; i++) {
            masm.mov32BitConstant(Aarch64.cpuRegisters[i], -25);
        }
        for (int i = 0; i < 5; i++) {
            theCompiler.do_loadTests(i, Kind.INT);
            masm.pop(1);
            masm.mov32BitConstant(Aarch64.r0, 100 + i);
            masm.push(1);
            theCompiler.do_storeTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(5, Kind.LONG);
        masm.pop(1 | 2);
        masm.mov32BitConstant(Aarch64.r0, (int) (172L & 0xffff)); //172
        masm.mov32BitConstant(Aarch64.r1, (int) (((172L >> 32) & 0xffff))); //0
        masm.push(1 | 2);
        theCompiler.do_storeTests(5, Kind.LONG);
        for (int i = 0; i < 5; i++) {
            theCompiler.do_loadTests(i, Kind.INT);
        }
        theCompiler.do_loadTests(5, Kind.LONG);
        masm.pop(1 | 2 | 4 | 8 | 16 | 32 | 64);
        theCompiler.emitEpilogueTests();

        expectedValues[0] = 100;
        expectedValues[1] = 101;
        expectedValues[2] = 102;
        expectedValues[3] = 103;
        expectedValues[4] = 104;
        expectedValues[5] = 172;
        expectedValues[6] = 0;
        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        for (int i = 0; i <= 6; i++) {
            assert registerValues[i] == expectedValues[i] : "Reg val " + registerValues[i] + "  Exp " + expectedValues[i];
        }

    }

    public void test_Add() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(1);
        theCompiler.do_iconstTests(2);
        theCompiler.do_iaddTests();
        theCompiler.emitEpilogueTests();
        expectedValues[0] = 3;
        expectedValues[1] = 2;

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        assert expectedValues[0] == registerValues[0];
    }

    public void test_Mul() throws Exception {
        initialiseFrameForCompilation();
        theCompiler.do_initFrameTests(anMethod, codeAttr);
        theCompiler.emitPrologueTests();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        masm.push(1 | 2 | 4 | 8 | 16 | 32 | 64 | 128 | 256 | 512);
        theCompiler.do_iconstTests(3); // push the constant 1 onto the operand stack
        theCompiler.do_iconstTests(4); // push the constant 2 onto the operand stack
        theCompiler.do_imulTests();
        theCompiler.emitEpilogueTests();
        expectedValues[0] = 12;
        expectedValues[1] = 4;

        long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
        assert expectedValues[0] == registerValues[0];
    }


    public void test_push_pop() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();
        for (int i = 0; i < 6; i++) {
            expectedValues[i] = i;
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, i);
            masm.push(Aarch64.r16);
        }
        masm.pop(Aarch64.r5);
        masm.pop(Aarch64.r4);
        masm.pop(Aarch64.r3);
        masm.pop(Aarch64.r2);
        masm.pop(Aarch64.r1);
        masm.pop(Aarch64.r0);

        long[] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 6; i++) {
            assert expectedValues[i] == simulatedValues[i]
                    : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PeekWord() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;

        for (int i = 0; i < 5; i++) {
            testValues[i] = true;
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(Aarch64.r16);
        }

        theCompiler.peekWord(Aarch64.r4, 0);
        theCompiler.peekWord(Aarch64.r3, 1);
        theCompiler.peekWord(Aarch64.r2, 2);
        theCompiler.peekWord(Aarch64.r1, 3);
        theCompiler.peekWord(Aarch64.r0, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }


    public void test_PokeWord() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = Long.MAX_VALUE;
        expectedValues[1] = Long.MIN_VALUE;
        expectedValues[2] = -123456789;
        expectedValues[3] = 0;
        expectedValues[4] = 123456789;
        theCompiler.incStack(4);
        for (int i = 0; i < 5; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.pokeWord(Aarch64.r16, i);
        }

        theCompiler.peekWord(Aarch64.r0, 0);
        theCompiler.peekWord(Aarch64.r1, 1);
        theCompiler.peekWord(Aarch64.r2, 2);
        theCompiler.peekWord(Aarch64.r3, 3);
        theCompiler.peekWord(Aarch64.r4, 4);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 5; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PeekObject() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = 123456789;
        expectedValues[1] = 975318642;
        expectedValues[2] = 135792468;
        expectedValues[3] = Long.MAX_VALUE;

        for (int i = 0; i < 4; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            masm.push(Aarch64.r16);
        }

        theCompiler.peekObject(Aarch64.r3, 0);
        theCompiler.peekObject(Aarch64.r2, 1);
        theCompiler.peekObject(Aarch64.r1, 2);
        theCompiler.peekObject(Aarch64.r0, 3);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    public void test_PokeObject() throws Exception {
        initialiseExpectedValues();
        resetIgnoreValues();
        Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
        masm.codeBuffer.reset();

        expectedValues[0] = 123456789;
        expectedValues[1] = 975318642;
        expectedValues[2] = 135792468;
        expectedValues[3] = Long.MAX_VALUE;
        theCompiler.incStack(4);
        for (int i = 0; i < 4; i++) {
            masm.mov64BitConstant(Aarch64.r16, expectedValues[i]);
            theCompiler.pokeObject(Aarch64.r16, i);
        }

        theCompiler.peekObject(Aarch64.r0, 0);
        theCompiler.peekObject(Aarch64.r1, 1);
        theCompiler.peekObject(Aarch64.r2, 2);
        theCompiler.peekObject(Aarch64.r3, 3);

        long [] simulatedValues = generateAndTest(expectedValues, testValues, bitmasks);

        for (int i = 0; i < 4; i++) {
            assert expectedValues[i] == simulatedValues[i]
                            : "Register " + i + " " + simulatedValues[i] + " expected " + expectedValues[i];
        }
    }

    static final class BranchInfo {

        private int bc;
        private int start;
        private int end;
        private int expected;
        private int step;

        private BranchInfo(int bc, int start, int end, int expected, int step) {
            this.bc = bc;
            this.end = end;
            this.start = start;
            this.expected = expected;
            this.step = step;
        }

        public int getBytecode() {
            return bc;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getExpected() {
            return expected;
        }

        public int getStep() {
            return step;
        }
    }

    public void test_SwitchTable() throws Exception {
        // int i = 1;
        // int j, k , l, m;
        // switch(i) {
        // case 0: j=10;
        // case 1: k=20;
        // case 2: l=30;
        // default: m=40;
        // }

        // int chooseNear(int i) {
        // switch (i) {
        // } }
        // compiles to:
        // case 0: return 0;
        // case 1: return 1;
        // case 2: return 2;
        // default: return -1;
        // Method int chooseNear(int)
        // 0 iload_1 // Push local variable 1 (argument i)
        // 1 tableswitch 0 to 2: // Valid indices are 0 through 2
        // 0: 28
        // 1: 30
        // 2: 32
        // default:34
        // 28 iconst_0
        // 29 ireturn
        // 30 iconst_1
        // 31 ireturn
        // 32 iconst_2
        // 33 ireturn
        // 34 iconst_m1
        // 35 ireturn

        int[] values = new int[] {10, 20, 30, 40};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (i > j) {
                    expectedValues[j] = 0;
                } else {
                    expectedValues[j] = values[j];
                }
            }

            byte[] instructions = new byte[36];
            if (i == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else if (i == 1) {
                instructions[0] = (byte) Bytecodes.ICONST_1;
            } else if (i == 2) {
                instructions[0] = (byte) Bytecodes.ICONST_2;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_3;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.ILOAD_1;

            instructions[3] = (byte) Bytecodes.TABLESWITCH;
            instructions[4] = (byte) 0;
            instructions[5] = (byte) 0;
            instructions[6] = (byte) 0;
            instructions[7] = (byte) 0x1f; //31

            instructions[8] = (byte) 0;
            instructions[9] = (byte) 0;
            instructions[10] = (byte) 0;
            instructions[11] = (byte) 0;

            instructions[12] = (byte) 0;
            instructions[13] = (byte) 0;
            instructions[14] = (byte) 0;
            instructions[15] = (byte) 0x2;  //2

            instructions[16] = (byte) 0;
            instructions[17] = (byte) 0;
            instructions[18] = (byte) 0;
            instructions[19] = (byte) 0x19; // 25

            instructions[20] = (byte) 0;
            instructions[21] = (byte) 0;
            instructions[22] = (byte) 0;
            instructions[23] = (byte) 0x1b; // 27

            instructions[24] = (byte) 0;
            instructions[25] = (byte) 0;
            instructions[26] = (byte) 0;
            instructions[27] = (byte) 0x1d; // 29

            instructions[28] = (byte) Bytecodes.BIPUSH;
            instructions[29] = (byte) values[0];

            instructions[30] = (byte) Bytecodes.BIPUSH;
            instructions[31] = (byte) values[1];

            instructions[32] = (byte) Bytecodes.BIPUSH;
            instructions[33] = (byte) values[2];

            instructions[34] = (byte) Bytecodes.BIPUSH;
            instructions[35] = (byte) values[3];

            initialiseFrameForCompilation(instructions, "(II)I");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, instructions);
            theCompiler.peekInt(Aarch64.r3, 0);
            theCompiler.peekInt(Aarch64.r2, 1);
            theCompiler.peekInt(Aarch64.r1, 2);
            theCompiler.peekInt(Aarch64.r0, 3);

            long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
            assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
            assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
            theCompiler.cleanup();
        }
    }

    public void test_LookupTable() throws Exception {
        // int ii = 1;
        // int o, k, l, m;
        // switch (ii) {
        // case -100:
        // o = 10;
        // case 0:
        // k = 20;
        // case 100:
        // l = 30;
        // default:
        // m = 40;
        // }
        int[] values = new int[] {10, 20, 30, 40};
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                if (i > j) {
                    expectedValues[j] = 0;
                } else {
                    expectedValues[j] = values[j];
                }
            }

            byte[] instructions = new byte[48];
            if (i == 0) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) -100;
            } else if (i == 1) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 0;
            } else if (i == 2) {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 100;
            } else {
                instructions[0] = (byte) Bytecodes.BIPUSH;
                instructions[1] = (byte) 1;
            }
            instructions[2] = (byte) Bytecodes.ISTORE_1;
            instructions[3] = (byte) Bytecodes.ILOAD_1;

            instructions[4] = (byte) Bytecodes.LOOKUPSWITCH;
            instructions[5] = (byte) 0;
            instructions[6] = (byte) 0;
            instructions[7] = (byte) 0;

            instructions[8] = (byte) 0;
            instructions[9] = (byte) 0;
            instructions[10] = (byte) 0;
            instructions[11] = (byte) 0x2A;

            instructions[12] = (byte) 0;
            instructions[13] = (byte) 0;
            instructions[14] = (byte) 0;
            instructions[15] = (byte) 3;

            instructions[16] = (byte) 0xff;
            instructions[17] = (byte) 0xff;
            instructions[18] = (byte) 0xff;
            instructions[19] = (byte) 0x9c;

            instructions[20] = (byte) 0;
            instructions[21] = (byte) 0;
            instructions[22] = (byte) 0;
            instructions[23] = (byte) 0x24;

            instructions[24] = (byte) 0;
            instructions[25] = (byte) 0;
            instructions[26] = (byte) 0;
            instructions[27] = (byte) 0;

            instructions[28] = (byte) 0;
            instructions[29] = (byte) 0;
            instructions[30] = (byte) 0;
            instructions[31] = (byte) 0x26;

            instructions[32] = (byte) 0;
            instructions[33] = (byte) 0;
            instructions[34] = (byte) 0;
            instructions[35] = (byte) 0x64;

            instructions[36] = (byte) 0;
            instructions[37] = (byte) 0;
            instructions[38] = (byte) 0;
            instructions[39] = (byte) 0x28;

            instructions[40] = (byte) Bytecodes.BIPUSH;
            instructions[41] = (byte) values[0];

            instructions[42] = (byte) Bytecodes.BIPUSH;
            instructions[43] = (byte) values[1];

            instructions[44] = (byte) Bytecodes.BIPUSH;
            instructions[45] = (byte) values[2];

            instructions[46] = (byte) Bytecodes.BIPUSH;
            instructions[47] = (byte) values[3];

            initialiseFrameForCompilation(instructions, "(II)I");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, instructions);
            theCompiler.peekInt(Aarch64.r3, 0);
            theCompiler.peekInt(Aarch64.r2, 1);
            theCompiler.peekInt(Aarch64.r1, 2);
            theCompiler.peekInt(Aarch64.r0, 3);

            long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
            assert registerValues[0] == expectedValues[0] : "Failed incorrect value " + registerValues[0] + " " + expectedValues[0];
            assert registerValues[1] == expectedValues[1] : "Failed incorrect value " + registerValues[1] + " " + expectedValues[1];
            assert registerValues[2] == expectedValues[2] : "Failed incorrect value " + registerValues[2] + " " + expectedValues[2];
            assert registerValues[3] == expectedValues[3] : "Failed incorrect value " + registerValues[3] + " " + expectedValues[3];
            theCompiler.cleanup();
        }
    }


    private static final List<BranchInfo> branches = new LinkedList<>();
    static {
        branches.add(new BranchInfo(Bytecodes.IF_ICMPLT, 0, 10, 10, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPLE, 0, 10, 11, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPGT, 5, 0, 0, -1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPGE, 5, 0, -1, -1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPNE, 5, 6, 6, 1));
        branches.add(new BranchInfo(Bytecodes.IF_ICMPEQ, 0, 0, 2, 2));
    }


    public void test_BranchBytecodes() throws Exception {
        /*
         * Based on pg41 JVMSv1.7 ... iconst_0 istore_1 goto 8 wrong it needs to be 6 iinc 1 1 iload_1 bipush 100
         * if_icmplt 5 this is WRONG it needs to be -6 // no return. corresponding to int i; for(i = 0; i < 100;i++) { ;
         * // empty loop body } return;
         */
        for (BranchInfo bi : branches) {
            expectedValues[0] = bi.getExpected();
            testValues[0] = true;
            byte[] instructions = new byte[16];
            if (bi.getStart() == 0) {
                instructions[0] = (byte) Bytecodes.ICONST_0;
            } else {
                instructions[0] = (byte) Bytecodes.ICONST_5;
            }
            instructions[1] = (byte) Bytecodes.ISTORE_1;
            instructions[2] = (byte) Bytecodes.GOTO;
            instructions[3] = (byte) 0;
            instructions[4] = (byte) 6;
            instructions[5] = (byte) Bytecodes.IINC;
            instructions[6] = (byte) 1;
            instructions[7] = (byte) bi.getStep();
            instructions[8] = (byte) Bytecodes.ILOAD_1;
            instructions[9] = (byte) Bytecodes.BIPUSH;
            instructions[10] = (byte) bi.getEnd();
            instructions[11] = (byte) bi.getBytecode();
            instructions[12] = (byte) 0xff;
            instructions[13] = (byte) 0xfa;
            instructions[14] = (byte) Bytecodes.ILOAD_1;
            instructions[15] = (byte) Bytecodes.NOP;

            // instructions[14] = (byte) Bytecodes.RETURN;
            initialiseFrameForCompilation(instructions, "(II)I");
            theCompiler.offlineT1XCompileNoEpilogue(anMethod, codeAttr, instructions);
            Aarch64MacroAssembler masm = theCompiler.getMacroAssembler();
            //masm.pop(Aarch64Assembler.ConditionFlag.AL, 1);
            masm.pop(32, Aarch64.r0);
            long[] registerValues = generateAndTest(expectedValues, testValues, bitmasks);
            assert registerValues[0] == (expectedValues[0] & 0xFFFFFFFFL) : "Failed incorrect value " + Long.toString(registerValues[0], 16) + " " + Long.toString(expectedValues[0], 16);
            theCompiler.cleanup();
        }
    }
//
// public void ignore_Locals() throws Exception {
// }
//


    public void ignore_ByteCodeLoad() throws Exception {
    }
}
